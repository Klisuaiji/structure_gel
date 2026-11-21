package com.legacy.structure_gel.core.commands;

import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class GetFeaturesCommand
{
	@SuppressWarnings("unchecked")
	public static LiteralArgumentBuilder<CommandSourceStack> get()
	{
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("getfeatures").requires(source -> source.hasPermission(2));

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
		return biome(context, context.getSource().getLevel().getBiome(pos));
	}

	private static int biome(CommandContext<CommandSourceStack> context)
	{
		CommandSourceStack source = context.getSource();
		return biome(context, source.getLevel().getBiome(BlockPos.containing(source.getPosition())));
	}

	private static int biome(CommandContext<CommandSourceStack> context, ResourceKey<Biome> biomeKey)
	{
		CommandSourceStack source = context.getSource();
		Optional<Holder.Reference<Biome>> opBiome = source.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).get(biomeKey);
		if (opBiome.isPresent())
		{
			return biome(context, opBiome.get());
		}
		source.sendFailure(Component.literal("The biome you're targeting is unregistered."));
		return 0;
	}

	private static int biome(CommandContext<CommandSourceStack> context, Holder<Biome> biome)
	{
		CommandSourceStack source = context.getSource();
		String biomeName = biome.unwrapKey().map(k -> k.location().toString()).orElse("Biome Not Registered");
		source.sendSuccess(() -> Component.literal("[" + biomeName + "]").withStyle(ChatFormatting.GREEN), true);
		
		List<Holder<PlacedFeature>> features = biome.value().getGenerationSettings().features().stream().flatMap(HolderSet::stream).toList();
		if (features.isEmpty())
		{
			source.sendSuccess(() -> Component.literal(biomeName + " has no features."), true);
		}
		else
		{
			features.stream().map(Holder::unwrapKey).map(op -> op.map(k -> k.location().toString()).orElse("Not registered")).forEach(s -> source.sendSuccess(() -> Component.literal(" - " + s), true));
		}
		
		return features.size();
	}
}