package com.legacy.structure_gel.core.block_entity;

import net.minecraft.core.HolderLookup;
import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.api.data_handler.DataHandlerType;
import com.legacy.structure_gel.api.data_handler.handlers.DataHandler;
import com.legacy.structure_gel.api.registry.StructureGelRegistries;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.registry.SGRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DataHandlerBlockEntity extends SGBlockEntity {
   public static final String HANDLERS_KEY = "handlers";
   public static final String USE_GRAVITY_KEY = "use_gravity";
   public static final String MARK_POST_PROCESSING_KEY = "mark_post_processing";
   public static final String OFFSET_KEY = "offset";
   public static final String CUSTOM_NAME_KEY = "CustomName";
   private WeightedRandomList<DataHandlerBlockEntity.RawHandler> handlers = WeightedRandomList.create();
   private Component name = null;
   private boolean useGravity = false;
   private boolean markPostProcessing = false;
   private Vec3 offset = Vec3.ZERO;

   public DataHandlerBlockEntity(BlockPos pos, BlockState state) {
      super(SGRegistry.BlockEntities.DATA_HANDLER.get(), pos, state);
   }

   public WeightedRandomList<DataHandlerBlockEntity.RawHandler> getHandlers() {
      return this.handlers;
   }

   public void setHandlers(WeightedRandomList<DataHandlerBlockEntity.RawHandler> handlers) {
      this.handlers = handlers;
      this.setChanged();
   }

   @Nullable
   public Component getCustomName() {
      return this.name;
   }

   public void setCustomName(Component name) {
      this.name = name;
   }

   public boolean useGravity() {
      return this.useGravity;
   }

   public void setUseGravity(boolean useGravity) {
      this.useGravity = useGravity;
   }

   public boolean shouldMarkPostProcessing() {
      return this.markPostProcessing;
   }

   public void setMarkPostProcessing(boolean markPostProcessing) {
      this.markPostProcessing = markPostProcessing;
   }

   public Vec3 getOffset() {
      return this.offset;
   }

   public void setOffset(Vec3 offset) {
      this.offset = offset;
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider p) {
      super.saveAdditional(tag, p);
      ListTag listTag = new ListTag();

      for (DataHandlerBlockEntity.RawHandler handler : this.handlers.unwrap()) {
         CompoundTag handlerTag = new CompoundTag();
         handler.trySave(handlerTag);
         listTag.add(handlerTag);
      }

      tag.put("handlers", listTag);
      if (this.name != null) {
         tag.putString("CustomName", Serializer.toJson(this.name));
      }

      tag.putBoolean("use_gravity", this.useGravity);
      tag.putBoolean("mark_post_processing", this.markPostProcessing);
      CompoundTag offsetTag = new CompoundTag();
      offsetTag.putString("facing", ((Direction)this.getBlockState().getValue(DataHandlerBlock.FACING)).getSerializedName());
      offsetTag.putDouble("x", this.offset.x);
      offsetTag.putDouble("y", this.offset.y);
      offsetTag.putDouble("z", this.offset.z);
      tag.put("offset", offsetTag);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider p) {
      super.loadAdditional(tag, p);
      this.handlers = loadAllHandlers(tag);
      if (tag.contains("CustomName", 8)) {
         this.name = Serializer.fromJson(tag.getString("CustomName"));
      } else {
         this.name = null;
      }

      this.useGravity = tag.getBoolean("use_gravity");
      this.markPostProcessing = tag.getBoolean("mark_post_processing");
      this.offset = loadOffset(tag);
   }

   public static WeightedRandomList<DataHandlerBlockEntity.RawHandler> loadAllHandlers(CompoundTag tag) {
      List<DataHandlerBlockEntity.RawHandler> handlers = new ArrayList<>();
      Optional<DataHandlerBlockEntity.RawHandler> legacyHandler = DataHandlerBlockEntity.RawHandler.tryLoad(tag);
      if (legacyHandler.isPresent()) {
         handlers.add(legacyHandler.get());
      }

      handlers.addAll(readHandlerList(tag));
      return WeightedRandomList.create(handlers);
   }

   public static boolean loadUseGravity(CompoundTag tag) {
      return tag.getBoolean("use_gravity");
   }

   public static boolean loadMarkPostProcessing(CompoundTag tag) {
      return tag.getBoolean("mark_post_processing");
   }

   public static Vec3 loadOffset(CompoundTag tag) {
      CompoundTag offsetTag = tag.getCompound("offset");
      return new Vec3(offsetTag.getDouble("x"), offsetTag.getDouble("y"), offsetTag.getDouble("z"));
   }

   private static List<DataHandlerBlockEntity.RawHandler> readHandlerList(CompoundTag tag) {
      List<DataHandlerBlockEntity.RawHandler> handlerList = new ArrayList<>();
      if (tag.contains("handlers", 9)) {
         for (Tag t : tag.getList("handlers", 10)) {
            if (t instanceof CompoundTag handlerTag) {
               Optional<DataHandlerBlockEntity.RawHandler> handler = DataHandlerBlockEntity.RawHandler.tryLoad(handlerTag);
               if (handler.isPresent()) {
                  handlerList.add(handler.get());
               }
            }
         }
      }

      return handlerList;
   }

   public record RawHandler(Weight weight, ResourceLocation typeName, LinkedHashMap<String, String> dataEntries) implements WeightedEntry {
      public static final DataHandlerBlockEntity.RawHandler EMPTY = new DataHandlerBlockEntity.RawHandler(
         Weight.of(1), ResourceLocation.parse(""), new LinkedHashMap<>()
      );
      public static final String WEIGHT_KEY = "weight";
      public static final String TYPE_KEY = "type";
      public static final String ENTRIES_KEY = "entries";

      public static Optional<DataHandlerBlockEntity.RawHandler> tryLoad(CompoundTag tag) {
         ResourceLocation type = readType(tag);
         if (type == null) {
            return Optional.empty();
         }

         int weight = tag.contains("weight", 3) ? tag.getInt("weight") : 1;
         LinkedHashMap<String, String> data = readDataEntries(tag);
         return Optional.of(new DataHandlerBlockEntity.RawHandler(Weight.of(weight), type, data));
      }

      @Nullable
      private static ResourceLocation readType(CompoundTag tag) {
         if (tag.contains("type", 8)) {
            String str = tag.getString("type");
            if (LegacyRL.isValid(str)) {
               return ResourceLocation.parse(str);
            }
         }

         return null;
      }

      private static LinkedHashMap<String, String> readDataEntries(CompoundTag tag) {
         LinkedHashMap<String, String> entries = new LinkedHashMap<>();
         CompoundTag dataTag = tag.getCompound("entries");

         for (String key : dataTag.getAllKeys()) {
            entries.put(key, dataTag.getString(key));
         }

         return entries;
      }

      public void trySave(CompoundTag tag) {
         if (!this.typeName.getPath().isEmpty()) {
            if (this.weight.asInt() != 1) {
               tag.putInt("weight", this.weight.asInt());
            }

            if (this.typeName != null) {
               tag.putString("type", this.typeName.toString());
            }

            CompoundTag dataTag = new CompoundTag();

            for (Entry<String, String> entry : this.dataEntries.entrySet()) {
               String value = entry.getValue();
               if (!value.isEmpty()) {
                  dataTag.putString(entry.getKey(), entry.getValue());
               }
            }

            tag.put("entries", dataTag);
         }
      }

      @Nullable
      public DataHandler<?> tryBake(BlockPos pos, LevelReader level) {
         try {
            if (this.typeName != null) {
               DataHandlerType<?> type = (DataHandlerType<?>)StructureGelRegistries.DATA_HANDLER_TYPE.get(this.typeName);
               if (type != null) {
                  return type.create(type.getDataParser().parse(this.dataEntries, level));
               }

               StructureGelMod.LOGGER.error("{} is not a registered DataHandlerType", this.typeName);
            } else {
               StructureGelMod.LOGGER.error("\type\" was null for the DataHandlerBlockEntity at {}", pos);
            }
         } catch (Exception e) {
            StructureGelMod.LOGGER.error("Couldn't deserialize DataHandler from {}", pos);
            StructureGelMod.LOGGER.error(e);
         }

         return null;
      }

      public Weight getWeight() {
         return this.weight;
      }
   }
}
