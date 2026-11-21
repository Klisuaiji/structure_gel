package com.legacy.structure_gel.core.item.building_tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.registry.SGSimpleRegistry;
import com.legacy.structure_gel.api.util.SGCodecs;
import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.capability.level.BuildingToolWorldData;
import com.legacy.structure_gel.core.capability.misc.LevelTicksData;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ActionHistory
{
	public static final Codec<ActionHistory> CODEC = RecordCodecBuilder.create(instance ->
	{
		// @formatter:off
		return instance.group(
				Codec.STRING.fieldOf("player").forGetter(i -> i.player),
				Action.CODEC.listOf().fieldOf("actions").forGetter(i -> i.actions),
				ExtraCodecs.NON_NEGATIVE_INT.fieldOf("last_action").forGetter(i -> i.lastActionIndex),
				Codec.LONG.optionalFieldOf("last_modified", 0L).forGetter(i -> i.lastModified)).apply(instance, ActionHistory::new);
		// @formatter:on
	});

	private String player;
	private final LinkedList<Action> actions;
	private int lastActionIndex;
	private boolean dirty = false;
	private long lastModified = 0L;

	public static final ActionHistory EMPTY = new ActionHistory("EMPTY")
	{
		@Override
		public void add(Level level, ActionBuilder action)
		{
		}

		@Override
		public boolean undo(ServerPlayer player)
		{
			return false;
		}

		@Override
		public boolean redo(ServerPlayer player)
		{
			return false;
		}

		@Override
		public boolean isEmpty()
		{
			return true;
		}
	};

	public ActionHistory(String player, List<Action> actions, int lastActionIndex, long lastModified)
	{
		this.player = player;
		this.actions = new LinkedList<>(actions);
		this.lastActionIndex = lastActionIndex;
		this.lastModified = lastModified;
	}

	public ActionHistory(String player)
	{
		this(player, Collections.emptyList(), 0, 0);
	}

	public static ActionHistory get(Player player)
	{
		return get(player.getGameProfile().getName(), player.level());
	}

	public static ActionHistory get(String playerName, Level level)
	{
		if (level instanceof ServerLevel serverLevel)
		{
			return BuildingToolPlayerData.get(serverLevel, playerName).getActionHistory();
		}
		return ActionHistory.EMPTY;
	}

	public static Set<String> getHistoryOwners(Level level)
	{
		if (level instanceof ServerLevel serverLevel)
		{
			return BuildingToolWorldData.get(serverLevel).getPlayers();
		}
		return Collections.emptySet();
	}

	public static Set<String> getHistoryOwners(MinecraftServer server)
	{
		Set<String> owners = new HashSet<>();
		server.getAllLevels().forEach(level -> owners.addAll(getHistoryOwners(level)));
		return owners;
	}

	public String getPlayer()
	{
		return this.player;
	}

	private boolean spamming = false;

	public void add(Level level, ActionBuilder action)
	{
		long oldModified = this.lastModified;
		this.modified(level);
		long timeSinceLastModification = this.lastModified - oldModified;
		boolean wasSpamming = this.spamming;
		if (timeSinceLastModification > 2)
		{
			this.spamming = false;
			while (this.lastActionIndex > 0)
			{
				this.actions.removeFirst();
				this.lastActionIndex--;
			}
		}
		else
		{
			this.spamming = true;
			// If this action happens within 1 tick of another, remove the last action and combine it into the new one, re-adding to the list
			Action firstAction = this.actions.pollFirst();
			action.insertOld(firstAction);
			// Add the action that initiated the spam
			if (!wasSpamming)
			{
				Action secondAction = this.actions.pollFirst();
				action.insertOld(secondAction);
			}
		}

		this.actions.addFirst(action.build(level));
		while (this.actions.size() > SGConfig.COMMON.getBuildingToolMaxUndos())
			this.actions.removeLast();
	}

	public boolean undo(ServerPlayer player)
	{
		this.modified(player.level());
		if (this.lastActionIndex < this.actions.size())
		{
			if (this.actions.get(this.lastActionIndex).undo(player, player.serverLevel()))
				this.lastActionIndex++;
			return true;
		}
		return false;
	}

	public boolean redo(ServerPlayer player)
	{
		this.modified(player.level());
		if (this.lastActionIndex > 0)
		{
			if (this.actions.get(this.lastActionIndex - 1).redo(player, player.serverLevel()))
				this.lastActionIndex--;
			return true;
		}
		return false;
	}

	public boolean isDirty()
	{
		return this.dirty;
	}

	public void setDirty(boolean changed)
	{
		this.dirty = changed;
	}

	public void setModified(long time)
	{
		this.lastModified = time;
		this.setDirty(true);
	}

	public void modified(Level level)
	{
		this.setModified(this.lastModified = level.getGameTime());
	}

	public boolean isExpired(Level level)
	{
		return this.isEmpty() || level.getGameTime() - this.lastModified > SGConfig.COMMON.buildingToolHistoryExpiration();
	}

	public boolean isEmpty()
	{
		return this.actions.isEmpty();
	}

	public void clear()
	{
		this.actions.clear();
		this.lastActionIndex = 0;
	}

	public static ActionBuilder newAction(@Nullable Level level, boolean causeUpdates)
	{
		return new ActionBuilder(level, causeUpdates);
	}

	public static class ActionBuilder
	{
		private final boolean causeUpdates;
		private final Map<Integer, LinkedList<BlockChange>> blockChanges = new HashMap<>();
		private final LinkedList<Change> changes = new LinkedList<>();

		private ActionBuilder(@Nullable Level level, boolean causeUpdates)
		{
			this.causeUpdates = causeUpdates;

			if (!this.causeUpdates)
			{
				LevelTicksData.shouldScheduleTicks(level, false);
			}
		}

		public ActionBuilder insertOld(@Nullable Action action)
		{
			if (action != null)
			{
				Change[] changes = action.changes;
				// Adds these actions to the beginning of the list since they're from an old action history and should be undone in the correct order
				for (int i = changes.length - 1; i > -1; i--)
					this.changes.addFirst(changes[i]);
			}
			return this;
		}

		public ActionBuilder changeBlock(BlockPos pos, BlockState oldState, @Nullable CompoundTag oldBlockEntity, BlockState newState, @Nullable CompoundTag newBlockEntity, int flags)
		{
			this.addChange(new BlockChange(pos, oldState, Optional.ofNullable(oldBlockEntity), newState, Optional.ofNullable(newBlockEntity), flags));
			return this;
		}

		public ActionBuilder changeSelection(BuildingToolMode mode, int posIndex, BlockPos oldPos, BlockPos newPos)
		{
			this.addChange(new SelectionChange(mode, posIndex, oldPos, newPos));
			return this;
		}

		public ActionBuilder addEntity(Entity entity)
		{
			CompoundTag tag = new CompoundTag();
			entity.save(tag);
			this.addChange(new AddEntity(entity.getUUID(), tag));
			return this;
		}

		public ActionBuilder removeEntity(Entity entity)
		{
			CompoundTag tag = new CompoundTag();
			entity.save(tag);
			this.addChange(new RemoveEntity(entity.getUUID(), tag));
			return this;
		}

		private void addChange(Change change)
		{
			if (change instanceof BlockChange blockChange)
				this.blockChanges.computeIfAbsent(blockChange.flags(), flag -> new LinkedList<>()).add(blockChange);
			else
				this.changes.add(change);
		}

		private void compressData()
		{
			if (!this.blockChanges.isEmpty())
			{
				this.blockChanges.forEach((flags, changes) -> this.changes.add(CombinedBlockChange.from(flags, changes)));
				//this.blockChanges.forEach(this.changes::add);
			}
		}

		private Action build(@Nullable Level level)
		{
			if (level != null)
			{
				if (!this.causeUpdates)
				{
					LevelTicksData.shouldScheduleTicks(level, true);
				}
			}
			this.compressData();
			return new Action(this.changes.toArray(Change[]::new), this.causeUpdates);
		}
	}

	private static record Action(Change[] changes, boolean causeUpdates)
	{

		public static final Codec<Action> CODEC = RecordCodecBuilder.create(instance ->
		{
			// formatter:off
			return instance.group(Change.CODEC.listOf().fieldOf("changes").xmap(l -> l.toArray(Change[]::new), Arrays::asList).forGetter(Action::changes), Codec.BOOL.optionalFieldOf("cause_updates", false).forGetter(Action::causeUpdates)).apply(instance, Action::new);
			// formatter:on
		});

		private static final String UNDO_KEY = "info.structure_gel.building_tool.message.undo";
		private static final String REDO_KEY = "info.structure_gel.building_tool.message.redo";

		public boolean undo(ServerPlayer player, ServerLevel level)
		{
			if (!this.causeUpdates)
				LevelTicksData.shouldScheduleTicks(level, false);
			int totalChanges = this.getChangeTotal();
			ActionCounter actionCounter = new ActionCounter(a -> this.sendMessage(player, UNDO_KEY, a.lastPercent, a.count, totalChanges));
			for (int i = this.changes.length - 1; i > -1; i--)
			{
				this.changes[i].undo(player, level, actionCounter);
			}
			if (!this.causeUpdates)
				LevelTicksData.shouldScheduleTicks(level, true);
			return true;
		}

		public boolean redo(ServerPlayer player, ServerLevel level)
		{
			if (!this.causeUpdates)
				LevelTicksData.shouldScheduleTicks(level, false);
			int totalChanges = this.getChangeTotal();
			ActionCounter actionCounter = new ActionCounter(a -> this.sendMessage(player, REDO_KEY, a.lastPercent, a.count, totalChanges));
			for (var change : this.changes)
			{
				change.redo(player, level, actionCounter);
			}
			if (!this.causeUpdates)
				LevelTicksData.shouldScheduleTicks(level, true);
			return true;
		}

		private int sendMessage(ServerPlayer player, String key, int lastPercent, int changes, int totalChanges)
		{
			int percent = Math.round(changes / (float) totalChanges * 100);
			if (percent - lastPercent >= 1 || percent == 100)
				player.displayClientMessage(Component.translatable(key, percent), true);
			return percent;
		}

		private int getChangeTotal()
		{
			int total = 0;
			for (Change c : this.changes)
				total += c.changeCount();
			return total;
		}
	}

	private static class ActionCounter
	{
		int lastPercent = 0;
		int count = 0;
		final Function<ActionCounter, Integer> onIncrement;

		ActionCounter(Function<ActionCounter, Integer> onIncrement)
		{
			this.onIncrement = onIncrement;
		}

		void increment()
		{
			count++;
			this.lastPercent = this.onIncrement.apply(this);
		}
	}

	private static interface Change
	{
		public static final SGSimpleRegistry<ResourceLocation, ChangeType> TYPE_REGISTRY = new SGSimpleRegistry<>(StructureGelMod.locate("world_change_type"), () -> null, null);
		public static final Codec<Change> CODEC = ResourceLocation.CODEC.xmap(name -> TYPE_REGISTRY.get(name), change -> TYPE_REGISTRY.getKey(change)).dispatch(Change::type, ChangeType::codec);

		static void init()
		{
			TYPE_REGISTRY.register(StructureGelMod.locate("block_change"), BlockChange.TYPE);
			TYPE_REGISTRY.register(StructureGelMod.locate("combined_block_change"), CombinedBlockChange.TYPE);
			TYPE_REGISTRY.register(StructureGelMod.locate("selection_change"), SelectionChange.TYPE);
			TYPE_REGISTRY.register(StructureGelMod.locate("add_entity"), AddEntity.TYPE);
			TYPE_REGISTRY.register(StructureGelMod.locate("remove_entity"), RemoveEntity.TYPE);
		}

		abstract void undo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished);

		abstract void redo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished);

		abstract ChangeType type();

		default int changeCount()
		{
			return 1;
		}

		public static interface ChangeType
		{
			abstract MapCodec<? extends Change> codec();
		}
	}

	private static record BlockChange(BlockPos pos, BlockState oldState, Optional<CompoundTag> oldBlockEntity, BlockState newState, Optional<CompoundTag> newBlockEntity, int flags) implements Change
	{

		public static final MapCodec<BlockChange> CODEC = RecordCodecBuilder.mapCodec(instance ->
		{
			// @formatter:off
			return instance.group(
					BlockPos.CODEC.fieldOf("pos").forGetter(BlockChange::pos),
					BlockState.CODEC.fieldOf("old_state").forGetter(BlockChange::oldState),
					CompoundTag.CODEC.optionalFieldOf("old_be").forGetter(BlockChange::oldBlockEntity),
					BlockState.CODEC.fieldOf("new_state").forGetter(BlockChange::newState),
					CompoundTag.CODEC.optionalFieldOf("new_be").forGetter(BlockChange::newBlockEntity),
					Codec.INT.optionalFieldOf("flags", Block.UPDATE_ALL + Block.UPDATE_KNOWN_SHAPE).forGetter(BlockChange::flags))
					.apply(instance, BlockChange::new);
			// @formatter:on
		});
		public static final ChangeType TYPE = () -> CODEC;

		@Override
		public void undo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			this.apply(level, this.oldState, this.oldBlockEntity, actionsFinished);
		}

		@Override
		public void redo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			this.apply(level, this.newState, this.newBlockEntity, actionsFinished);
		}

		private void apply(ServerLevel level, BlockState state, Optional<CompoundTag> blockEntityTag, ActionCounter actionsFinished)
		{
			BlockPos pos = this.pos;
			// Clear current block entity
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity != null)
			{
				Clearable.tryClear(blockEntity);
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
			}
			// Place new block with nbt
			level.setBlock(pos, state, this.flags);
			if (blockEntityTag.isPresent())
				level.getBlockEntity(pos).loadWithComponents(blockEntityTag.get(), level.registryAccess());

			actionsFinished.increment();
		}

		@Override
		public ChangeType type()
		{
			return TYPE;
		}
	}

	private static record CombinedBlockChange(int flags, List<BlockState> palette, List<Long> blockPositions, List<BlockChangeData> blockData, Map<Integer, BlockEntityChangeData> beData) implements Change
	{

		public static final MapCodec<CombinedBlockChange> CODEC = RecordCodecBuilder.mapCodec(instance ->
		{
			Codec<Integer> intAsString = Codec.STRING.xmap(Integer::valueOf, String::valueOf); // Can't just use int codec because NBT gets angry when you serialize a key as a int instead of a string
			// @formatter:off
			return instance.group(
					Codec.INT.fieldOf("flags").forGetter(CombinedBlockChange::flags),
					BlockState.CODEC.listOf().optionalFieldOf("palette", List.of()).forGetter(CombinedBlockChange::palette),
					Codec.LONG.listOf().optionalFieldOf("block_positions", List.of()).forGetter(CombinedBlockChange::blockPositions),
					BlockChangeData.CODEC.listOf().optionalFieldOf("block_data", List.of()).forGetter(CombinedBlockChange::blockData),
					Codec.unboundedMap(intAsString, BlockEntityChangeData.CODEC).optionalFieldOf("be_data", Map.of()).forGetter(CombinedBlockChange::beData))
					.apply(instance, CombinedBlockChange::new);
			// @formatter:on
		});
		public static final ChangeType TYPE = () -> CODEC;

		private static CombinedBlockChange from(int flags, List<BlockChange> blockChanges)
		{
			List<BlockState> palette = new ArrayList<>();
			Map<BlockState, Short> reversedPalette = new HashMap<>();
			List<Long> blockPositions = new ArrayList<>();
			List<BlockChangeData> blockData = new ArrayList<>();
			Map<Integer, BlockEntityChangeData> beData = new HashMap<>();
			short paletteIndex = 0;
			int index = 0;
			for (BlockChange blockChange : blockChanges)
			{
				BlockState oldState = blockChange.oldState, newState = blockChange.newState;
				short oldStateId;
				if (!reversedPalette.containsKey(oldState))
				{
					oldStateId = paletteIndex;
					palette.add(oldState);
					reversedPalette.put(oldState, paletteIndex++);
				}
				else
				{
					oldStateId = reversedPalette.get(oldState);
				}

				short newStateId;
				if (!reversedPalette.containsKey(newState))
				{
					newStateId = paletteIndex;
					palette.add(newState);
					reversedPalette.put(newState, paletteIndex++);
				}
				else
				{
					newStateId = reversedPalette.get(newState);
				}

				long pos = blockChange.pos.asLong();
				blockPositions.add(pos);
				blockData.add(new BlockChangeData(oldStateId, newStateId));
				if (blockChange.oldBlockEntity.isPresent() || blockChange.newBlockEntity.isPresent())
					beData.put(index, new BlockEntityChangeData(blockChange.oldBlockEntity, blockChange.newBlockEntity));

				index++;
			}
			return new CombinedBlockChange(flags, palette, blockPositions, blockData, beData);
		}

		@Override
		public void undo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			this.apply(player, level, c -> c.undo(player, level, actionsFinished), true);
		}

		@Override
		public void redo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			this.apply(player, level, c -> c.redo(player, level, actionsFinished), false);
		}

		private void apply(ServerPlayer player, ServerLevel level, Consumer<BlockChange> action, boolean reverseOrder)
		{
			int size = this.blockPositions.size();
			for (int i = reverseOrder ? size - 1 : 0; reverseOrder ? i > -1 : i < size; i = reverseOrder ? i - 1 : i + 1)
			{
				BlockChangeData change = this.blockData.get(i);
				BlockEntityChangeData beChange = this.beData.getOrDefault(i, BlockEntityChangeData.EMPTY);
				BlockChange c = new BlockChange(BlockPos.of(this.blockPositions.get(i)), this.palette.get(change.oldState), beChange.oldBlockEntity, this.palette.get(change.newState), beChange.newBlockEntity, this.flags);
				action.accept(c);
			}
		}

		@Override
		public ChangeType type()
		{
			return TYPE;
		}

		@Override
		public int changeCount()
		{
			return this.blockPositions.size();
		}

		private static record BlockChangeData(short oldState, short newState)
		{
			// Short is 16 bit. Compress 2 of them into an int
			public static final Codec<BlockChangeData> CODEC = Codec.INT.xmap(i ->
			{
				return new BlockChangeData((short) (i >> 16), (short) (i & Short.MAX_VALUE));
			}, b ->
			{
				return ((int) b.oldState << 16) | b.newState;
			});
		}

		private static record BlockEntityChangeData(Optional<CompoundTag> oldBlockEntity, Optional<CompoundTag> newBlockEntity)
		{
			public static final Codec<BlockEntityChangeData> CODEC = RecordCodecBuilder.create(instance ->
			{
				// @formatter:off
				return instance.group(
						CompoundTag.CODEC.optionalFieldOf("old_be").forGetter(BlockEntityChangeData::oldBlockEntity),
						CompoundTag.CODEC.optionalFieldOf("new_be").forGetter(BlockEntityChangeData::newBlockEntity))
						.apply(instance, BlockEntityChangeData::new);
				// @formatter:on
			});
			public static final BlockEntityChangeData EMPTY = new BlockEntityChangeData(Optional.empty(), Optional.empty());
		}
	}

	private static record SelectionChange(BuildingToolMode mode, int posIndex, BlockPos oldPos, BlockPos newPos) implements Change
	{

		public static final MapCodec<SelectionChange> CODEC = RecordCodecBuilder.mapCodec(instance ->
		{
			// @formatter:off
			return instance.group(BuildingToolMode.CODEC.fieldOf("mode").forGetter(SelectionChange::mode), 
					ExtraCodecs.NON_NEGATIVE_INT.fieldOf("pos_index").forGetter(SelectionChange::posIndex), 
					BlockPos.CODEC.fieldOf("old_pos").forGetter(SelectionChange::oldPos), 
					BlockPos.CODEC.fieldOf("new_pos").forGetter(SelectionChange::newPos)).apply(instance, SelectionChange::new);
			// @formatter:on
		});
		public static final ChangeType TYPE = () -> CODEC;

		@Override
		public void undo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			this.apply(player, this.oldPos, actionsFinished);
		}

		@Override
		public void redo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			this.apply(player, this.newPos, actionsFinished);
		}

		private void apply(ServerPlayer player, BlockPos pos, ActionCounter actionsFinished)
		{
			ItemStack mainItem = player.getMainHandItem();
			ItemStack offItem = player.getOffhandItem();
			ItemStack buildingTool = mainItem.is(SGRegistry.Items.BUILDING_TOOL.get()) ? mainItem : offItem.is(SGRegistry.Items.BUILDING_TOOL.get()) ? offItem : null;
			if (buildingTool != null && BuildingToolItem.getMode(buildingTool) == this.mode)
			{
				BuildingToolItem.setPos(buildingTool, this.posIndex, pos);
			}

			actionsFinished.increment();
		}

		@Override
		public ChangeType type()
		{
			return TYPE;
		}
	}

	private static record AddEntity(UUID uuid, CompoundTag entity) implements Change
	{

		public static final MapCodec<AddEntity> CODEC = RecordCodecBuilder.mapCodec(instance ->
		{
			// @formatter:off
			return instance.group(SGCodecs.UUID.fieldOf("uuid").forGetter(AddEntity::uuid),
					CompoundTag.CODEC.fieldOf("entity").forGetter(AddEntity::entity)).apply(instance, AddEntity::new);
			// @formatter:on
		});
		public static final ChangeType TYPE = () -> CODEC;

		@Override
		public void undo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			Entity entity = level.getEntity(this.uuid);
			if (entity != null)
				entity.remove(RemovalReason.DISCARDED);

			actionsFinished.increment();
		}

		@Override
		public void redo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			Entity entity = EntityType.create(this.entity, level, MobSpawnType.COMMAND).orElse(null);
			if (entity != null)
				level.addFreshEntity(entity);

			actionsFinished.increment();
		}

		@Override
		public ChangeType type()
		{
			return TYPE;
		}
	}

	private static record RemoveEntity(UUID uuid, CompoundTag entity) implements Change
	{

		public static final MapCodec<RemoveEntity> CODEC = RecordCodecBuilder.mapCodec(instance ->
		{
			// @formatter:off
			return instance.group(SGCodecs.UUID.fieldOf("uuid").forGetter(RemoveEntity::uuid),
					CompoundTag.CODEC.fieldOf("entity").forGetter(RemoveEntity::entity)).apply(instance, RemoveEntity::new);
			// @formatter:on
		});
		public static final ChangeType TYPE = () -> CODEC;

		@Override
		public void undo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			new AddEntity(uuid, entity).redo(player, level, actionsFinished);
		}

		@Override
		public void redo(ServerPlayer player, ServerLevel level, ActionCounter actionsFinished)
		{
			new AddEntity(uuid, entity).undo(player, level, actionsFinished);
		}

		@Override
		public ChangeType type()
		{
			return TYPE;
		}
	}

	public static void init()
	{
		Change.init();
	}
}
