package com.legacy.structure_gel.api.structure.jigsaw;

import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.util.Lazy;

public abstract class ExtendedJigsawStructurePiece extends PoolElementStructurePiece implements IModifyState {
   private static final String CAP_KEY = "capability";
   private Optional<JigsawCapability> jigsawCapability = Optional.empty();

   public ExtendedJigsawStructurePiece(IPieceFactory.Context context) {
      super(context.structureManager(), context.poolElement(), context.pos(), context.groundLevelDelta(), context.rotation(), context.bounds());
      this.setStructurePieceType(this.getType());
      this.jigsawCapability = context.jigsawCapability();
   }

   public ExtendedJigsawStructurePiece(StructurePieceSerializationContext context, CompoundTag nbt) {
      super(context, nbt);
      this.setStructurePieceType(this.getType());
      this.loadCapability(context, nbt);
   }

   protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
      super.addAdditionalSaveData(context, nbt);
      this.saveCapability(context, nbt);
   }

   private void loadCapability(StructurePieceSerializationContext context, CompoundTag nbt) {
      if (nbt.contains(this.getCapabilityKey(), 10)) {
         Optional<Pair<JigsawCapability, Tag>> result = JigsawCapability.CODEC
            .decode(RegistryOps.create(NbtOps.INSTANCE, context.registryAccess()), nbt.getCompound("capability"))
            .result();
         if (result.isPresent()) {
            JigsawCapability cap = (JigsawCapability)result.get().getFirst();
            this.attachCapability(Optional.ofNullable(cap));
         }
      }
   }

   private void saveCapability(StructurePieceSerializationContext context, CompoundTag nbt) {
      if (this.jigsawCapability.isPresent()) {
         Optional<Tag> result = JigsawCapability.CODEC
            .encodeStart(RegistryOps.create(NbtOps.INSTANCE, context.registryAccess()), this.jigsawCapability.get())
            .result();
         if (result.isPresent() && result.get() instanceof CompoundTag) {
            Tag tag = result.get();
            nbt.put(this.getCapabilityKey(), tag);
         }
      }
   }

   protected String getCapabilityKey() {
      return "capability";
   }

   public abstract void handleDataMarker(String var1, BlockPos var2, ServerLevelAccessor var3, RandomSource var4, BoundingBox var5);

   public abstract StructurePieceType getType();

   private void setStructurePieceType(StructurePieceType structurePieceType) {
      this.type = structurePieceType;
   }

   private final void attachCapability(@Nonnull Optional<JigsawCapability> jigsawCapability) {
      Objects.requireNonNull(jigsawCapability);
      this.jigsawCapability = jigsawCapability;
   }

   public Optional<JigsawCapability> getCapability() {
      return this.jigsawCapability;
   }

   public <C extends JigsawCapability> Optional<C> getCapability(Class<C> capabilityClass) {
      return this.getCapability().map(c -> capabilityClass.isInstance(c) ? capabilityClass.cast(c) : null);
   }

   protected <C extends JigsawCapability, T> Lazy<T> getCapabilityValue(Class<C> capabilityClass, Function<C, T> getter, Supplier<T> errorFallback) {
      return Lazy.of(() -> this.getCapability(capabilityClass).map(getter).orElseGet(() -> {
         StructureGelMod.LOGGER.warn("The capability for a jigsaw piece did not match the expected class: " + capabilityClass);
         return errorFallback.get();
      }));
   }

   @Nullable
   @Override
   public BlockState modifyState(ServerLevelAccessor level, RandomSource rand, BlockPos pos, BlockState originalState) {
      return originalState;
   }

   public <T extends Entity> T createEntity(EntityType<T> entityType, ServerLevelAccessor level, BlockPos pos, Rotation rotation) {
      T entity = (T)entityType.create(level.getLevel());
      entity.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rotation.rotate(Direction.SOUTH).toYRot(), 0.0F);
      return entity;
   }

   public ResourceLocation getLocation() {
      return this.element instanceof SinglePoolElement singleElement
         ? JigsawAccessHelper.getSingleJigsawPieceLocation(singleElement)
         : new ResourceLocation("empty");
   }

   public StructureTemplateManager getStructureManager() {
      return this.structureTemplateManager;
   }

   public Vec3i getSize() {
      Optional<StructureTemplate> optional = this.getStructureManager().get(this.getLocation());
      return optional.isPresent() ? optional.get().getSize() : Vec3i.ZERO;
   }

   public void setAir(LevelAccessor level, BlockPos pos) {
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
   }

   public void extendDown(LevelAccessor level, BlockState state, BoundingBox bounds, Rotation rotation, RandomSource rand) {
      this.extendDown(level, (Function<RandomSource, BlockState>)(random -> state), bounds, rotation, rand);
   }

   public void extendDown(LevelAccessor level, Function<RandomSource, BlockState> randStateGetter, BoundingBox bounds, Rotation rotation, RandomSource rand) {
      int offsetX = rotation != Rotation.CLOCKWISE_180 && this.rotation != Rotation.CLOCKWISE_90 ? 0 : -(this.getSize().getX() - 1);
      int offsetZ = rotation != Rotation.CLOCKWISE_180 && this.rotation != Rotation.COUNTERCLOCKWISE_90 ? 0 : -(this.getSize().getZ() - 1);

      for (int x = 0; x < 13; x++) {
         for (int z = 0; z < 13; z++) {
            if (!level.getBlockState(this.position.offset(x + offsetX, 0, z + offsetZ)).isAir()) {
               for (int offsetY = -1;
                  (
                        level.isEmptyBlock(this.position.offset(x + offsetX, offsetY, z + offsetZ))
                           || level.getBlockState(this.position.offset(x + offsetX, offsetY, z + offsetZ)).liquid()
                     )
                     && this.position.getY() + offsetY > 0;
                  offsetY--
               ) {
                  level.setBlock(this.position.offset(x + offsetX, offsetY, z + offsetZ), randStateGetter.apply(rand), 2);
               }
            }
         }
      }
   }
}
