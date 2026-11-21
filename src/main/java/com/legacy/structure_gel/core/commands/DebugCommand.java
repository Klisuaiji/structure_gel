package com.legacy.structure_gel.core.commands;

import com.legacy.blue_skies.compat.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class DebugCommand {
   public static LiteralArgumentBuilder<CommandSourceStack> get() {
      LiteralArgumentBuilder<CommandSourceStack> command = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("debug")
         .requires(source -> source.hasPermission(2));
      LiteralArgumentBuilder<CommandSourceStack> updateStructuresToStructure = Commands.literal("update_structures_to_structure");
      updateStructuresToStructure.then(
         Commands.argument("from", BlockPosArgument.blockPos())
            .then(
               Commands.argument("to", BlockPosArgument.blockPos())
                  .executes(
                     context -> updateStructuresToStructure(
                        context,
                        BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to"))
                     )
                  )
            )
      );
      updateStructuresToStructure.then(
         ((LiteralArgumentBuilder)Commands.literal("search").executes(context -> updateStructuresToStructure(context, 32)))
            .then(
               Commands.argument("radius", IntegerArgumentType.integer(0, 128))
                  .executes(context -> updateStructuresToStructure(context, IntegerArgumentType.getInteger(context, "radius")))
            )
      );
      StructureGelCommand.addChild(
         command,
         updateStructuresToStructure,
         Component.empty()
            .append(Component.literal("Updates save mode structure blocks by replacing "))
            .append(Component.literal(":structures/").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" to "))
            .append(Component.literal(":structure/").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" for Minecraft 1.21+ file location changes"))
      );
      command.then(updateStructuresToStructure);
      return command;
   }

   private static int updateStructuresToStructure(CommandContext<CommandSourceStack> context, BoundingBox area) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      return updateStructuresToStructure(context, StructureGelCommand.findStructureBlocks(level, area));
   }

   private static int updateStructuresToStructure(CommandContext<CommandSourceStack> context, int radius) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      BlockPos pos = BlockPos.containing(((CommandSourceStack)context.getSource()).getPosition());
      return updateStructuresToStructure(context, StructureGelCommand.findStructureBlocks(level, pos, radius));
   }

   private static int updateStructuresToStructure(CommandContext<CommandSourceStack> context, Map<BlockPos, StructureBlockEntity> structureBlocks) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      List<DebugCommand.UpdatedStructureBlock> updated = new ArrayList<>();
      Map<BlockPos, String> errored = new LinkedHashMap<>();

      for (Entry<BlockPos, StructureBlockEntity> entry : structureBlocks.entrySet()) {
         StructureBlockEntity structureBlock = entry.getValue();
         if (structureBlock.hasStructureName() && structureBlock.saveStructure()) {
            String oldName = structureBlock.getStructureName();
            if (oldName.contains(":structures/")) {
               String newName = oldName.replaceFirst(":structures/", ":structure/");
               if (LegacyRL.isValid(newName)) {
                  structureBlock.setStructureName(ResourceLocation.parse(newName));
                  structureBlock.setChanged();
                  BlockState state = structureBlock.getBlockState();
                  level.sendBlockUpdated(entry.getKey(), state, state, 3);
                  updated.add(new DebugCommand.UpdatedStructureBlock(oldName, newName));
               } else {
                  errored.put(entry.getKey(), oldName);
               }
            }
         }
      }

      if (updated.size() > 0) {
         ((CommandSourceStack)context.getSource())
            .sendSuccess(() -> Component.literal("[Updated " + updated.size() + " Structure Blocks]").withStyle(ChatFormatting.GREEN), true);
         updated.stream()
            .sorted()
            .forEach(structure -> ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(" - " + structure), true));
      } else {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("No structure blocks were updated."), true);
      }

      if (errored.size() > 0) {
         ((CommandSourceStack)context.getSource())
            .sendSuccess(
               () -> Component.literal("Warning: Found " + errored.size() + " structure blocks that could not be updated. Click to teleport.")
                  .withStyle(ChatFormatting.RED),
               true
            );
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("[Errored Structure Blocks]").withStyle(ChatFormatting.RED), true);
         errored.forEach(
            (pos, name) -> ((CommandSourceStack)context.getSource())
               .sendSuccess(
                  () -> Component.literal(String.format("%s at (%d, %d, %d)", name, pos.getX(), pos.getY(), pos.getZ()))
                     .withStyle(
                        style -> style.withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                     ),
                  true
               )
         );
      }

      return updated.size();
   }

   private record UpdatedStructureBlock(String oldName, String newName) implements Comparable<DebugCommand.UpdatedStructureBlock> {
      @Override
      public final String toString() {
         return this.oldName + " -> " + this.newName;
      }

      public int compareTo(DebugCommand.UpdatedStructureBlock o) {
         return this.toString().compareTo(o.toString());
      }
   }
}
