package com.legacy.structure_gel.api.structure.jigsaw;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.legacy.structure_gel.api.structure.base.IModifyState;
import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.datafixers.util.Pair;

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
import net.minecraft.world.entity.MobSpawnType;
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

/**
 * A pool element structure piece for jigsaw structures that allows for data
 * structure block interaction and contains more functional methods to go along
 * with that. Use this as your piece type if you're doing anything with jigsaw
 * structures, as you'll have more options.
 *
 * @author Silver_David
 */
public abstract class ExtendedJigsawStructurePiece extends PoolElementStructurePiece implements IModifyState
{
	private static final String CAP_KEY = "capability";
	private Optional<JigsawCapability> jigsawCapability = Optional.empty();

	public ExtendedJigsawStructurePiece(IPieceFactory.Context context)
	{
		super(context.structureManager(), context.poolElement(), context.pos(), context.groundLevelDelta(), context.rotation(), context.bounds(), context.liquidSettings());
		this.setStructurePieceType(this.getType());
		this.jigsawCapability = context.jigsawCapability();
	}

	public ExtendedJigsawStructurePiece(StructurePieceSerializationContext context, CompoundTag nbt)
	{
		super(context, nbt);
		this.setStructurePieceType(this.getType());
		this.loadCapability(context, nbt);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt)
	{
		super.addAdditionalSaveData(context, nbt);
		this.saveCapability(context, nbt);
	}

	private void loadCapability(StructurePieceSerializationContext context, CompoundTag nbt)
	{
		if (nbt.contains(this.getCapabilityKey(), Tag.TAG_COMPOUND))
		{
			Optional<Pair<JigsawCapability, Tag>> result = JigsawCapability.CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, context.registryAccess()), nbt.getCompound(CAP_KEY)).result();
			if (result.isPresent())
			{
				JigsawCapability cap = result.get().getFirst();
				this.attachCapability(Optional.ofNullable(cap));
			}
		}
	}

	private void saveCapability(StructurePieceSerializationContext context, CompoundTag nbt)
	{
		if (this.jigsawCapability.isPresent())
		{
			Optional<Tag> result = JigsawCapability.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, context.registryAccess()), this.jigsawCapability.get()).result();
			if (result.isPresent() && result.get() instanceof CompoundTag)
			{
				Tag tag = result.get();
				nbt.put(this.getCapabilityKey(), tag);
			}
		}
	}

	/**
	 * Override this if you use "capability" as a key for nbt in your piece.
	 * 
	 * @return The key to save your capability under. "capability" by default.
	 */
	protected String getCapabilityKey()
	{
		return CAP_KEY;
	}

	/**
	 * Runs this function for every data structure block
	 *
	 * @param key
	 * @param pos
	 * @param level
	 * @param bounds
	 */
	public abstract void handleDataMarker(String key, BlockPos pos, ServerLevelAccessor level, RandomSource rand, BoundingBox bounds);

	/**
	 * @return The structure piece type for this piece
	 */
	public abstract StructurePieceType getType();

	/**
	 * Sets the structure piece type for this piece.
	 *
	 * @param structurePieceType
	 */
	private void setStructurePieceType(StructurePieceType structurePieceType)
	{
		this.type = structurePieceType;
	}

	private final void attachCapability(@Nonnull Optional<JigsawCapability> jigsawCapability)
	{
		Objects.requireNonNull(jigsawCapability);
		this.jigsawCapability = jigsawCapability;
	}

	public Optional<JigsawCapability> getCapability()
	{
		return this.jigsawCapability;
	}

	/**
	 * @return Obtains the capability, casting it to the class passed, or an empty
	 *         optional if the class didn't match.
	 */
	public <C extends JigsawCapability> Optional<C> getCapability(Class<C> capabilityClass)
	{
		return this.getCapability().map(c -> capabilityClass.isInstance(c) ? capabilityClass.cast(c) : null);
	}

	/**
	 * Creates a {@link Lazy} for obtaining a value from your capability.
	 * <p>
	 * Useful for situations like
	 * {@link #modifyState(ServerLevelAccessor, RandomSource, BlockPos, BlockState)
	 * modifyState} where you may need to use the value frequently. Since the
	 * capability is saved and loaded from nbt, this can be done in place of saving
	 * the value represented to nbt.
	 * 
	 * @param <C>
	 *            The class of your capability
	 * @param <T>
	 *            The class of the value to obtain
	 * @param capabilityClass
	 * @param getter
	 *            Obtains a value from the capability.
	 * @param errorFallback
	 *            A default return value if something goes wrong.
	 * @return
	 */
	protected <C extends JigsawCapability, T> Lazy<T> getCapabilityValue(Class<C> capabilityClass, Function<C, T> getter, Supplier<T> errorFallback)
	{
		return Lazy.of(() -> this.getCapability(capabilityClass).map(getter).orElseGet(() ->
		{
			StructureGelMod.LOGGER.warn("The capability for a jigsaw piece did not match the expected class: " + capabilityClass);
			return errorFallback.get();
		}));
	}

	@Override
	@Nullable
	public BlockState modifyState(ServerLevelAccessor level, RandomSource rand, BlockPos pos, BlockState originalState)
	{
		return originalState;
	}

	/**
	 * Shorthand method to create an entity with the given pos (offset by 0.5 on x
	 * and z) and rotation. Rotation is south by default, with the structure's
	 * rotation taken into account.
	 *
	 * @param entityType
	 * @param level
	 * @param pos
	 * @param rotation
	 * @return The newly created entity
	 */
	public <T extends Entity> T createEntity(EntityType<T> entityType, ServerLevelAccessor level, BlockPos pos, Rotation rotation)
	{
		T entity = entityType.create(level.getLevel(), MobSpawnType.STRUCTURE);
		entity.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rotation.rotate(Direction.SOUTH).toYRot(), 0);
		return entity;
	}

	/**
	 * Gives you the name of this structure piece. Useful for cases where you want
	 * extra things to happen when this piece generates
	 *
	 * @return The name of this structure piece
	 */
	public ResourceLocation getLocation()
	{
		if (this.element instanceof SinglePoolElement singleElement)
			return JigsawAccessHelper.getSingleJigsawPieceLocation(singleElement);
		return ResourceLocation.parse("empty");
	}

	/**
	 * @return The structure manager for this piece
	 */
	public StructureTemplateManager getStructureManager()
	{
		return this.structureTemplateManager;
	}

	/**
	 * @return The size of the structure
	 */
	public Vec3i getSize()
	{
		Optional<StructureTemplate> optional = this.getStructureManager().get(this.getLocation());
		return optional.isPresent() ? optional.get().getSize() : Vec3i.ZERO;
	}

	/**
	 * Shorthand for<br>
	 * level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
	 *
	 * @param level
	 * @param pos
	 */
	public void setAir(LevelAccessor level, BlockPos pos)
	{
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	/**
	 * Fills the air and liquid below the lowest part of this structure with the
	 * passed block state
	 *
	 * @param level
	 * @param state
	 * @param bounds
	 * @param rotation
	 * @param rand
	 */
	public void extendDown(LevelAccessor level, BlockState state, BoundingBox bounds, Rotation rotation, RandomSource rand)
	{
		this.extendDown(level, random -> state, bounds, rotation, rand);
	}

	/**
	 * Fills the air and liquid below the lowest part of this structure with the
	 * passed block state
	 *
	 * @param level
	 * @param randStateGetter
	 * @param bounds
	 * @param rotation
	 * @param rand
	 */
	public void extendDown(LevelAccessor level, Function<RandomSource, BlockState> randStateGetter, BoundingBox bounds, Rotation rotation, RandomSource rand)
	{
		int offsetX = rotation == Rotation.CLOCKWISE_180 || this.rotation == Rotation.CLOCKWISE_90 ? -(getSize().getX() - 1) : 0;
		int offsetZ = rotation == Rotation.CLOCKWISE_180 || this.rotation == Rotation.COUNTERCLOCKWISE_90 ? -(getSize().getZ() - 1) : 0;

		for (int x = 0; x < 13; x++)
		{
			for (int z = 0; z < 13; z++)
			{
				if (!level.getBlockState(position.offset(x + offsetX, 0, z + offsetZ)).isAir())
				{
					int offsetY = -1;
					while ((level.isEmptyBlock(position.offset(x + offsetX, offsetY, z + offsetZ)) || level.getBlockState(position.offset(x + offsetX, offsetY, z + offsetZ)).liquid()) && position.getY() + offsetY > 0)
					{
						level.setBlock(position.offset(x + offsetX, offsetY, z + offsetZ), randStateGetter.apply(rand), 2);
						--offsetY;
					}
				}
			}
		}
	}
}
