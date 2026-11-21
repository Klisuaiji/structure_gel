package com.legacy.structure_gel.api.util;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.hash.HashingOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class NetworkUtil {
   public static <T> void writeOptional(FriendlyByteBuf buff, Optional<T> optional, Consumer<T> write) {
      buff.writeBoolean(optional.isPresent());
      if (optional.isPresent()) {
         write.accept(optional.get());
      }
   }

   public static <T> Optional<T> readOptional(FriendlyByteBuf buff, Supplier<T> read, Supplier<T> defaultVal) {
      return Optional.ofNullable(buff.readBoolean() ? read.get() : defaultVal.get());
   }

   public static void writeCompressedNbt(FriendlyByteBuf buff, HashFunction hashFunction, CompoundTag tag) {
      ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
      HashingOutputStream hasingOutputStream = new HashingOutputStream(hashFunction, byteArrayOutput);

      byte[] bytes;
      try {
         NbtIo.writeCompressed(tag, hasingOutputStream);
         bytes = byteArrayOutput.toByteArray();
      } catch (IOException e) {
         e.printStackTrace();
         bytes = new byte[0];
      }

      buff.writeInt(bytes.length);
      if (bytes.length > 0) {
         buff.writeBytes(bytes);
      }
   }

   public static void writeCompressedNbt(FriendlyByteBuf buff, CompoundTag tag) {
      writeCompressedNbt(buff, defaultHashFunc(), tag);
   }

   public static CompoundTag readCompressedNbt(FriendlyByteBuf buff, HashFunction hashFunction, Supplier<CompoundTag> defaultVal) {
      int size = buff.readInt();
      if (size > 0) {
         byte[] bytes = new byte[size];
         buff.readBytes(bytes);
         ByteArrayInputStream byteInputStream = new ByteArrayInputStream(bytes);
         HashingInputStream hasingInput = new HashingInputStream(hashFunction, byteInputStream);

         try {
            return NbtIo.readCompressed(hasingInput, NbtAccounter.unlimitedHeap());
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return defaultVal.get();
   }

   public static CompoundTag readCompressedNbt(FriendlyByteBuf buff, Supplier<CompoundTag> defaultVal) {
      return readCompressedNbt(buff, defaultHashFunc(), defaultVal);
   }

   public static CompoundTag readCompressedNbt(FriendlyByteBuf buff) {
      return readCompressedNbt(buff, CompoundTag::new);
   }

   private static HashFunction defaultHashFunc() {
      return Hashing.sha1();
   }

   public static void writeBoundingBox(FriendlyByteBuf buff, BoundingBox bounds) {
      buff.writeInt(bounds.minX());
      buff.writeInt(bounds.minY());
      buff.writeInt(bounds.minZ());
      buff.writeInt(bounds.maxX());
      buff.writeInt(bounds.maxY());
      buff.writeInt(bounds.maxZ());
   }

   public static BoundingBox readBoundingBox(FriendlyByteBuf buff) {
      return new BoundingBox(buff.readInt(), buff.readInt(), buff.readInt(), buff.readInt(), buff.readInt(), buff.readInt());
   }
}
