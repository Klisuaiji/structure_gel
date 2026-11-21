package com.legacy.structure_gel.core.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.SendStructureInfoToClientPacket;
import com.legacy.structure_gel.core.network.s_to_c.ToggleViewBoundsPacket;
import com.legacy.structure_gel.core.structure.StructureInfo;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class ViewBoundsCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> get()
	{
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("viewbounds").requires(source -> source.hasPermission(2));

		// @formatter:off
		command
			.executes(ViewBoundsCommand::on)
			.then(Commands.literal("on")
					.executes(ViewBoundsCommand::on))
			.then(Commands.literal("off")
					.executes(ViewBoundsCommand::off))
			.then(Commands.literal("refresh")
					.executes(ViewBoundsCommand::on));
		// @formatter:on

		return command;
	}

	private static int on(CommandContext<CommandSourceStack> context)
	{
		return toggle(context.getSource(), true);
	}

	private static int off(CommandContext<CommandSourceStack> context)
	{
		return toggle(context.getSource(), false);
	}

	private static int toggle(CommandSourceStack source, boolean state)
	{
		Entity entity = source.getEntity();
		if (entity instanceof ServerPlayer)
		{
			SGPacketHandler.sendToClient(new ToggleViewBoundsPacket(state), (ServerPlayer) entity);
			if (state)
				refresh(source);
			return 1;
		}
		return 0;
	}

	private static int refresh(CommandSourceStack source)
	{
		Entity entity = source.getEntity();
		if (entity instanceof ServerPlayer)
		{
			ServerPlayer player = (ServerPlayer) entity;
			ServerLevel level = source.getLevel();
			ResourceLocation dimensionName = level.dimension().location();
			StructureManager manager = level.structureManager();
			String playerName = player.getName().getString();
			StructureGelMod.LOGGER.log("Gathering structure bounding boxes for {} in {}", playerName, dimensionName);

			// Gather structures
			Map<BoundingBox, StructureInfo> structureInfos = new HashMap<>();
			final int dist = 10;
			for (int x = -dist; x < dist; x++)
			{
				for (int y = -dist; y < dist; y++)
				{
					for (int z = -dist; z < dist; z++)
					{
						var startsByStructure = manager.getAllStructuresAt(BlockPos.containing(source.getPosition()).offset(x * 16, y * 16, z * 16));
						for (var entry : startsByStructure.entrySet())
						{
							List<StructureStart> starts = new ArrayList<>();
							manager.fillStartsForStructure(entry.getKey(), entry.getValue(), starts::add);

							for (int i = starts.size() - 1; i > -1; i--)
							{
								StructureStart start = starts.get(i);
								BoundingBox fullBounds = start.getBoundingBox();
								if (!structureInfos.containsKey(fullBounds))
								{
									structureInfos.put(fullBounds, new StructureInfo(dimensionName, start));
								}
							}
						}
					}
				}
			}

			// Send to client
			StructureGelMod.LOGGER.log("Sending {} structure infos to {}", structureInfos.size(), playerName);
			structureInfos.values().forEach(info -> SGPacketHandler.sendToClient(new SendStructureInfoToClientPacket(info), player));

			return 1;
		}
		return 0;
	}
}
