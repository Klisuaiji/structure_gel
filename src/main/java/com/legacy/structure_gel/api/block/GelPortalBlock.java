package com.legacy.structure_gel.api.block;

import com.legacy.structure_gel.api.dimension.portal.GelPortalLogic;
import com.legacy.structure_gel.api.dimension.portal.GelTeleporter;
import com.legacy.structure_gel.core.capability.GelCapability;
import com.legacy.structure_gel.core.capability.entity.GelEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.serialization.MapCodec;
import java.util.Random;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class GelPortalBlock extends Block {
   public static final EnumProperty<Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
   protected static final int AABB_OFFSET = 2;
   protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
   protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

   public GelPortalBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         ((BlockState)this.stateDefinition.any()).hasProperty(AXIS)
            ? (BlockState)((BlockState)this.stateDefinition.any()).setValue(AXIS, Axis.X)
            : (BlockState)this.stateDefinition.any()
      );
   }

   public abstract MapCodec<? extends GelPortalBlock> codec();

   public abstract GelTeleporter getTeleporter(@Nullable ServerLevel var1, GelPortalBlock var2);

   public GelTeleporter getTeleporter(@Nullable ServerLevel serverLevel) {
      return this.getTeleporter(serverLevel, this);
   }

   public Supplier<BlockState> getFrameBlock() {
      return this.getTeleporter(null, this).getFrameBlock();
   }

   @OnlyIn(Dist.CLIENT)
   public void renderPortal(float timeInPortal, int screenHeight, int screenWidth) {
      if (timeInPortal < 1.0F) {
         timeInPortal *= timeInPortal;
         timeInPortal *= timeInPortal;
         timeInPortal = timeInPortal * 0.8F + 0.2F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, timeInPortal);
      RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      TextureAtlasSprite sprite = this.getPortalTexture();
      float f = sprite.getU0();
      float f1 = sprite.getV0();
      float f2 = sprite.getU1();
      float f3 = sprite.getV1();
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffBuilder = tessellator.getBuilder();
      buffBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      buffBuilder.vertex(0.0, screenHeight, -90.0).uv(f, f3).endVertex();
      buffBuilder.vertex(screenWidth, screenHeight, -90.0).uv(f2, f3).endVertex();
      buffBuilder.vertex(screenWidth, 0.0, -90.0).uv(f2, f1).endVertex();
      buffBuilder.vertex(0.0, 0.0, -90.0).uv(f, f1).endVertex();
      tessellator.end();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @OnlyIn(Dist.CLIENT)
   public TextureAtlasSprite getPortalTexture() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getBlockRenderer().getBlockModelShaper().getParticleIcon(this.defaultBlockState());
   }

   @OnlyIn(Dist.CLIENT)
   public SoundInstance getTriggerSound() {
      return SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, new Random().nextFloat() * 0.4F + 0.8F, 0.25F);
   }

   @OnlyIn(Dist.CLIENT)
   public SoundInstance getTravelSound() {
      return SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, new Random().nextFloat() * 0.4F + 0.8F, 0.25F);
   }

   public int getMaxTimeInside(Entity entity) {
      return entity.getPortalWaitTime();
   }

   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
   }

   public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
      Axis facingAxis = facing.getAxis();
      Axis portalAxis = (Axis)state.getValue(AXIS);
      boolean flag = portalAxis != facingAxis && facingAxis.isHorizontal();
      return !flag && !facingState.is(this) && !GelPortalLogic.isPortalComplete(level, currentPos, portalAxis, this.getFrameBlock().get().getBlock(), this)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
   }

   public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      GelCapability.ifPresent(entity, gelEntity -> {
         if (!entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) {
            entity.handleInsidePortal(pos);
            gelEntity.setPortal(this);
            GelEntity.setPortalClient(entity, this);
         }
      });
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (state.hasProperty(AXIS)) {
         switch ((Axis)state.getValue(AXIS)) {
            case Z:
               return Z_AXIS_AABB;
            case X:
            default:
               return X_AXIS_AABB;
         }
      } else {
         return Shapes.block();
      }
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos blockPos, BlockState state) {
      return ItemStack.EMPTY;
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            switch ((Axis)state.getValue(AXIS)) {
               case Z:
                  return (BlockState)state.setValue(AXIS, Axis.X);
               case X:
                  return (BlockState)state.setValue(AXIS, Axis.Z);
               default:
                  return state;
            }
         default:
            return state;
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{AXIS});
   }
}
