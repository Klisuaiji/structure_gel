package com.legacy.structure_gel.core.item.building_tool;

import com.legacy.structure_gel.api.registry.SGSimpleRegistry;
import com.legacy.structure_gel.api.util.SGCodecs;
import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.capability.level.BuildingToolWorldData;
import com.legacy.structure_gel.core.capability.misc.LevelTicksData;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ActionHistory {
   public static final Codec<ActionHistory> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(i -> i.player),
            ActionHistory.Action.CODEC.listOf().fieldOf("actions").forGetter(i -> i.actions),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("last_action").forGetter(i -> i.lastActionIndex),
            Codec.LONG.optionalFieldOf("last_modified", 0L).forGetter(i -> i.lastModified)
         )
         .apply(instance, ActionHistory::new)
   );
   private String player;
   private final LinkedList<ActionHistory.Action> actions;
   private int lastActionIndex;
   private boolean dirty = false;
   private long lastModified = 0L;
   public static final ActionHistory EMPTY = new ActionHistory("EMPTY") {
      @Override
      public void add(Level level, ActionHistory.ActionBuilder action) {
      }

      @Override
      public boolean undo(ServerPlayer player) {
         return false;
      }

      @Override
      public boolean redo(ServerPlayer player) {
         return false;
      }

      @Override
      public boolean isEmpty() {
         return true;
      }
   };
   private boolean spamming = false;

   public ActionHistory(String player, List<ActionHistory.Action> actions, int lastActionIndex, long lastModified) {
      this.player = player;
      this.actions = new LinkedList<>(actions);
      this.lastActionIndex = lastActionIndex;
      this.lastModified = lastModified;
   }

   public ActionHistory(String player) {
      this(player, Collections.emptyList(), 0, 0L);
   }

   public static ActionHistory get(Player player) {
      return get(player.getGameProfile().getName(), player.level());
   }

   public static ActionHistory get(String playerName, Level level) {
      return level instanceof ServerLevel serverLevel ? BuildingToolPlayerData.get(serverLevel, playerName).getActionHistory() : EMPTY;
   }

   public static Set<String> getHistoryOwners(Level level) {
      return level instanceof ServerLevel serverLevel ? BuildingToolWorldData.get(serverLevel).getPlayers() : Collections.emptySet();
   }

   public static Set<String> getHistoryOwners(MinecraftServer server) {
      Set<String> owners = new HashSet<>();
      server.getAllLevels().forEach(level -> owners.addAll(getHistoryOwners(level)));
      return owners;
   }

   public String getPlayer() {
      return this.player;
   }

   public void add(Level level, ActionHistory.ActionBuilder action) {
      long oldModified = this.lastModified;
      this.modified(level);
      long timeSinceLastModification = this.lastModified - oldModified;
      boolean wasSpamming = this.spamming;
      if (timeSinceLastModification > 2L) {
         for (this.spamming = false; this.lastActionIndex > 0; this.lastActionIndex--) {
            this.actions.removeFirst();
         }
      } else {
         this.spamming = true;
         ActionHistory.Action firstAction = this.actions.pollFirst();
         action.insertOld(firstAction);
         if (!wasSpamming) {
            ActionHistory.Action secondAction = this.actions.pollFirst();
            action.insertOld(secondAction);
         }
      }

      this.actions.addFirst(action.build(level));

      while (this.actions.size() > SGConfig.COMMON.getBuildingToolMaxUndos()) {
         this.actions.removeLast();
      }
   }

   public boolean undo(ServerPlayer player) {
      this.modified(player.level());
      if (this.lastActionIndex < this.actions.size()) {
         if (this.actions.get(this.lastActionIndex).undo(player, player.serverLevel())) {
            this.lastActionIndex++;
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean redo(ServerPlayer player) {
      this.modified(player.level());
      if (this.lastActionIndex > 0) {
         if (this.actions.get(this.lastActionIndex - 1).redo(player, player.serverLevel())) {
            this.lastActionIndex--;
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public void setDirty(boolean changed) {
      this.dirty = changed;
   }

   public void setModified(long time) {
      this.lastModified = time;
      this.setDirty(true);
   }

   public void modified(Level level) {
      this.setModified(this.lastModified = level.getGameTime());
   }

   public boolean isExpired(Level level) {
      return this.isEmpty() || level.getGameTime() - this.lastModified > SGConfig.COMMON.buildingToolHistoryExpiration();
   }

   public boolean isEmpty() {
      return this.actions.isEmpty();
   }

   public void clear() {
      this.actions.clear();
      this.lastActionIndex = 0;
   }

   public static ActionHistory.ActionBuilder newAction(@Nullable Level level, boolean causeUpdates) {
      return new ActionHistory.ActionBuilder(level, causeUpdates);
   }

   public static void init() {
      ActionHistory.Change.init();
   }

   private record Action(ActionHistory.Change[] changes, boolean causeUpdates) {
      public static final Codec<ActionHistory.Action> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               ActionHistory.Change.CODEC
                  .listOf()
                  .fieldOf("changes")
                  .xmap(l -> l.toArray(ActionHistory.Change[]::new), Arrays::asList)
                  .forGetter(ActionHistory.Action::changes),
               Codec.BOOL.optionalFieldOf("cause_updates", false).forGetter(ActionHistory.Action::causeUpdates)
            )
            .apply(instance, ActionHistory.Action::new)
      );
      private static final String UNDO_KEY = "info.structure_gel.building_tool.message.undo";
      private static final String REDO_KEY = "info.structure_gel.building_tool.message.redo";

      public boolean undo(ServerPlayer player, ServerLevel level) {
         if (!this.causeUpdates) {
            LevelTicksData.shouldScheduleTicks(level, false);
         }

         int totalChanges = this.changes.length;
         int changes = 0;
         int lastPercent = 0;

         for (int i = this.changes.length - 1; i > -1; i--) {
            this.changes[i].undo(player, level);
            lastPercent = this.sendMessage(player, "info.structure_gel.building_tool.message.undo", lastPercent, ++changes, totalChanges);
         }

         if (!this.causeUpdates) {
            LevelTicksData.shouldScheduleTicks(level, true);
         }

         return true;
      }

      public boolean redo(ServerPlayer player, ServerLevel level) {
         if (!this.causeUpdates) {
            LevelTicksData.shouldScheduleTicks(level, false);
         }

         int totalChanges = this.changes.length;
         int changes = 0;
         int lastPercent = 0;

         for (ActionHistory.Change change : this.changes) {
            change.redo(player, level);
            lastPercent = this.sendMessage(player, "info.structure_gel.building_tool.message.redo", lastPercent, ++changes, totalChanges);
         }

         if (!this.causeUpdates) {
            LevelTicksData.shouldScheduleTicks(level, true);
         }

         return true;
      }

      private int sendMessage(ServerPlayer player, String key, int lastPercent, int changes, int totalChanges) {
         int percent = Math.round((float)changes / totalChanges * 100.0F);
         if (percent - lastPercent >= 1 || percent == 100) {
            player.displayClientMessage(Component.translatable(key, new Object[]{percent}), true);
         }

         return percent;
      }
   }

   public static class ActionBuilder {
      private final boolean causeUpdates;
      private final LinkedList<ActionHistory.Change> changes = new LinkedList<>();

      private ActionBuilder(@Nullable Level level, boolean causeUpdates) {
         this.causeUpdates = causeUpdates;
         if (!this.causeUpdates) {
            LevelTicksData.shouldScheduleTicks(level, false);
         }
      }

      public ActionHistory.ActionBuilder insertOld(@Nullable ActionHistory.Action action) {
         if (action != null) {
            ActionHistory.Change[] changes = action.changes;

            for (int i = changes.length - 1; i > -1; i--) {
               this.changes.addFirst(changes[i]);
            }
         }

         return this;
      }

      public ActionHistory.ActionBuilder changeBlock(
         BlockPos pos, BlockState oldState, @Nullable CompoundTag oldBlockEntity, BlockState newState, @Nullable CompoundTag newBlockEntity, int flags
      ) {
         this.changes
            .add(new ActionHistory.BlockChange(pos, oldState, Optional.ofNullable(oldBlockEntity), newState, Optional.ofNullable(newBlockEntity), flags));
         return this;
      }

      public ActionHistory.ActionBuilder changeSelection(BuildingToolMode mode, int posIndex, BlockPos oldPos, BlockPos newPos) {
         this.changes.add(new ActionHistory.SelectionChange(mode, posIndex, oldPos, newPos));
         return this;
      }

      public ActionHistory.ActionBuilder addEntity(Entity entity) {
         CompoundTag tag = new CompoundTag();
         entity.save(tag);
         this.changes.add(new ActionHistory.AddEntity(entity.getUUID(), tag));
         return this;
      }

      public ActionHistory.ActionBuilder removeEntity(Entity entity) {
         CompoundTag tag = new CompoundTag();
         entity.save(tag);
         this.changes.add(new ActionHistory.RemoveEntity(entity.getUUID(), tag));
         return this;
      }

      private ActionHistory.Action build(@Nullable Level level) {
         if (level != null && !this.causeUpdates) {
            LevelTicksData.shouldScheduleTicks(level, true);
         }

         return new ActionHistory.Action(this.changes.toArray(ActionHistory.Change[]::new), this.causeUpdates);
      }
   }

   private record AddEntity(UUID uuid, CompoundTag entity) implements ActionHistory.Change {
      public static final Codec<ActionHistory.AddEntity> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               SGCodecs.UUID.fieldOf("uuid").forGetter(ActionHistory.AddEntity::uuid),
               CompoundTag.CODEC.fieldOf("entity").forGetter(ActionHistory.AddEntity::entity)
            )
            .apply(instance, ActionHistory.AddEntity::new)
      );
      public static final ActionHistory.Change.ChangeType TYPE = () -> CODEC;

      @Override
      public void undo(ServerPlayer player, ServerLevel level) {
         Entity entity = level.getEntity(this.uuid);
         if (entity != null) {
            entity.remove(RemovalReason.DISCARDED);
         }
      }

      @Override
      public void redo(ServerPlayer player, ServerLevel level) {
         Entity entity = (Entity)EntityType.create(this.entity, level).orElse(null);
         if (entity != null) {
            level.addFreshEntity(entity);
         }
      }

      @Override
      public ActionHistory.Change.ChangeType type() {
         return TYPE;
      }
   }

   private record BlockChange(
      BlockPos pos, BlockState oldState, Optional<CompoundTag> oldBlockEntity, BlockState newState, Optional<CompoundTag> newBlockEntity, int flags
   ) implements ActionHistory.Change {
      public static final Codec<ActionHistory.BlockChange> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(ActionHistory.BlockChange::pos),
               BlockState.CODEC.fieldOf("old_state").forGetter(ActionHistory.BlockChange::oldState),
               CompoundTag.CODEC.optionalFieldOf("old_be").forGetter(ActionHistory.BlockChange::oldBlockEntity),
               BlockState.CODEC.fieldOf("new_state").forGetter(ActionHistory.BlockChange::newState),
               CompoundTag.CODEC.optionalFieldOf("new_be").forGetter(ActionHistory.BlockChange::newBlockEntity),
               Codec.INT.optionalFieldOf("flags", 19).forGetter(ActionHistory.BlockChange::flags)
            )
            .apply(instance, ActionHistory.BlockChange::new)
      );
      public static final ActionHistory.Change.ChangeType TYPE = () -> CODEC;

      @Override
      public void undo(ServerPlayer player, ServerLevel level) {
         this.apply(level, this.oldState, this.oldBlockEntity);
      }

      @Override
      public void redo(ServerPlayer player, ServerLevel level) {
         this.apply(level, this.newState, this.newBlockEntity);
      }

      private void apply(ServerLevel level, BlockState state, Optional<CompoundTag> blockEntityTag) {
         BlockPos pos = this.pos;
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity != null) {
            Clearable.tryClear(blockEntity);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
         }

         level.setBlock(pos, state, this.flags);
         if (blockEntityTag.isPresent()) {
            level.getBlockEntity(pos).load(blockEntityTag.get());
         }
      }

      @Override
      public ActionHistory.Change.ChangeType type() {
         return TYPE;
      }
   }

   private interface Change {
      SGSimpleRegistry<ResourceLocation, ActionHistory.Change.ChangeType> TYPE_REGISTRY = new SGSimpleRegistry<>(
         StructureGelMod.locate("world_change_type"), () -> null, null
      );
      Codec<ActionHistory.Change> CODEC = ResourceLocation.CODEC
         .xmap(name -> TYPE_REGISTRY.get(name), change -> TYPE_REGISTRY.getKey(change))
         .dispatch(ActionHistory.Change::type, ActionHistory.Change.ChangeType::codec);

      static void init() {
         TYPE_REGISTRY.register(StructureGelMod.locate("block_change"), ActionHistory.BlockChange.TYPE);
         TYPE_REGISTRY.register(StructureGelMod.locate("selection_change"), ActionHistory.SelectionChange.TYPE);
         TYPE_REGISTRY.register(StructureGelMod.locate("add_entity"), ActionHistory.AddEntity.TYPE);
         TYPE_REGISTRY.register(StructureGelMod.locate("remove_entity"), ActionHistory.RemoveEntity.TYPE);
      }

      void undo(ServerPlayer var1, ServerLevel var2);

      void redo(ServerPlayer var1, ServerLevel var2);

      ActionHistory.Change.ChangeType type();

      public interface ChangeType {
         Codec<? extends ActionHistory.Change> codec();
      }
   }

   private record RemoveEntity(UUID uuid, CompoundTag entity) implements ActionHistory.Change {
      public static final Codec<ActionHistory.RemoveEntity> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               SGCodecs.UUID.fieldOf("uuid").forGetter(ActionHistory.RemoveEntity::uuid),
               CompoundTag.CODEC.fieldOf("entity").forGetter(ActionHistory.RemoveEntity::entity)
            )
            .apply(instance, ActionHistory.RemoveEntity::new)
      );
      public static final ActionHistory.Change.ChangeType TYPE = () -> CODEC;

      @Override
      public void undo(ServerPlayer player, ServerLevel level) {
         new ActionHistory.AddEntity(this.uuid, this.entity).redo(player, level);
      }

      @Override
      public void redo(ServerPlayer player, ServerLevel level) {
         new ActionHistory.AddEntity(this.uuid, this.entity).undo(player, level);
      }

      @Override
      public ActionHistory.Change.ChangeType type() {
         return TYPE;
      }
   }

   private record SelectionChange(BuildingToolMode mode, int posIndex, BlockPos oldPos, BlockPos newPos) implements ActionHistory.Change {
      public static final Codec<ActionHistory.SelectionChange> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               BuildingToolMode.CODEC.fieldOf("mode").forGetter(ActionHistory.SelectionChange::mode),
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("pos_index").forGetter(ActionHistory.SelectionChange::posIndex),
               BlockPos.CODEC.fieldOf("old_pos").forGetter(ActionHistory.SelectionChange::oldPos),
               BlockPos.CODEC.fieldOf("new_pos").forGetter(ActionHistory.SelectionChange::newPos)
            )
            .apply(instance, ActionHistory.SelectionChange::new)
      );
      public static final ActionHistory.Change.ChangeType TYPE = () -> CODEC;

      @Override
      public void undo(ServerPlayer player, ServerLevel level) {
         this.apply(player, this.oldPos);
      }

      @Override
      public void redo(ServerPlayer player, ServerLevel level) {
         this.apply(player, this.newPos);
      }

      private void apply(ServerPlayer player, BlockPos pos) {
         ItemStack mainItem = player.getMainHandItem();
         ItemStack offItem = player.getOffhandItem();
         ItemStack buildingTool = mainItem.is(SGRegistry.Items.BUILDING_TOOL.get())
            ? mainItem
            : (offItem.is(SGRegistry.Items.BUILDING_TOOL.get()) ? offItem : null);
         if (buildingTool != null && BuildingToolItem.getMode(buildingTool) == this.mode) {
            BuildingToolItem.setPos(buildingTool, this.posIndex, pos);
         }
      }

      @Override
      public ActionHistory.Change.ChangeType type() {
         return TYPE;
      }
   }
}
