package com.legacy.structure_gel.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.fml.util.ObfuscationReflectionHelper.UnableToFindMethodException;

public record SGAccessor<R>(Method method) {
   public static final SGAccessor<?> ENTITY_PROCESS_PORTAL_COOLDOWN = create(Entity.class, "processPortalCooldown");
   public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_NONE = create(NoiseRouterData.class, "none");
   public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_OVERWORLD = create(
      NoiseRouterData.class, "overworld", HolderGetter.class, HolderGetter.class, boolean.class, boolean.class
   );
   public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_NETHER = create(NoiseRouterData.class, "nether", HolderGetter.class, HolderGetter.class);
   public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_END = create(NoiseRouterData.class, "end", HolderGetter.class);
   public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_CAVES = create(NoiseRouterData.class, "caves", HolderGetter.class, HolderGetter.class);
   public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_FLOATING_ISLANDS = create(
      NoiseRouterData.class, "floatingIslands", HolderGetter.class, HolderGetter.class
   );
   public static final SGAccessor<Boolean> STRUCTURE_PIECE_CREATE_CHEST = create(
      StructurePiece.class,
      "createChest",
      ServerLevelAccessor.class,
      BoundingBox.class,
      RandomSource.class,
      BlockPos.class,
      ResourceLocation.class,
      BlockState.class
   );
   public static final SGAccessor<?> HANGING_ENTITY_SET_DIRECTION = create(HangingEntity.class, "setDirection", Direction.class);

   public static <R> SGAccessor<R> create(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
      try {
         return new SGAccessor<>(ObfuscationReflectionHelper.findMethod(clazz, methodName, parameterTypes));
      } catch (Throwable t) {
         throw new UnableToFindMethodException(t);
      }
   }

   @Nullable
   public R invoke(Object owner, Object... params) {
      try {
         return (R)this.method.invoke(owner, params);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
         StructureGelMod.LOGGER.fatal("Couldn't use accessor. {}", e);
         return null;
      }
   }

   @Nullable
   public R invokeStatic(Object... params) {
      return this.invoke(null, params);
   }

   public static void init() {
   }
}
