package com.legacy.structure_gel.core.block_entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DataHandlerBlockEntity extends SGBlockEntity/* implements IRotatable*/
{
	private WeightedRandomList<RawHandler> handlers = WeightedRandomList.create();
	private Component name = null;
	private boolean useGravity = false, markPostProcessing = false;
	private Vec3 offset = Vec3.ZERO;

	public DataHandlerBlockEntity(BlockPos pos, BlockState state)
	{
		super(SGRegistry.BlockEntities.DATA_HANDLER.get(), pos, state);
	}

	public WeightedRandomList<RawHandler> getHandlers()
	{
		return this.handlers;
	}

	public void setHandlers(WeightedRandomList<RawHandler> handlers)
	{
		this.handlers = handlers;
		this.setChanged();
	}

	@Nullable
	public Component getCustomName()
	{
		return this.name;
	}

	public void setCustomName(@Nullable Component name)
	{
		this.name = name;
	}

	public boolean useGravity()
	{
		return this.useGravity;
	}

	public void setUseGravity(boolean useGravity)
	{
		this.useGravity = useGravity;
	}

	public boolean shouldMarkPostProcessing()
	{
		return this.markPostProcessing;
	}

	public void setMarkPostProcessing(boolean markPostProcessing)
	{
		this.markPostProcessing = markPostProcessing;
	}

	public Vec3 getOffset()
	{
		return this.offset;
	}

	public void setOffset(Vec3 offset)
	{
		this.offset = offset;
	}

	/*private Direction testDir = Direction.NORTH;
	
	@Override
	public void rotate(Rotation rotation)
	{
		this.testDir = rotation.rotate(this.testDir);
	}
	
	@Override
	public void mirror(Mirror mirror)
	{
		this.testDir = mirror.mirror(this.testDir);
	}
	
	@Override
	public void transform(Mirror mirror, Rotation rotation)
	{
		System.out.println("Transformed data handler");
		IRotatable.super.transform(mirror, rotation);
	}*/
	
	public static final String HANDLERS_KEY = "handlers", USE_GRAVITY_KEY = "use_gravity",
			MARK_POST_PROCESSING_KEY = "mark_post_processing", OFFSET_KEY = "offset";
	/** Uppercase intentionally */
	public static final String CUSTOM_NAME_KEY = "CustomName";

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup)
	{
		//tag.putString("test_dir", this.testDir.getSerializedName());

		super.saveAdditional(tag, lookup);

		ListTag listTag = new ListTag();
		for (var handler : this.handlers.unwrap())
		{
			CompoundTag handlerTag = new CompoundTag();
			handler.trySave(handlerTag);
			listTag.add(handlerTag);
		}
		tag.put(HANDLERS_KEY, listTag);

		if (this.name != null)
		{
			tag.putString(CUSTOM_NAME_KEY, Component.Serializer.toJson(this.name, lookup));
		}

		tag.putBoolean(USE_GRAVITY_KEY, this.useGravity);
		tag.putBoolean(MARK_POST_PROCESSING_KEY, this.markPostProcessing);

		CompoundTag offsetTag = new CompoundTag();
		//offsetTag.putString("facing", this.getBlockState().getValue(DataHandlerBlock.FACING).getSerializedName());
		offsetTag.putDouble("x", this.offset.x);
		offsetTag.putDouble("y", this.offset.y);
		offsetTag.putDouble("z", this.offset.z);
		tag.put(OFFSET_KEY, offsetTag);
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup)
	{
		//System.out.println("Loaded data handler");
		//this.testDir = tag.contains("test_dir") ? Direction.byName(tag.getString("test_dir")) : Direction.NORTH;

		super.loadAdditional(tag, lookup);
		this.handlers = loadAllHandlers(tag);

		if (tag.contains(CUSTOM_NAME_KEY, Tag.TAG_STRING))
		{
			this.name = Component.Serializer.fromJson(tag.getString(CUSTOM_NAME_KEY), lookup);
		}
		else
		{
			this.name = null;
		}

		this.useGravity = tag.getBoolean(USE_GRAVITY_KEY); // False by default
		this.markPostProcessing = tag.getBoolean(MARK_POST_PROCESSING_KEY); // False by default
		this.offset = loadOffset(tag);
	}

	public static WeightedRandomList<RawHandler> loadAllHandlers(CompoundTag tag)
	{
		List<RawHandler> handlers = new ArrayList<>();

		// Reads legacy data
		Optional<RawHandler> legacyHandler = RawHandler.tryLoad(tag);
		if (legacyHandler.isPresent())
			handlers.add(legacyHandler.get());

		handlers.addAll(readHandlerList(tag));
		return WeightedRandomList.create(handlers);
	}

	public static boolean loadUseGravity(CompoundTag tag)
	{
		return tag.getBoolean(USE_GRAVITY_KEY);
	}

	public static boolean loadMarkPostProcessing(CompoundTag tag)
	{
		return tag.getBoolean(MARK_POST_PROCESSING_KEY);
	}

	public static Vec3 loadOffset(CompoundTag tag)
	{
		CompoundTag offsetTag = tag.getCompound(OFFSET_KEY);
		return new Vec3(offsetTag.getDouble("x"), offsetTag.getDouble("y"), offsetTag.getDouble("z"));
	}

	private static List<RawHandler> readHandlerList(CompoundTag tag)
	{
		List<RawHandler> handlerList = new ArrayList<>();
		if (tag.contains(HANDLERS_KEY, Tag.TAG_LIST))
		{
			ListTag listTag = tag.getList(HANDLERS_KEY, Tag.TAG_COMPOUND);
			for (Tag t : listTag)
			{
				if (t instanceof CompoundTag handlerTag)
				{
					Optional<RawHandler> handler = RawHandler.tryLoad(handlerTag);
					if (handler.isPresent())
						handlerList.add(handler.get());
				}
			}
		}
		return handlerList;
	}

	public static record RawHandler(Weight weight, ResourceLocation typeName, LinkedHashMap<String, String> dataEntries) implements WeightedEntry
	{

		public static final RawHandler EMPTY = new RawHandler(Weight.of(1), ResourceLocation.withDefaultNamespace(""), new LinkedHashMap<>());
		public static final String WEIGHT_KEY = "weight", TYPE_KEY = "type", ENTRIES_KEY = "entries";

		public static Optional<RawHandler> tryLoad(CompoundTag tag)
		{
			var type = readType(tag);
			if (type == null)
				return Optional.empty();

			int weight = tag.contains(WEIGHT_KEY, Tag.TAG_INT) ? tag.getInt("weight") : 1;
			var data = readDataEntries(tag);
			return Optional.of(new RawHandler(Weight.of(weight), type, data));
		}

		@Nullable
		private static ResourceLocation readType(CompoundTag tag)
		{
			if (tag.contains(TYPE_KEY, Tag.TAG_STRING))
			{
				String str = tag.getString(TYPE_KEY);
				ResourceLocation name = ResourceLocation.tryParse(str);
				if (name != null)
					return name;
			}
			return null;
		}

		private static LinkedHashMap<String, String> readDataEntries(CompoundTag tag)
		{
			LinkedHashMap<String, String> entries = new LinkedHashMap<>();
			CompoundTag dataTag = tag.getCompound(ENTRIES_KEY);
			for (var key : dataTag.getAllKeys())
				entries.put(key, dataTag.getString(key));
			return entries;
		}

		public void trySave(CompoundTag tag)
		{
			// Never save a data handler without a valid path
			if (this.typeName.getPath().isEmpty())
				return;

			if (this.weight.asInt() != 1)
				tag.putInt(WEIGHT_KEY, this.weight.asInt());

			if (this.typeName != null)
				tag.putString(TYPE_KEY, this.typeName.toString());

			CompoundTag dataTag = new CompoundTag();
			for (var entry : this.dataEntries.entrySet())
			{
				String value = entry.getValue();
				if (!value.isEmpty())
					dataTag.putString(entry.getKey(), entry.getValue());
			}
			tag.put(ENTRIES_KEY, dataTag);
		}

		@Nullable
		public DataHandler<?> tryBake(BlockPos pos, LevelReader level)
		{
			try
			{
				if (this.typeName != null)
				{
					DataHandlerType<?> type = StructureGelRegistries.DATA_HANDLER_TYPE.get(this.typeName);
					if (type != null)
					{
						return type.create(type.getDataParser(level.registryAccess()).parse(this.dataEntries, level));
					}
					else
					{
						StructureGelMod.LOGGER.error("{} is not a registered DataHandlerType", this.typeName);
					}
				}
				else
				{
					StructureGelMod.LOGGER.error("\type\" was null for the DataHandlerBlockEntity at {}", pos);
				}
			}
			catch (Exception e)
			{
				StructureGelMod.LOGGER.error("Couldn't deserialize DataHandler from {}", pos);
				StructureGelMod.LOGGER.error(e);
			}
			return null;
		}

		@Override
		public Weight getWeight()
		{
			return this.weight;
		}
	}
}
