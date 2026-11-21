package com.legacy.structure_gel.core.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class DebugCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> get()
	{
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("debug").requires(source -> source.hasPermission(2));

		// @formatter:off
		var updateStructuresToStructure = Commands.literal("update_structures_to_structure");

		updateStructuresToStructure
		.then(Commands.argument("from", BlockPosArgument.blockPos())
				.then(Commands.argument("to", BlockPosArgument.blockPos())
						.executes(context -> updateStructuresToStructure(context, BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to"))))));

		updateStructuresToStructure
		.then(Commands.literal("search")
				.executes(context -> DebugCommand.updateStructuresToStructure(context, 32))
				.then(Commands.argument("radius", IntegerArgumentType.integer(0, 128))
						.executes(context -> DebugCommand.updateStructuresToStructure(context, IntegerArgumentType.getInteger(context, "radius")))));

		StructureGelCommand.addChild(command, updateStructuresToStructure, Component.empty().append(Component.literal("Updates save mode structure blocks by replacing ")).append(Component.literal(":structures/").withStyle(ChatFormatting.GREEN)).append(Component.literal(" to ")).append(Component.literal(":structure/").withStyle(ChatFormatting.GREEN)).append(Component.literal(" for Minecraft 1.21+ file location changes")));
		// @formatter:on

		command.then(updateStructuresToStructure);

		return command;
	}

	private static int updateStructuresToStructure(CommandContext<CommandSourceStack> context, BoundingBox area)
	{
		ServerLevel level = context.getSource().getLevel();
		return updateStructuresToStructure(context, StructureGelCommand.findStructureBlocks(level, area));
	}

	private static int updateStructuresToStructure(CommandContext<CommandSourceStack> context, int radius)
	{
		ServerLevel level = context.getSource().getLevel();
		BlockPos pos = BlockPos.containing(context.getSource().getPosition());

		return updateStructuresToStructure(context, StructureGelCommand.findStructureBlocks(level, pos, radius));
	}

	private static int updateStructuresToStructure(CommandContext<CommandSourceStack> context, Map<BlockPos, StructureBlockEntity> structureBlocks)
	{
		ServerLevel level = context.getSource().getLevel();
		List<UpdatedStructureBlock> updated = new ArrayList<>();
		Map<BlockPos, String> errored = new LinkedHashMap<>();
		for (var entry : structureBlocks.entrySet())
		{
			StructureBlockEntity structureBlock = entry.getValue();
			if (structureBlock.hasStructureName() && structureBlock.saveStructure())
			{
				String oldName = structureBlock.getStructureName();
				if (oldName.contains(":structures/"))
				{
					String newName = oldName.replaceFirst(":structures/", ":structure/");
					ResourceLocation structureName = ResourceLocation.tryParse(newName);
					if (structureName != null)
					{
						structureBlock.setStructureName(structureName);
						structureBlock.setChanged();
						BlockState state = structureBlock.getBlockState();
						level.sendBlockUpdated(entry.getKey(), state, state, Block.UPDATE_ALL);
						updated.add(new UpdatedStructureBlock(oldName, newName));
					}
					else
					{
						errored.put(entry.getKey(), oldName);
					}
				}
			}
		}
		if (updated.size() > 0)
		{
			context.getSource().sendSuccess(() -> Component.literal("[Updated " + updated.size() + " Structure Blocks]").withStyle(ChatFormatting.GREEN), true);
			updated.stream().sorted().forEach(structure -> context.getSource().sendSuccess(() -> Component.literal(" - " + structure), true));
		}
		else
		{
			context.getSource().sendSuccess(() -> Component.literal("No structure blocks were updated."), true);
		}

		if (errored.size() > 0)
		{
			context.getSource().sendSuccess(() -> Component.literal("Warning: Found " + errored.size() + " structure blocks that could not be updated. Click to teleport.").withStyle(ChatFormatting.RED), true);
			context.getSource().sendSuccess(() -> Component.literal("[Errored Structure Blocks]").withStyle(ChatFormatting.RED), true);
			errored.forEach((pos, name) -> context.getSource().sendSuccess(() -> Component.literal(String.format("%s at (%d, %d, %d)", name, pos.getX(), pos.getY(), pos.getZ())).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))), true));
		}
		return updated.size();
	}

	private static record UpdatedStructureBlock(String oldName, String newName) implements Comparable<UpdatedStructureBlock>
	{
		@Override
		public final String toString()
		{
			return oldName + " -> " + newName;
		}

		@Override
		public int compareTo(UpdatedStructureBlock o)
		{
			return this.toString().compareTo(o.toString());
		}
	}
}
