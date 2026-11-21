package com.legacy.structure_gel.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;

public class GetBiomesCommand {
   public static LiteralArgumentBuilder<CommandSourceStack> get() {
      LiteralArgumentBuilder<CommandSourceStack> command = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("getbiomes")
         .requires(source -> source.hasPermission(2));
      command.then(
         Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
            .suggests((context, builder) -> StructureGelCommand.suggestFromRegistry(context, builder, Registries.STRUCTURE))
            .executes(context -> getBiomes(context, StructureGelCommand.getResourceKeyHolder(context, Registries.STRUCTURE, "structure")))
      );
      return command;
   }

   private static int getBiomes(CommandContext<CommandSourceStack> context, Holder<Structure> structure) {
      List<String> biomes = ((Structure)structure.value())
         .biomes()
         .stream()
         .<Optional>map(Holder::unwrapKey)
         .filter(Optional::isPresent)
         .map(o -> ((ResourceKey)o.get()).location().toString())
         .sorted()
         .toList();
      Optional<ResourceKey<Structure>> op = structure.unwrapKey();
      String name;
      if (op.isPresent()) {
         name = op.get().location().toString();
      } else {
         name = "null";
      }

      CommandSourceStack source = (CommandSourceStack)context.getSource();
      if (biomes.isEmpty()) {
         source.sendSuccess(() -> Component.literal(name + " has no biomes."), true);
      } else {
         source.sendSuccess(() -> Component.literal("[" + name + "]").withStyle(ChatFormatting.GREEN), true);
         biomes.forEach(biomeName -> source.sendSuccess(() -> Component.literal(" - " + biomeName), true));
      }

      return biomes.size();
   }
}
