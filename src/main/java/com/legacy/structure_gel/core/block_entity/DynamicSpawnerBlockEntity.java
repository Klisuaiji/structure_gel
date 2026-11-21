package com.legacy.structure_gel.core.block_entity;

import com.legacy.structure_gel.api.dynamic_spawner.DynamicSpawnerType;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.dynamic_spawner.DynamicSpawner;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.mojang.datafixers.util.Either;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.state.BlockState;

public class DynamicSpawnerBlockEntity extends SGBlockEntity implements Spawner
{
	public static final String SPAWNER_ID_KEY = "SpawnerID", SPAWNER_KEY = "Spawner", EXP_RANGE_KEY = "exp_range";

	private ResourceLocation spawnerID = SGRegistry.DynamicSpawnerTypes.DEFAULT.getName();
	private final DynamicSpawner spawner = new DynamicSpawner()
	{
		public com.mojang.datafixers.util.Either<net.minecraft.world.level.block.entity.BlockEntity, net.minecraft.world.entity.Entity> getOwner()
		{
			return Either.left(DynamicSpawnerBlockEntity.this);
		}
	};
	private ExpRange expRange = ExpRange.VANILLA;

	public DynamicSpawnerBlockEntity(BlockPos pos, BlockState state)
	{
		super(SGRegistry.BlockEntities.DYNAMIC_SPAWNER.get(), pos, state);
	}

	public void setSpawner(DynamicSpawnerType type, HolderLookup.Provider lookup)
	{
		ResourceLocation newName = StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.getKey(type);
		if (newName != null && !newName.equals(this.spawnerID))
		{
			this.spawnerID = newName;
			this.spawner.copyFrom(type.create(lookup));
		}
	}

	@Override
	public void setEntityId(EntityType<?> entityType, RandomSource random)
	{
	}

	public ResourceLocation getSpawnerID()
	{
		return this.spawnerID;
	}

	public void setExpRange(ExpRange maxInclusive)
	{
		this.expRange = maxInclusive;
		this.setChanged();
	}

	public ExpRange getExpRange()
	{
		return this.expRange;
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup)
	{
		super.loadAdditional(tag, lookup);
		if (tag.contains(SPAWNER_ID_KEY, Tag.TAG_STRING))
		{
			ResourceLocation typeName = ResourceLocation.parse(tag.getString(SPAWNER_ID_KEY));
			if (tag.contains(SPAWNER_KEY, Tag.TAG_COMPOUND)) // Exists when data is sent from server to client
			{
				this.spawnerID = typeName;
				this.spawner.load(this.level, this.worldPosition, tag.getCompound(SPAWNER_KEY));
			}
			else
			{
				this.setSpawner(StructureGelRegistries.DYNAMIC_SPAWNER_TYPE.getValue(typeName), lookup);
			}
		}
		if (tag.contains(EXP_RANGE_KEY, Tag.TAG_COMPOUND))
			this.expRange = ExpRange.load(tag.getCompound(EXP_RANGE_KEY));
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup)
	{
		super.saveAdditional(tag, lookup);
		if (this.spawnerID != null)
			tag.putString(SPAWNER_ID_KEY, this.spawnerID.toString());
		tag.put(EXP_RANGE_KEY, this.expRange.save());
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this, (b, r) ->
		{
			CompoundTag tag = this.saveWithoutMetadata(r);
			CompoundTag spawnerTag = this.spawner.save(new CompoundTag());
			tag.put(SPAWNER_KEY, spawnerTag);
			return tag;
		});
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, DynamicSpawnerBlockEntity blockEntity)
	{
		blockEntity.spawner.clientTick(level, pos);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, DynamicSpawnerBlockEntity blockEntity)
	{
		blockEntity.spawner.serverTick((ServerLevel) level, pos);
	}

	@Override
	public boolean triggerEvent(int paramA, int paramB)
	{
		return this.spawner.onEventTriggered(this.level, paramA) ? true : super.triggerEvent(paramA, paramB);
	}

	public DynamicSpawner getSpawner()
	{
		return this.spawner;
	}

	public static class ExpRange
	{
		public static final String MIN_KEY = "min", MAX_KEY = "max";
		/**
		 * Values based on vanilla mob spawner
		 */
		public static final ExpRange VANILLA = new ExpRange(15, 43);
		public final int min, max;

		public ExpRange(int minInclusive, int maxInclusive)
		{
			this.min = Math.max(0, minInclusive);
			this.max = Math.max(this.min, maxInclusive);
		}

		public static ExpRange load(CompoundTag tag)
		{
			if (tag.contains(MIN_KEY, Tag.TAG_INT) && tag.contains(MAX_KEY, Tag.TAG_INT))
				return new ExpRange(tag.getInt(MIN_KEY), tag.getInt(MAX_KEY));
			return ExpRange.VANILLA;
		}

		public CompoundTag save()
		{
			CompoundTag tag = new CompoundTag();
			tag.putInt(MIN_KEY, this.min);
			tag.putInt(MAX_KEY, this.max);
			return tag;
		}
	}
}