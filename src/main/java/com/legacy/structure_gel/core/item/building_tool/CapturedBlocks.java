package com.legacy.structure_gel.core.item.building_tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.legacy.structure_gel.core.SGConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
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

public class CapturedBlocks
{
	public static final Codec<CapturedBlocks> CODEC = RecordCodecBuilder.create(instance ->
	{
		//@formatter:off
		return instance.group(
				BlockPos.CODEC.fieldOf("world_pos").forGetter(CapturedBlocks::getWorldPos),
				BlockPos.CODEC.fieldOf("center_offset").forGetter(CapturedBlocks::getCenterPos),
				BoundingBox.CODEC.fieldOf("bounds").forGetter(CapturedBlocks::getBounds),
				BlockInfo.CODEC.listOf().fieldOf("block_infos").forGetter(CapturedBlocks::getBlockInfos),
				EntityInfo.CODEC.listOf().fieldOf("entity_infos").forGetter(CapturedBlocks::getEntityInfos),
				Mirror.CODEC.fieldOf("mirror").forGetter(CapturedBlocks::getMirror),
				Rotation.CODEC.fieldOf("rotation").forGetter(CapturedBlocks::getRotation)).apply(instance, CapturedBlocks::new);
		//@formatter:on
	});

	private final BlockPos worldPos;
	private final BlockPos centerOffset;
	private final BoundingBox bounds;
	private List<BlockInfo> blockInfos;
	private final List<EntityInfo> entityInfos;
	private final Mirror mirror;
	private final Rotation rotation;

	@Nullable // Used as a cache, not saved
	private CapturedBlocks lastTransform = null;
	@Nullable
	private CapturedBlocks clientCopy = null;
	@Nullable
	private Map<BlockPos, VoxelShape> shapeCache = null;
	@Nullable
	private List<AABB> entityBoxesCache = null;

	private CapturedBlocks(BlockPos worldPos, BlockPos centerOffset, BoundingBox bounds, List<BlockInfo> blockInfos, List<EntityInfo> entityInfos, Mirror mirror, Rotation rotation)
	{
		this.worldPos = worldPos;
		this.centerOffset = centerOffset;
		this.bounds = bounds;
		this.blockInfos = blockInfos;
		this.entityInfos = entityInfos;
		this.mirror = mirror;
		this.rotation = rotation;
	}

	public CapturedBlocks copy()
	{
		return new CapturedBlocks(this.worldPos, this.centerOffset, this.bounds, this.blockInfos, this.entityInfos, this.mirror, this.rotation);
	}

	public CapturedBlocks clientCopy()
	{
		if (this.clientCopy == null)
		{
			this.clientCopy = this.copy();
		}
		return this.clientCopy;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		else if (obj instanceof CapturedBlocks other)
		{
			if (!worldPos.equals(other.worldPos))
				return false;
			if (!centerOffset.equals(other.centerOffset))
				return false;
			if (!bounds.equals(other.bounds))
				return false;
			if (!mirror.equals(other.mirror))
				return false;
			if (!rotation.equals(other.rotation))
				return false;
			if (!blockInfos.equals(other.blockInfos))
				return false;
			if (!entityInfos.equals(other.entityInfos))
				return false;

			return true;
		}
		return false;
	}

	public CapturedBlocks(Level level, BlockPos cornerA, BlockPos cornerB, boolean includeEntities)
	{
		this(level, cornerA, cornerB, Mirror.NONE, Rotation.NONE, includeEntities);
	}

	public CapturedBlocks(Level level, BlockPos cornerA, BlockPos cornerB, Mirror mirror, Rotation rotation, boolean includeEntities)
	{
		BoundingBox bounds = BoundingBox.fromCorners(cornerA, cornerB);
		List<BlockInfo> infos = new LinkedList<>();
		int minX = bounds.minX();
		int minY = bounds.minY();
		int minZ = bounds.minZ();
		int maxX = bounds.maxX();
		int maxY = bounds.maxY();
		int maxZ = bounds.maxZ();
		Vec3i bbLength = bounds.getLength();
		Vec3i transformedBBLength = bbLength;
		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; y <= maxY; y++)
			{
				for (int z = minZ; z <= maxZ; z++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = level.getBlockState(pos);
					BlockEntity blockEntity = level.getBlockEntity(pos);
					CompoundTag blockEntityTag = blockEntity != null ? blockEntity.saveWithoutMetadata(level.registryAccess()) : null;
					BlockPos transformedPos = this.transformPos(new BlockPos(x - minX, y - minY, z - minZ), bbLength, mirror, rotation);
					infos.add(new BlockInfo(transformedPos, state.mirror(mirror).rotate(rotation), Optional.ofNullable(blockEntityTag)));
				}
			}
		}

		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)
		{
			bounds = BoundingBox.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(minX + bbLength.getZ(), maxY, minZ + bbLength.getX()));
			transformedBBLength = bounds.getLength();
		}
		this.bounds = bounds;

		this.centerOffset = new BlockPos(-transformedBBLength.getX() + transformedBBLength.getX() / 2, 0, -transformedBBLength.getZ() + transformedBBLength.getZ() / 2);
		this.worldPos = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());

		if (includeEntities)
		{
			List<EntityInfo> entityInfos = new ArrayList<>();

			List<Entity> entities = level.getEntities(null, AABB.of(this.bounds));
			for (Entity entity : entities)
			{
				if (entity instanceof Player)
					continue;
				Vec3 pos = this.transformPos(entity.position().subtract(Vec3.atLowerCornerOf(this.worldPos)), bbLength, mirror, rotation);
				CompoundTag entityTag = new CompoundTag();
				entity.save(entityTag);
				entityTag.remove("UUID"); // Remove UUID as to not cause duplicates
				entityInfos.add(new EntityInfo(entity.getUUID(), pos, entityTag));
			}
			this.entityInfos = entityInfos;
		}
		else
		{
			this.entityInfos = Collections.emptyList();
		}

		this.blockInfos = new ArrayList<>(infos);
		this.mirror = mirror;
		this.rotation = rotation;
	}

	public CapturedBlocks withTransforms(Mirror mirror, Rotation rotation)
	{
		if (this.lastTransform != null && this.lastTransform.mirror == mirror && this.lastTransform.rotation == rotation)
			return this.lastTransform;

		BoundingBox bounds = this.bounds;
		List<BlockInfo> blockInfos = new ArrayList<>(this.blockInfos.size());
		int minX = bounds.minX();
		int minY = bounds.minY();
		int minZ = bounds.minZ();
		int maxY = bounds.maxY();
		Vec3i bbLength = bounds.getLength();
		Vec3i transformedBBLength = bbLength;

		for (BlockInfo info : this.blockInfos)
		{
			BlockPos pos = info.pos;
			BlockPos transformedPos = this.transformPos(pos, bbLength, mirror, rotation);
			blockInfos.add(new BlockInfo(transformedPos, info.state.mirror(mirror).rotate(rotation), info.blockEntityTag));
		}

		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)
		{
			bounds = BoundingBox.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(minX + bbLength.getZ(), maxY, minZ + bbLength.getX()));
			transformedBBLength = bounds.getLength();
		}

		BlockPos centerOffset = new BlockPos(-transformedBBLength.getX() + transformedBBLength.getX() / 2, 0, -transformedBBLength.getZ() + transformedBBLength.getZ() / 2);
		BlockPos worldPos = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());

		List<EntityInfo> entityInfos = new ArrayList<>();
		for (EntityInfo info : this.entityInfos)
		{
			Vec3 pos = info.pos;
			Vec3 transformedPos = this.transformPos(pos, bbLength, mirror, rotation);
			entityInfos.add(new EntityInfo(info.original, transformedPos, info.entityTag));
		}

		this.lastTransform = new CapturedBlocks(worldPos, centerOffset, bounds, blockInfos, entityInfos, mirror, rotation);
		return this.lastTransform;
	}

	boolean compressedForRender = false;

	public void compressForRender(Level level, BlockPos startPos, Player player)
	{
		if (this.compressedForRender)
			return;

		int maxTime = SGConfig.CLIENT.maxCloneCompileTime();
		boolean timeExceeded = false;
		long startTime = System.currentTimeMillis();
		//System.out.println("Start Compress");

		Map<BlockPos, BlockState> positions = this.blockInfos.stream().collect(Collectors.toMap(BlockInfo::pos, BlockInfo::state));
		List<BlockInfo> newBlockInfos = new ArrayList<>(this.blockInfos.size() / 3);
		for (BlockInfo info : this.blockInfos)
		{
			boolean remove = true;
			BlockPos pos = info.pos;
			for (Direction dir : Direction.values())
			{
				BlockPos offset = pos.relative(dir);
				BlockState offsetState = positions.get(offset);
				if (offsetState == null || !offsetState.isSolidRender())
				{
					remove = false;
					break;
				}
			}
			if (!remove)
				newBlockInfos.add(info);

			if (System.currentTimeMillis() - startTime > maxTime)
			{
				timeExceeded = true;
				break;
			}
		}

		if (timeExceeded)
		{
			player.displayClientMessage(Component.literal("[Structure Gel] A Building Tool render tool longer than " + maxTime + "ms to compile. Aborting.").withStyle(ChatFormatting.RED), false);
			this.blockInfos = new ArrayList<>(0);
		}
		else
		{
			this.blockInfos = newBlockInfos;
		}
		this.compressedForRender = true;
		//System.out.println("End Compress " + (System.currentTimeMillis() - startTime));
	}

	private BlockPos transformPos(BlockPos pos, Vec3i size, Mirror mirror, Rotation rotation)
	{
		if (mirror == Mirror.NONE && rotation == Rotation.NONE)
			return pos;

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		switch (mirror)
		{
		case LEFT_RIGHT:
			z = size.getZ() - z;
			break;
		case FRONT_BACK:
			x = size.getX() - x;
			break;
		default:
		}

		switch (rotation)
		{
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
			break;
		default:
		}

		return new BlockPos(x, y, z);
	}

	private Vec3 transformPos(Vec3 pos, Vec3i size, Mirror mirror, Rotation rotation)
	{
		if (mirror == Mirror.NONE && rotation == Rotation.NONE)
			return pos;
		size = size.offset(1, 1, 1);

		double x = pos.x();
		double y = pos.y();
		double z = pos.z();

		switch (mirror)
		{
		case LEFT_RIGHT:
			z = size.getZ() - z;
			break;
		case FRONT_BACK:
			x = size.getX() - x;
			break;
		default:
		}

		switch (rotation)
		{
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
			break;
		default:
		}

		//System.out.printf("[%d, %d, %d] %s -> %s\n", size.getX(), size.getY(), size.getZ(), pos, new Vec3(x, y, z));
		return new Vec3(x, y, z);
	}

	public BoundingBox getBounds()
	{
		return this.bounds;
	}

	public BlockPos getCenterPos()
	{
		return this.centerOffset;
	}

	public BlockPos getWorldPos()
	{
		return this.worldPos;
	}

	public Mirror getMirror()
	{
		return this.mirror;
	}

	public Rotation getRotation()
	{
		return this.rotation;
	}

	public List<BlockInfo> getBlockInfos()
	{
		return Collections.unmodifiableList(this.blockInfos);
	}

	public List<EntityInfo> getEntityInfos()
	{
		return Collections.unmodifiableList(this.entityInfos);
	}

	public Map<BlockPos, VoxelShape> getShapes(Level level, BlockPos worldPos)
	{
		if (this.shapeCache == null)
		{
			this.shapeCache = this.getBlockInfos().stream().collect(Collectors.toMap(b -> worldPos.offset(b.pos()), b -> b.state().getShape(level, b.pos())));
		}
		return this.shapeCache;
	}

	public List<AABB> getEntityBoxes(Level level, BlockPos worldPos)
	{
		if (this.entityBoxesCache == null)
		{
			this.entityBoxesCache = this.entityInfos.stream().map(info -> info.createEntity(level, worldPos, this.mirror, this.rotation)).filter(e -> e != null).map(Entity::getBoundingBox).map(aabb -> aabb.getSize() <= 0 ? AABB.ofSize(aabb.getCenter(), 0.75, 0.75, 0.75) : aabb).toList();
		}
		return this.entityBoxesCache;
	}

	private static final String PALETTE_KEY = "palette", DEFAULT_STATE_KEY = "default_state", BLOCKS_KEY = "blocks",
			STATE_KEY = "state", TAG_KEY = "tag", BOUNDS_KEY = "bounds", WORLD_POS_KEY = "world_pos",
			CENTER_OFFSET_KEY = "center_offset", MIRROR_KEY = "mirror", ROTATION_KEY = "rotation",
			ENTITIES_KEY = "entities", ENTITY_KEY = "entity", POS_KEY = "pos", UUID_KEY = "uuid";

	public CompoundTag toCompressedTag(HolderLookup.Provider registryAccess)
	{
		CompoundTag tag = new CompoundTag();

		// Each state should only be in here once, and have an index. The block with the most usage should be excluded and saved as "default"
		BiMap<BlockState, Integer> states = HashBiMap.create();
		Map<BlockState, MutableInt> stateUsage = new HashMap<>();
		for (BlockInfo info : this.blockInfos)
		{
			BlockState state = info.state;
			// Store states with their IDs. The first value should start at 0
			states.computeIfAbsent(state, i -> states.size());

			// If it doesn't have an nbt tag, make it a contender for most used
			if (!info.blockEntityTag.isPresent())
			{
				MutableInt count = stateUsage.get(state);
				if (count == null)
					stateUsage.put(state, new MutableInt(1));
				else
					count.increment();
			}
		}

		// We won't include the most used block later, so it can be used as the default. This figures out which is most common
		BlockState mostUsed = stateUsage.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).map(Map.Entry::getKey).findFirst().orElse(null);
		if (mostUsed != null)
		{
			tag.put(DEFAULT_STATE_KEY, NbtUtils.writeBlockState(mostUsed));
		}

		// Stored in order of creation. The state with an id of 0 should be first in the list on read and write
		ListTag paletteTag = new ListTag();
		for (var state : states.keySet())
			paletteTag.add(NbtUtils.writeBlockState(state));
		tag.put(PALETTE_KEY, paletteTag);

		// Store blocks in the blocksTag with their position as the key to store them under
		CompoundTag blocksTag = new CompoundTag();
		for (BlockInfo info : this.blockInfos)
		{
			// Skip mostUsed since it's a default
			if (info.state == mostUsed)
				continue;

			CompoundTag infoTag = new CompoundTag();
			infoTag.putInt(STATE_KEY, states.get(info.state));
			if (info.blockEntityTag.isPresent())
				infoTag.put(TAG_KEY, info.blockEntityTag.get());
			blocksTag.put(Long.toString(info.pos.asLong()), infoTag);
		}
		tag.put(BLOCKS_KEY, blocksTag);

		ListTag entitiesListTag = new ListTag();
		for (EntityInfo info : this.entityInfos)
		{
			CompoundTag infoTag = new CompoundTag();

			infoTag.putUUID(UUID_KEY, info.original);

			ListTag pos = new ListTag();
			pos.add(FloatTag.valueOf((float) info.pos.x));
			pos.add(FloatTag.valueOf((float) info.pos.y));
			pos.add(FloatTag.valueOf((float) info.pos.z));
			infoTag.put(POS_KEY, pos);

			infoTag.put(ENTITY_KEY, info.entityTag);

			entitiesListTag.add(infoTag);
		}
		tag.put(ENTITIES_KEY, entitiesListTag);

		// Store bounds as an array of 2 compressed positions
		tag.putLongArray(BOUNDS_KEY, new long[] { BlockPos.asLong(this.bounds.minX(), this.bounds.minY(), this.bounds.minZ()), BlockPos.asLong(this.bounds.maxX(), this.bounds.maxY(), this.bounds.maxZ()) });

		tag.putLong(WORLD_POS_KEY, this.worldPos.asLong());
		tag.putLong(CENTER_OFFSET_KEY, this.centerOffset.asLong());
		tag.putString(MIRROR_KEY, this.mirror.name()); // Using .name() because valueOf() uses that
		tag.putString(ROTATION_KEY, this.rotation.name()); // Using .name() because valueOf() uses that
		return tag;
	}

	public static CapturedBlocks fromCompressedTag(HolderLookup.Provider registryAccess, CompoundTag tag)
	{
		// Read the palette and store into an array
		HolderGetter<Block> blockRegistry = registryAccess.lookupOrThrow(Registries.BLOCK);
		BlockState[] palette = tag.getList(PALETTE_KEY, Tag.TAG_COMPOUND).stream().map(t -> NbtUtils.readBlockState(blockRegistry, (CompoundTag) t)).toArray(BlockState[]::new);
		BlockState defaultState = tag.contains(DEFAULT_STATE_KEY, Tag.TAG_COMPOUND) ? NbtUtils.readBlockState(blockRegistry, tag.getCompound(DEFAULT_STATE_KEY)) : null;

		long[] boundsCorners = tag.getLongArray(BOUNDS_KEY);
		BoundingBox bounds = BoundingBox.fromCorners(BlockPos.of(boundsCorners[0]), BlockPos.of(boundsCorners[1]));
		CompoundTag blocksTag = tag.getCompound(BLOCKS_KEY);
		List<BlockInfo> blockInfos = new ArrayList<>(bounds.getXSpan() * bounds.getYSpan() * bounds.getZSpan());

		for (int x = 0; x <= bounds.getXSpan() - 1; x++)
		{
			for (int z = 0; z <= bounds.getZSpan() - 1; z++)
			{
				for (int y = 0; y <= bounds.getYSpan() - 1; y++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					String l = Long.toString(pos.asLong());
					if (blocksTag.contains(l, Tag.TAG_COMPOUND))
					{
						CompoundTag infoTag = blocksTag.getCompound(l);
						BlockState state = palette[infoTag.getInt(STATE_KEY)];
						Optional<CompoundTag> beTag = Optional.ofNullable(infoTag.contains(TAG_KEY) ? infoTag.getCompound(TAG_KEY) : null);
						blockInfos.add(new BlockInfo(pos, state, beTag));
					}
					else if (defaultState != null)
					{
						blockInfos.add(new BlockInfo(pos, defaultState, Optional.empty()));
					}
				}
			}
		}

		ListTag entitiesListTag = tag.getList(ENTITIES_KEY, Tag.TAG_COMPOUND);
		List<EntityInfo> entityInfos = new ArrayList<>(entitiesListTag.size());
		for (Tag t : entitiesListTag)
		{
			if (t instanceof CompoundTag infoTag)
			{
				ListTag posTag = infoTag.getList(POS_KEY, Tag.TAG_FLOAT);
				entityInfos.add(new EntityInfo(infoTag.getUUID(UUID_KEY), new Vec3(posTag.getFloat(0), posTag.getFloat(1), posTag.getFloat(2)), infoTag.getCompound(ENTITY_KEY)));
			}
		}

		return new CapturedBlocks(BlockPos.of(tag.getLong(WORLD_POS_KEY)), BlockPos.of(tag.getLong(CENTER_OFFSET_KEY)), bounds, blockInfos, entityInfos, Mirror.valueOf(tag.getString(MIRROR_KEY)), Rotation.valueOf(tag.getString(ROTATION_KEY)));
	}

	public static record BlockInfo(BlockPos pos, BlockState state, Optional<CompoundTag> blockEntityTag)
	{
		public static final Codec<BlockInfo> CODEC = RecordCodecBuilder.create(instance ->
		{
			return instance.group(BlockPos.CODEC.fieldOf("pos").forGetter(BlockInfo::pos), BlockState.CODEC.fieldOf("state").forGetter(BlockInfo::state), CompoundTag.CODEC.optionalFieldOf("tag").forGetter(BlockInfo::blockEntityTag)).apply(instance, BlockInfo::new);
		});
	}

	public static record EntityInfo(UUID original, Vec3 pos, CompoundTag entityTag)
	{

		public static final Codec<EntityInfo> CODEC = RecordCodecBuilder.create(instance ->
		{
			return instance.group(Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("original").forGetter(EntityInfo::original), Vec3.CODEC.fieldOf("pos").forGetter(EntityInfo::pos), CompoundTag.CODEC.fieldOf("entity").forGetter(EntityInfo::entityTag)).apply(instance, EntityInfo::new);
		});

		@Nullable
		public Entity createEntity(Level level, BlockPos startPos, Mirror mirror, Rotation rotation)
		{
			Entity entity = EntityType.create(this.entityTag(), level, MobSpawnType.COMMAND).orElse(null);
			if (entity != null)
			{
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
