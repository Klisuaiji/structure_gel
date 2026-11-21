package com.legacy.structure_gel.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
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

/**
 * 
 * @author Silver_David
 *
 * @param <R>
 *            The return type of the method. Uses a wild card for void.
 */
public record SGAccessor<R>(Method method)
{

	public static <R> SGAccessor<R> create(Class<?> clazz, String methodName, Class<?>... parameterTypes)
	{
		try
		{
			return new SGAccessor<R>(ObfuscationReflectionHelper.findMethod(clazz, methodName, parameterTypes));
		}
		catch (Throwable t)
		{
			throw new UnableToFindMethodException(t);
		}
	}

	// Entity.processPortalCooldown m_8021_
	public static final SGAccessor<?> ENTITY_PROCESS_PORTAL_COOLDOWN = create(Entity.class, "processPortalCooldown");

	// NoiseRouterData.none m_238384_
	public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_NONE = create(NoiseRouterData.class, "none");
	// NoiseRouterData.overworld m_255262_
	/**
	 * registry, large biomes, amplified
	 */
	public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_OVERWORLD = create(NoiseRouterData.class, "overworld", HolderGetter.class, HolderGetter.class, boolean.class, boolean.class);
	// NoiseRouterData.nether m_255404_
	public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_NETHER = create(NoiseRouterData.class, "nether", HolderGetter.class, HolderGetter.class);
	// NoiseRouterData.end m_255104_
	public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_END = create(NoiseRouterData.class, "end", HolderGetter.class);
	// NoiseRouterData.caves m_255020_
	public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_CAVES = create(NoiseRouterData.class, "caves", HolderGetter.class, HolderGetter.class);
	// NoiseRouterData.floatingIslands m_254860_
	public static final SGAccessor<NoiseRouter> NOISE_ROUTER_DATA_FLOATING_ISLANDS = create(NoiseRouterData.class, "floatingIslands", HolderGetter.class, HolderGetter.class);

	// StructurePiece.createChest m_226762_
	public static final SGAccessor<Boolean> STRUCTURE_PIECE_CREATE_CHEST = create(StructurePiece.class, "createChest", ServerLevelAccessor.class, BoundingBox.class, RandomSource.class, BlockPos.class, ResourceKey.class, BlockState.class);

	// HangingEntity.setDirection m_6022_
	public static final SGAccessor<?> HANGING_ENTITY_SET_DIRECTION = create(HangingEntity.class, "setDirection", Direction.class);

	@SuppressWarnings("unchecked")
	@Nullable
	public R invoke(@Nullable Object owner, Object... params)
	{
		try
		{
			return (R) this.method.invoke(owner, params);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			StructureGelMod.LOGGER.fatal("Couldn't use accessor. {}", e);
			return null;
		}
	}

	@Nullable
	public R invokeStatic(Object... params)
	{
		return this.invoke(null, params);
	}

	public static void init()
	{

	}
}
