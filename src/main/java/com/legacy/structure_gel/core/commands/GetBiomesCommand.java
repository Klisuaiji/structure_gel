package com.legacy.structure_gel.core.commands;

import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.Structure;

public class GetBiomesCommand
{
	public static LiteralArgumentBuilder<CommandSourceStack> get()
	{
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("getbiomes").requires(source -> source.hasPermission(2));

		//@formatter:off
		
		command
			.then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
				.suggests((context, builder) -> StructureGelCommand.suggestFromRegistry(context, builder, Registries.STRUCTURE))
				.executes(context -> getBiomes(context, StructureGelCommand.getResourceKeyHolder(context, Registries.STRUCTURE, "structure"))));
		
		//@formatter:on

		return command;
	}

	private static int getBiomes(CommandContext<CommandSourceStack> context, Holder<Structure> structure)
	{
		List<String> biomes = structure.value().biomes().stream().map(Holder::unwrapKey).filter(Optional::isPresent).map(o -> o.get().location().toString()).sorted().toList();
		
		String name;
		var op = structure.unwrapKey();
		if (op.isPresent())
			name = op.get().location().toString();
		else
			name = "null";

		var source = context.getSource();

		if (biomes.isEmpty())
		{
			source.sendSuccess(() -> Component.literal(name + " has no biomes."), true);
		}
		else
		{
			source.sendSuccess(() -> Component.literal("[" + name + "]").withStyle(ChatFormatting.GREEN), true);
			biomes.forEach(biomeName -> source.sendSuccess(() -> Component.literal(" - " + biomeName), true));
		}
		return biomes.size();
	}
}
