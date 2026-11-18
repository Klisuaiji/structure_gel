package com.legacy.structure_gel.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class GetSpawnsCommand {
   public static LiteralArgumentBuilder<CommandSourceStack> get() {
      LiteralArgumentBuilder<CommandSourceStack> command = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal("getspawns")
            .requires(source -> source.hasPermission(2)))
         .executes(context -> getSpawns(context, MobCategory.values()));

      for (MobCategory category : MobCategory.values()) {
         command.then(Commands.literal(category.getName()).executes(context -> getSpawns(context, category)));
      }

      return command;
   }

   private static int getSpawns(CommandContext<CommandSourceStack> context, MobCategory... categories) {
      Map<MobCategory, WeightedRandomList<SpawnerData>> map = new LinkedHashMap<>();

      for (MobCategory category : categories) {
         WeightedRandomList<SpawnerData> list = getSpawnList(context, category);
         if (!list.isEmpty()) {
            map.put(category, list);
         }
      }

      if (!map.isEmpty()) {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("--Spawn Data--"), true);
         map.entrySet().stream().sorted(new GetSpawnsCommand.ClassificationComparator<>()).forEach(e -> printSpawns(context, e.getKey(), e.getValue()));
      } else {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("No spawn data."), true);
      }

      return 1;
   }

   private static WeightedRandomList<SpawnerData> getSpawnList(CommandContext<CommandSourceStack> context, MobCategory category) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      BlockPos pos = BlockPos.containing(((CommandSourceStack)context.getSource()).getPosition());
      Holder<Biome> biome = level.getBiome(pos);
      ServerChunkCache chunkSource = level.getChunkSource();
      ChunkGenerator chunkGen = chunkSource.getGenerator();
      StructureManager manager = level.structureManager();
      return chunkGen.getMobsAt(biome, manager, category, pos);
   }

   private static void printSpawns(CommandContext<CommandSourceStack> context, MobCategory category, WeightedRandomList<SpawnerData> spawns) {
      if (!spawns.isEmpty()) {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("[" + category.getName() + "]").withStyle(ChatFormatting.GREEN), true);
         spawns.unwrap()
            .stream()
            .sorted(new GetSpawnsCommand.SpawnInfoComparator<>())
            .forEach(
               spawn -> ((CommandSourceStack)context.getSource())
                  .sendSuccess(
                     () -> Component.literal(
                        String.format(
                           " - %s, weight:%d, min:%d, max:%d",
                           BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type),
                           spawn.getWeight().asInt(),
                           spawn.minCount,
                           spawn.maxCount
                        )
                     ),
                     true
                  )
            );
      }
   }

   private static class ClassificationComparator<T extends Entry<MobCategory, WeightedRandomList<SpawnerData>>> implements Comparator<T> {
      public int compare(T o1, T o2) {
         return o1.getKey().getName().compareTo(o2.getKey().getName());
      }
   }

   private static class SpawnInfoComparator<T extends SpawnerData> implements Comparator<T> {
      public int compare(T o1, T o2) {
         ResourceLocation n1 = BuiltInRegistries.ENTITY_TYPE.getKey(o1.type);
         ResourceLocation n2 = BuiltInRegistries.ENTITY_TYPE.getKey(o2.type);
         return n1 != null && n2 != null ? n1.toString().compareTo(n2.toString()) : 0;
      }
   }
}
