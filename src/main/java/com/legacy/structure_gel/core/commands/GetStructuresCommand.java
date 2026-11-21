package com.legacy.structure_gel.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.legacy.structure_gel.api.structure.StructureAccessHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;

public class GetStructuresCommand
{
	@SuppressWarnings("unchecked")
	public static LiteralArgumentBuilder<CommandSourceStack> get()
	{
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("getstructures").requires(source -> source.hasPermission(2));

		//@formatter:off
		
		command
			.then(Commands.literal("here")
				.executes(c -> here(c, BlockPos.containing(c.getSource().getPosition())))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(c -> here(c, BlockPosArgument.getLoadedBlockPos(c, "pos")))));
		command
			.then(Commands.literal("biome")
				.executes(c -> biome(c))
				.then(Commands.argument("biome", ResourceKeyArgument.key(Registries.BIOME))
						.executes(c -> biome(c, c.getArgument("biome", ResourceKey.class)))));
		
		//@formatter:on
		return command;
	}

	private static int here(CommandContext<CommandSourceStack> context, BlockPos pos)
	{
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		var structureReg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		List<Pair<String, String>> structures = level.structureManager().startsForStructure(new ChunkPos(pos), s -> true).stream().map(s ->
		{
			Structure struc = s.getStructure();
			boolean inBounds = StructureAccessHelper.isInStructure(level, struc, pos);
			boolean inPiece = StructureAccessHelper.isInStructurePiece(level, struc, pos);
			
			String name = structureReg.getResourceKey(struc).map(r -> r.location().toString()).orElse("null");
			
			return Pair.of(name, inPiece ? "inside" : inBounds ? "in bounds" : "not inside");
		}).sorted((p1, p2) -> p1.getFirst().compareTo(p2.getFirst())).toList();
		int size = structures.size();
		if (size > 0)
		{
			source.sendSuccess(() -> Component.literal(String.format("Structures at (%d, %d, %d):", pos.getX(), pos.getY(), pos.getZ())).withStyle(ChatFormatting.GREEN), true);
			structures.forEach(p -> source.sendSuccess(() -> Component.literal(String.format("- %s (%s)", p.getFirst(), p.getSecond())), true));
			return size;
		}
		source.sendSuccess(() -> Component.literal("There are no structures."), true);
		return 0;
	}

	private static int biome(CommandContext<CommandSourceStack> context)
	{
		CommandSourceStack source = context.getSource();
		Optional<ResourceKey<Biome>> opName = source.getLevel().getBiome(BlockPos.containing(source.getPosition())).unwrapKey();
		if (opName.isPresent())
		{
			return biome(context, opName.get());
		}
		source.sendFailure(Component.literal("The biome you're in doesn't have a registry name."));
		return 0;
	}

	private static int biome(CommandContext<CommandSourceStack> context, ResourceKey<Biome> biomeKey)
	{
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		ResourceLocation name = biomeKey.location();
		Optional<Holder.Reference<Biome>> opBiome = level.registryAccess().lookupOrThrow(Registries.BIOME).get(biomeKey);
		if (opBiome.isPresent())
		{
			Holder<Biome> biome = opBiome.get();
			source.sendSuccess(() -> Component.literal("[" + name.toString() + "]").withStyle(ChatFormatting.GREEN), true);
			List<Structure> structures = new ArrayList<>();

			ChunkGeneratorStructureState chunkGen = level.getChunkSource().getGeneratorState();
			chunkGen.possibleStructureSets().forEach(holder ->
			{
				StructureSet structureSet = holder.value();
				for (StructureSelectionEntry entry : structureSet.structures())
				{
					var structure = entry.structure().value();
					if (structure.biomes().contains(biome))
						structures.add(structure);
				}
			});

			if (structures.isEmpty())
				source.sendSuccess(() -> Component.literal(name.toString() + " has no structures."), true);
			else
			{
				Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
				structures.stream().map(registry::getKey).map(r -> r == null ? "null" : r.toString()).sorted().forEach(r -> source.sendSuccess(() -> Component.literal(" - " + r), true));
			}
			return structures.size();
		}
		source.sendFailure(Component.literal("The biome you're in is unregistered."));
		return 0;
	}
}
