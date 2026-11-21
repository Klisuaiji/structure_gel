package com.legacy.structure_gel.core.commands;

import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class SaveStructuresCommand {
   public static LiteralArgumentBuilder<CommandSourceStack> get() {
      LiteralArgumentBuilder<CommandSourceStack> command = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("savestructures")
         .requires(source -> source.hasPermission(2));
      command.then(
         Commands.argument("from", BlockPosArgument.blockPos())
            .then(
               Commands.argument("to", BlockPosArgument.blockPos())
                  .executes(
                     context -> saveStructuresBounds(
                        context,
                        BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to"))
                     )
                  )
            )
      );
      command.then(
         ((LiteralArgumentBuilder)Commands.literal("search").executes(context -> saveStructuresSearch(context, 32)))
            .then(
               Commands.argument("radius", IntegerArgumentType.integer(0, 128))
                  .executes(context -> saveStructuresSearch(context, IntegerArgumentType.getInteger(context, "radius")))
            )
      );
      return command;
   }

   private static int saveStructuresBounds(CommandContext<CommandSourceStack> context, BoundingBox area) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      return save(context, StructureGelCommand.findStructureBlocks(level, area));
   }

   private static int saveStructuresSearch(CommandContext<CommandSourceStack> context, int radius) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      BlockPos pos = BlockPos.containing(((CommandSourceStack)context.getSource()).getPosition());
      return save(context, StructureGelCommand.findStructureBlocks(level, pos, radius));
   }

   private static int save(CommandContext<CommandSourceStack> context, Map<BlockPos, StructureBlockEntity> structureBlocks) {
      List<String> savedStructures = new ArrayList<>();
      Map<BlockPos, String> duplicates = new HashMap<>();

      for (Entry<BlockPos, StructureBlockEntity> entry : structureBlocks.entrySet()) {
         StructureBlockEntity structureBlock = entry.getValue();
         if (structureBlock.hasStructureName() && structureBlock.saveStructure()) {
            if (savedStructures.contains(structureBlock.getStructureName())) {
               duplicates.put(new BlockPos((Vec3i)entry.getKey()), structureBlock.getStructureName());
            }

            savedStructures.add(structureBlock.getStructureName());
         }
      }

      if (savedStructures.size() > 0) {
         ((CommandSourceStack)context.getSource())
            .sendSuccess(() -> Component.literal("[Saved " + savedStructures.size() + " Structures]").withStyle(ChatFormatting.GREEN), true);
         if (savedStructures.size() <= 50) {
            savedStructures.stream()
               .sorted()
               .forEach(structure -> ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(" - " + structure), true));
         } else {
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(" - Too many structures to print. Check the console."), true);
            StructureGelMod.LOGGER.log("Saved structures:");
            savedStructures.stream().sorted().forEach(x$0 -> rec$.log(x$0));
         }
      } else {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("No structures were saved."), true);
      }

      if (duplicates.size() > 0) {
         ((CommandSourceStack)context.getSource())
            .sendSuccess(
               () -> Component.literal("Warning: Found " + duplicates.size() + " structures with a duplicate name. Click to teleport.")
                  .withStyle(ChatFormatting.RED),
               true
            );
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("[Duplicate Structures]").withStyle(ChatFormatting.RED), true);
         if (duplicates.size() <= 50) {
            duplicates.forEach(
               (pos, structure) -> ((CommandSourceStack)context.getSource())
                  .sendSuccess(
                     () -> Component.literal(String.format("%s at (%d, %d, %d)", structure, pos.getX(), pos.getY(), pos.getZ()))
                        .withStyle(
                           style -> style.withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                        ),
                     true
                  )
            );
         } else {
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(" - Too many duplicates to print. Check the console."), true);
            StructureGelMod.LOGGER.log("Duplicate structures:");
            duplicates.forEach(
               (pos, structure) -> StructureGelMod.LOGGER.log(String.format("%s /tp @s %d %d %d", structure, pos.getX(), pos.getY(), pos.getZ()))
            );
         }
      }

      return savedStructures.size();
   }
}
