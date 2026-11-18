package com.legacy.structure_gel.core.item.building_tool;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.legacy.structure_gel.core.SGConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public class CapturedBlocks {
   public static final Codec<CapturedBlocks> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            BlockPos.CODEC.fieldOf("world_pos").forGetter(CapturedBlocks::getWorldPos),
            BlockPos.CODEC.fieldOf("center_offset").forGetter(CapturedBlocks::getCenterPos),
            BoundingBox.CODEC.fieldOf("bounds").forGetter(CapturedBlocks::getBounds),
            CapturedBlocks.BlockInfo.CODEC.listOf().fieldOf("block_infos").forGetter(CapturedBlocks::getBlockInfos),
            CapturedBlocks.EntityInfo.CODEC.listOf().fieldOf("entity_infos").forGetter(CapturedBlocks::getEntityInfos),
            Mirror.CODEC.fieldOf("mirror").forGetter(CapturedBlocks::getMirror),
            Rotation.CODEC.fieldOf("rotation").forGetter(CapturedBlocks::getRotation)
         )
         .apply(instance, CapturedBlocks::new)
   );
   private final BlockPos worldPos;
   private final BlockPos centerOffset;
   private final BoundingBox bounds;
   private List<CapturedBlocks.BlockInfo> blockInfos;
   private final List<CapturedBlocks.EntityInfo> entityInfos;
   private final Mirror mirror;
   private final Rotation rotation;
   @Nullable
   private CapturedBlocks lastTransform = null;
   @Nullable
   private CapturedBlocks clientCopy = null;
   @Nullable
   private Map<BlockPos, VoxelShape> shapeCache = null;
   @Nullable
   private List<AABB> entityBoxesCache = null;
   boolean compressedForRender = false;
   private static final String PALETTE_KEY = "palette";
   private static final String DEFAULT_STATE_KEY = "default_state";
   private static final String BLOCKS_KEY = "blocks";
   private static final String STATE_KEY = "state";
   private static final String TAG_KEY = "tag";
   private static final String BOUNDS_KEY = "bounds";
   private static final String WORLD_POS_KEY = "world_pos";
   private static final String CENTER_OFFSET_KEY = "center_offset";
   private static final String MIRROR_KEY = "mirror";
   private static final String ROTATION_KEY = "rotation";
   private static final String ENTITIES_KEY = "entities";
   private static final String ENTITY_KEY = "entity";
   private static final String POS_KEY = "pos";
   private static final String UUID_KEY = "uuid";

   private CapturedBlocks(
      BlockPos worldPos,
      BlockPos centerOffset,
      BoundingBox bounds,
      List<CapturedBlocks.BlockInfo> blockInfos,
      List<CapturedBlocks.EntityInfo> entityInfos,
      Mirror mirror,
      Rotation rotation
   ) {
      this.worldPos = worldPos;
      this.centerOffset = centerOffset;
      this.bounds = bounds;
      this.blockInfos = blockInfos;
      this.entityInfos = entityInfos;
      this.mirror = mirror;
      this.rotation = rotation;
   }

   public CapturedBlocks copy() {
      return new CapturedBlocks(this.worldPos, this.centerOffset, this.bounds, this.blockInfos, this.entityInfos, this.mirror, this.rotation);
   }

   public CapturedBlocks clientCopy() {
      if (this.clientCopy == null) {
         this.clientCopy = this.copy();
      }

      return this.clientCopy;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj instanceof CapturedBlocks other) {
         if (!this.worldPos.equals(other.worldPos)) {
            return false;
         } else if (!this.centerOffset.equals(other.centerOffset)) {
            return false;
         } else if (!this.bounds.equals(other.bounds)) {
            return false;
         } else if (!this.mirror.equals(other.mirror)) {
            return false;
         } else if (!this.rotation.equals(other.rotation)) {
            return false;
         } else {
            return !this.blockInfos.equals(other.blockInfos) ? false : this.entityInfos.equals(other.entityInfos);
         }
      } else {
         return false;
      }
   }

   public CapturedBlocks(Level level, BlockPos cornerA, BlockPos cornerB, boolean includeEntities) {
      this(level, cornerA, cornerB, Mirror.NONE, Rotation.NONE, includeEntities);
   }

   public CapturedBlocks(Level level, BlockPos cornerA, BlockPos cornerB, Mirror mirror, Rotation rotation, boolean includeEntities) {
      BoundingBox bounds = BoundingBox.fromCorners(cornerA, cornerB);
      List<CapturedBlocks.BlockInfo> infos = new LinkedList<>();
      int minX = bounds.minX();
      int minY = bounds.minY();
      int minZ = bounds.minZ();
      int maxX = bounds.maxX();
      int maxY = bounds.maxY();
      int maxZ = bounds.maxZ();
      Vec3i bbLength = bounds.getLength();
      Vec3i transformedBBLength = bbLength;

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockPos pos = new BlockPos(x, y, z);
               BlockState state = level.getBlockState(pos);
               BlockEntity blockEntity = level.getBlockEntity(pos);
               CompoundTag blockEntityTag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;
               BlockPos transformedPos = this.transformPos(new BlockPos(x - minX, y - minY, z - minZ), bbLength, mirror, rotation);
               infos.add(new CapturedBlocks.BlockInfo(transformedPos, state.mirror(mirror).rotate(rotation), Optional.ofNullable(blockEntityTag)));
            }
         }
      }

      if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
         bounds = BoundingBox.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(minX + bbLength.getZ(), maxY, minZ + bbLength.getX()));
         transformedBBLength = bounds.getLength();
      }

      this.bounds = bounds;
      this.centerOffset = new BlockPos(
         -transformedBBLength.getX() + transformedBBLength.getX() / 2, 0, -transformedBBLength.getZ() + transformedBBLength.getZ() / 2
      );
      this.worldPos = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
      if (includeEntities) {
         List<CapturedBlocks.EntityInfo> entityInfos = new ArrayList<>();

         for (Entity entity : level.getEntities(null, AABB.of(this.bounds))) {
            if (!(entity instanceof Player)) {
               Vec3 pos = this.transformPos(entity.position().subtract(Vec3.atLowerCornerOf(this.worldPos)), bbLength, mirror, rotation);
               CompoundTag entityTag = new CompoundTag();
               entity.save(entityTag);
               entityTag.remove("UUID");
               entityInfos.add(new CapturedBlocks.EntityInfo(entity.getUUID(), pos, entityTag));
            }
         }

         this.entityInfos = entityInfos;
      } else {
         this.entityInfos = Collections.emptyList();
      }

      this.blockInfos = new ArrayList<>(infos);
      this.mirror = mirror;
      this.rotation = rotation;
   }

   public CapturedBlocks withTransforms(Mirror mirror, Rotation rotation) {
      if (this.lastTransform != null && this.lastTransform.mirror == mirror && this.lastTransform.rotation == rotation) {
         return this.lastTransform;
      } else {
         BoundingBox bounds = this.bounds;
         List<CapturedBlocks.BlockInfo> blockInfos = new ArrayList<>(this.blockInfos.size());
         int minX = bounds.minX();
         int minY = bounds.minY();
         int minZ = bounds.minZ();
         int maxY = bounds.maxY();
         Vec3i bbLength = bounds.getLength();
         Vec3i transformedBBLength = bbLength;

         for (CapturedBlocks.BlockInfo info : this.blockInfos) {
            BlockPos pos = info.pos;
            BlockPos transformedPos = this.transformPos(pos, bbLength, mirror, rotation);
            blockInfos.add(new CapturedBlocks.BlockInfo(transformedPos, info.state.mirror(mirror).rotate(rotation), info.blockEntityTag));
         }

         if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            bounds = BoundingBox.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(minX + bbLength.getZ(), maxY, minZ + bbLength.getX()));
            transformedBBLength = bounds.getLength();
         }

         BlockPos centerOffset = new BlockPos(
            -transformedBBLength.getX() + transformedBBLength.getX() / 2, 0, -transformedBBLength.getZ() + transformedBBLength.getZ() / 2
         );
         BlockPos worldPos = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
         List<CapturedBlocks.EntityInfo> entityInfos = new ArrayList<>();

         for (CapturedBlocks.EntityInfo info : this.entityInfos) {
            Vec3 pos = info.pos;
            Vec3 transformedPos = this.transformPos(pos, bbLength, mirror, rotation);
            entityInfos.add(new CapturedBlocks.EntityInfo(info.original, transformedPos, info.entityTag));
         }

         this.lastTransform = new CapturedBlocks(worldPos, centerOffset, bounds, blockInfos, entityInfos, mirror, rotation);
         return this.lastTransform;
      }
   }

   public void compressForRender(Level level, BlockPos startPos, Player player) {
      if (!this.compressedForRender) {
         int maxTime = SGConfig.CLIENT.maxCloneCompileTime();
         boolean timeExceeded = false;
         long startTime = System.currentTimeMillis();
         Map<BlockPos, BlockState> positions = this.blockInfos
            .stream()
            .collect(Collectors.toMap(CapturedBlocks.BlockInfo::pos, CapturedBlocks.BlockInfo::state));
         List<CapturedBlocks.BlockInfo> newBlockInfos = new ArrayList<>(this.blockInfos.size() / 3);

         for (CapturedBlocks.BlockInfo info : this.blockInfos) {
            boolean remove = true;
            BlockPos pos = info.pos;

            for (Direction dir : Direction.values()) {
               BlockPos offset = pos.relative(dir);
               BlockState offsetState = positions.get(offset);
               if (offsetState == null || !offsetState.isSolidRender(level, startPos.offset(offset))) {
                  remove = false;
                  break;
               }
            }

            if (!remove) {
               newBlockInfos.add(info);
            }

            if (System.currentTimeMillis() - startTime > maxTime) {
               timeExceeded = true;
               break;
            }
         }

         if (timeExceeded) {
            player.displayClientMessage(
               Component.literal("[Structure Gel] A Building Tool render tool longer than " + maxTime + "ms to compile. Aborting.")
                  .withStyle(ChatFormatting.RED),
               false
            );
            this.blockInfos = new ArrayList<>(0);
         } else {
            this.blockInfos = newBlockInfos;
         }

         this.compressedForRender = true;
      }
   }

   private BlockPos transformPos(BlockPos pos, Vec3i size, Mirror mirror, Rotation rotation) {
      if (mirror == Mirror.NONE && rotation == Rotation.NONE) {
         return pos;
      } else {
         int x = pos.getX();
         int y = pos.getY();
         int z = pos.getZ();
         switch (mirror) {
            case LEFT_RIGHT:
               z = size.getZ() - z;
               break;
            case FRONT_BACK:
               x = size.getX() - x;
         }

         switch (rotation) {
            case CLOCKWISE_90:
               int invZ = size.getZ() - z;
               z = x;
               x = invZ;
               break;
            case CLOCKWISE_180:
               x = size.getX() - x;
               z = size.getZ() - z;
               break;
            case COUNTERCLOCKWISE_90:
               int invX = size.getX() - x;
               x = z;
               z = invX;
         }

         return new BlockPos(x, y, z);
      }
   }

   private Vec3 transformPos(Vec3 pos, Vec3i size, Mirror mirror, Rotation rotation) {
      if (mirror == Mirror.NONE && rotation == Rotation.NONE) {
         return pos;
      } else {
         size = size.offset(1, 1, 1);
         double x = pos.x();
         double y = pos.y();
         double z = pos.z();
         switch (mirror) {
            case LEFT_RIGHT:
               z = size.getZ() - z;
               break;
            case FRONT_BACK:
               x = size.getX() - x;
         }

         switch (rotation) {
            case CLOCKWISE_90:
               double invZ = size.getZ() - z;
               z = x;
               x = invZ;
               break;
            case CLOCKWISE_180:
               x = size.getX() - x;
               z = size.getZ() - z;
               break;
            case COUNTERCLOCKWISE_90:
               double invX = size.getX() - x;
               x = z;
               z = invX;
         }

         return new Vec3(x, y, z);
      }
   }

   public BoundingBox getBounds() {
      return this.bounds;
   }

   public BlockPos getCenterPos() {
      return this.centerOffset;
   }

   public BlockPos getWorldPos() {
      return this.worldPos;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public List<CapturedBlocks.BlockInfo> getBlockInfos() {
      return Collections.unmodifiableList(this.blockInfos);
   }

   public List<CapturedBlocks.EntityInfo> getEntityInfos() {
      return Collections.unmodifiableList(this.entityInfos);
   }

   public Map<BlockPos, VoxelShape> getShapes(Level level, BlockPos worldPos) {
      if (this.shapeCache == null) {
         this.shapeCache = this.getBlockInfos().stream().collect(Collectors.toMap(b -> worldPos.offset(b.pos()), b -> b.state().getShape(level, b.pos())));
      }

      return this.shapeCache;
   }

   public List<AABB> getEntityBoxes(Level level, BlockPos worldPos) {
      if (this.entityBoxesCache == null) {
         this.entityBoxesCache = this.entityInfos
            .stream()
            .map(info -> info.createEntity(level, worldPos, this.mirror, this.rotation))
            .filter(e -> e != null)
            .<AABB>map(Entity::getBoundingBox)
            .map(aabb -> aabb.getSize() <= 0.0 ? AABB.ofSize(aabb.getCenter(), 0.75, 0.75, 0.75) : aabb)
            .toList();
      }

      return this.entityBoxesCache;
   }

   public CompoundTag toCompressedTag(RegistryAccess registryAccess) {
      CompoundTag tag = new CompoundTag();
      BiMap<BlockState, Integer> states = HashBiMap.create();
      Map<BlockState, MutableInt> stateUsage = new HashMap<>();

      for (CapturedBlocks.BlockInfo info : this.blockInfos) {
         BlockState state = info.state;
         states.computeIfAbsent(state, i -> states.size());
         if (!info.blockEntityTag.isPresent()) {
            MutableInt count = stateUsage.get(state);
            if (count == null) {
               stateUsage.put(state, new MutableInt(1));
            } else {
               count.increment();
            }
         }
      }

      BlockState mostUsed = stateUsage.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).map(Entry::getKey).findFirst().orElse(null);
      if (mostUsed != null) {
         tag.put("default_state", NbtUtils.writeBlockState(mostUsed));
      }

      ListTag paletteTag = new ListTag();

      for (BlockState state : states.keySet()) {
         paletteTag.add(NbtUtils.writeBlockState(state));
      }

      tag.put("palette", paletteTag);
      CompoundTag blocksTag = new CompoundTag();

      for (CapturedBlocks.BlockInfo infox : this.blockInfos) {
         if (infox.state != mostUsed) {
            CompoundTag infoTag = new CompoundTag();
            infoTag.putInt("state", (Integer)states.get(infox.state));
            if (infox.blockEntityTag.isPresent()) {
               infoTag.put("tag", (Tag)infox.blockEntityTag.get());
            }

            blocksTag.put(Long.toString(infox.pos.asLong()), infoTag);
         }
      }

      tag.put("blocks", blocksTag);
      ListTag entitiesListTag = new ListTag();

      for (CapturedBlocks.EntityInfo infoxx : this.entityInfos) {
         CompoundTag infoTag = new CompoundTag();
         infoTag.putUUID("uuid", infoxx.original);
         ListTag pos = new ListTag();
         pos.add(FloatTag.valueOf((float)infoxx.pos.x));
         pos.add(FloatTag.valueOf((float)infoxx.pos.y));
         pos.add(FloatTag.valueOf((float)infoxx.pos.z));
         infoTag.put("pos", pos);
         infoTag.put("entity", infoxx.entityTag);
         entitiesListTag.add(infoTag);
      }

      tag.put("entities", entitiesListTag);
      tag.putLongArray(
         "bounds",
         new long[]{
            BlockPos.asLong(this.bounds.minX(), this.bounds.minY(), this.bounds.minZ()),
            BlockPos.asLong(this.bounds.maxX(), this.bounds.maxY(), this.bounds.maxZ())
         }
      );
      tag.putLong("world_pos", this.worldPos.asLong());
      tag.putLong("center_offset", this.centerOffset.asLong());
      tag.putString("mirror", this.mirror.name());
      tag.putString("rotation", this.rotation.name());
      return tag;
   }

   public static CapturedBlocks fromCompressedTag(RegistryAccess registryAccess, CompoundTag tag) {
      HolderGetter<Block> blockRegistry = registryAccess.registryOrThrow(Registries.BLOCK).asLookup();
      BlockState[] palette = tag.getList("palette", 10).stream().map(tx -> NbtUtils.readBlockState(blockRegistry, (CompoundTag)tx)).toArray(BlockState[]::new);
      BlockState defaultState = tag.contains("default_state", 10) ? NbtUtils.readBlockState(blockRegistry, tag.getCompound("default_state")) : null;
      long[] boundsCorners = tag.getLongArray("bounds");
      BoundingBox bounds = BoundingBox.fromCorners(BlockPos.of(boundsCorners[0]), BlockPos.of(boundsCorners[1]));
      CompoundTag blocksTag = tag.getCompound("blocks");
      List<CapturedBlocks.BlockInfo> blockInfos = new ArrayList<>(bounds.getXSpan() * bounds.getYSpan() * bounds.getZSpan());

      for (int x = 0; x <= bounds.getXSpan() - 1; x++) {
         for (int z = 0; z <= bounds.getZSpan() - 1; z++) {
            for (int y = 0; y <= bounds.getYSpan() - 1; y++) {
               BlockPos pos = new BlockPos(x, y, z);
               String l = Long.toString(pos.asLong());
               if (blocksTag.contains(l, 10)) {
                  CompoundTag infoTag = blocksTag.getCompound(l);
                  BlockState state = palette[infoTag.getInt("state")];
                  Optional<CompoundTag> beTag = Optional.ofNullable(infoTag.contains("tag") ? infoTag.getCompound("tag") : null);
                  blockInfos.add(new CapturedBlocks.BlockInfo(pos, state, beTag));
               } else if (defaultState != null) {
                  blockInfos.add(new CapturedBlocks.BlockInfo(pos, defaultState, Optional.empty()));
               }
            }
         }
      }

      ListTag entitiesListTag = tag.getList("entities", 10);
      List<CapturedBlocks.EntityInfo> entityInfos = new ArrayList<>(entitiesListTag.size());

      for (Tag t : entitiesListTag) {
         if (t instanceof CompoundTag infoTag) {
            ListTag posTag = infoTag.getList("pos", 5);
            entityInfos.add(
               new CapturedBlocks.EntityInfo(
                  infoTag.getUUID("uuid"), new Vec3(posTag.getFloat(0), posTag.getFloat(1), posTag.getFloat(2)), infoTag.getCompound("entity")
               )
            );
         }
      }

      return new CapturedBlocks(
         BlockPos.of(tag.getLong("world_pos")),
         BlockPos.of(tag.getLong("center_offset")),
         bounds,
         blockInfos,
         entityInfos,
         Mirror.valueOf(tag.getString("mirror")),
         Rotation.valueOf(tag.getString("rotation"))
      );
   }

   public record BlockInfo(BlockPos pos, BlockState state, Optional<CompoundTag> blockEntityTag) {
      public static final Codec<CapturedBlocks.BlockInfo> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(CapturedBlocks.BlockInfo::pos),
               BlockState.CODEC.fieldOf("state").forGetter(CapturedBlocks.BlockInfo::state),
               CompoundTag.CODEC.optionalFieldOf("tag").forGetter(CapturedBlocks.BlockInfo::blockEntityTag)
            )
            .apply(instance, CapturedBlocks.BlockInfo::new)
      );
   }

   public record EntityInfo(UUID original, Vec3 pos, CompoundTag entityTag) {
      public static final Codec<CapturedBlocks.EntityInfo> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("original").forGetter(CapturedBlocks.EntityInfo::original),
               Vec3.CODEC.fieldOf("pos").forGetter(CapturedBlocks.EntityInfo::pos),
               CompoundTag.CODEC.fieldOf("entity").forGetter(CapturedBlocks.EntityInfo::entityTag)
            )
            .apply(instance, CapturedBlocks.EntityInfo::new)
      );

      @Nullable
      public Entity createEntity(Level level, BlockPos startPos, Mirror mirror, Rotation rotation) {
         Entity entity = (Entity)EntityType.create(this.entityTag(), level).orElse(null);
         if (entity != null) {
            float rot = entity.mirror(mirror);
            entity.setYRot(rot);
            rot = entity.rotate(rotation);
            entity.setYRot(rot);
            entity.setPos(this.pos().add(Vec3.atLowerCornerOf(startPos)));
         }

         return entity;
      }
   }
}
