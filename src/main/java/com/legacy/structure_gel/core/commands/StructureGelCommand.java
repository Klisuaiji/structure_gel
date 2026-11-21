package com.legacy.structure_gel.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class StructureGelCommand {
   private static final DynamicCommandExceptionType INVALID_KEY = new DynamicCommandExceptionType(obj -> Component.literal(obj + " is not valid in registry."));

   public static <T> CompletableFuture<Suggestions> suggestFromRegistry(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, ResourceKey<Registry<T>> registry
   ) {
      ((Registry)((CommandSourceStack)context.getSource()).registryAccess().registry(registry).get())
         .keySet()
         .stream()
         .map(ResourceLocation::toString)
         .forEach(builder::suggest);
      return builder.buildFuture();
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("structure_gel");
      command.then(DebugCommand.get());
      LiteralArgumentBuilder<CommandSourceStack> help = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("?")
         .executes(
            context -> {
               ((CommandSourceStack)context.getSource())
                  .sendSuccess(() -> Component.literal("Explains the basic function of a command").withStyle(ChatFormatting.GRAY), false);
               return 1;
            }
         );
      command.then(help);
      addChild(command, SaveStructuresCommand.get(), Component.literal("Runs the Save operation on Structure Blocks in the area or found in the search"));
      addChild(command, GetSpawnsCommand.get(), Component.literal("Gets the mob spawns in the current location"));
      addChild(
         command, GetStructuresCommand.get(), Component.literal("Gets the structures that can generate in the provided biome, or the current one you're in")
      );
      addChild(command, GetBiomesCommand.get(), Component.literal("Gets the biomes that a structure can generate in"));
      addChild(
         command,
         ViewBoundsCommand.get(),
         Component.literal(
            "Sends structure bounding boxes to the client. The white box is the full bounds of the structure, while colored boxes are individual pieces"
         )
      );
      addChild(command, BuildingToolCommand.get(), Component.literal("Allows handling specific building tool actions and managing building tool data"));
      addChild(
         command,
         DistanceCommand.get(),
         Component.empty()
            .append(Component.literal("You should only run this command in testing worlds.").withStyle(ChatFormatting.RED))
            .append(
               Component.literal(
                  "\nSelects random positions in the world, locates the nearest structure, and reports the average distance. Larger sample sizes provide more accurate results."
               )
            )
      );
      addChild(command, GetFeaturesCommand.get(), Component.literal("Gets the features that can generate in the provided biome, or the current one you're in"));
      addChild(command, NightVisionCommand.get(), Component.literal("Shorthand to toggle night vision"));
      dispatcher.register(command);
   }

   public static LiteralArgumentBuilder<CommandSourceStack> addChild(
      LiteralArgumentBuilder<CommandSourceStack> parent, LiteralArgumentBuilder<CommandSourceStack> child, MutableComponent childHelp
   ) {
      if (childHelp != null) {
         parent.then(
            Commands.literal("?").then(((LiteralArgumentBuilder)Commands.literal(child.getLiteral()).requires(child.getRequirement())).executes(context -> {
               ((CommandSourceStack)context.getSource()).sendSuccess(() -> childHelp.withStyle(ChatFormatting.GRAY), false);
               return 1;
            }))
         );
      }

      parent.then(child);
      return parent;
   }

   public static <T> Holder<T> getResourceKeyHolder(CommandContext<CommandSourceStack> context, ResourceKey<Registry<T>> registryKey, String arg) throws CommandSyntaxException {
      ResourceKey<?> rawKey = (ResourceKey<?>)context.getArgument(arg, ResourceKey.class);
      ResourceKey<T> key = (ResourceKey<T>)rawKey.cast(registryKey).orElseThrow(() -> INVALID_KEY.create(rawKey));
      return (Holder<T>)((CommandSourceStack)context.getSource())
         .getServer()
         .registryAccess()
         .registryOrThrow(registryKey)
         .getHolder(key)
         .orElseThrow(() -> INVALID_KEY.create(key));
   }

   public static Map<BlockPos, StructureBlockEntity> findStructureBlocks(ServerLevel level, BlockPos origin, int radius) {
      Map<BlockPos, StructureBlockEntity> structureBlocks = findStructureBlocks(level, origin, radius, new LinkedHashMap<>());
      displayStructureBlockParticles(level, structureBlocks);
      return structureBlocks;
   }

   private static Map<BlockPos, StructureBlockEntity> findStructureBlocks(
      ServerLevel level, BlockPos origin, int radius, Map<BlockPos, StructureBlockEntity> structureBlocks
   ) {
      ChunkPos originChunk = new ChunkPos(origin);
      int cRadius = radius / 16 + 1;

      for (int cx = -cRadius; cx <= cRadius; cx++) {
         for (int cz = -cRadius; cz <= cRadius; cz++) {
            for (BlockPos pos : level.getChunk(originChunk.x + cx, originChunk.z + cz).getBlockEntitiesPos()) {
               if (inSquareRadius(origin, pos, radius)
                  && level.getBlockEntity(pos) instanceof StructureBlockEntity structureBlock
                  && structureBlock.getMode() == StructureMode.SAVE
                  && structureBlocks.put(pos, structureBlock) == null) {
                  findStructureBlocks(level, pos, radius, structureBlocks);
               }
            }
         }
      }

      return structureBlocks;
   }

   public static Map<BlockPos, StructureBlockEntity> findStructureBlocks(ServerLevel level, BoundingBox area) {
      Map<BlockPos, StructureBlockEntity> structureBlocks = new LinkedHashMap<>();

      for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
         if (level.getBlockEntity(pos) instanceof StructureBlockEntity structureBlock && structureBlock.getMode() == StructureMode.SAVE) {
            structureBlocks.put(pos, structureBlock);
         }
      }

      displayStructureBlockParticles(level, structureBlocks);
      return structureBlocks;
   }

   private static void displayStructureBlockParticles(ServerLevel level, Map<BlockPos, StructureBlockEntity> structureBlocks) {
      for (ServerPlayer player : level.players()) {
         for (Entry<BlockPos, StructureBlockEntity> entry : structureBlocks.entrySet()) {
            BlockPos above = entry.getKey().above();
            if (level.getBlockState(above).isAir()) {
               level.sendParticles(
                  player,
                  new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.REDSTONE_TORCH.defaultBlockState()),
                  true,
                  above.getX() + 0.5,
                  above.getY() + 0.5,
                  above.getZ() + 0.5,
                  1,
                  0.0,
                  0.0,
                  0.0,
                  0.0
               );
            }
         }
      }
   }

   private static boolean inSquareRadius(BlockPos a, BlockPos b, int r) {
      return Math.abs(a.getX() - b.getX()) <= r && Math.abs(a.getY() - b.getY()) <= r && Math.abs(a.getZ() - b.getZ()) <= r;
   }
}
