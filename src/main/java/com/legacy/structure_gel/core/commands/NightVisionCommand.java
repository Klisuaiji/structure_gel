package com.legacy.structure_gel.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class NightVisionCommand {
   public static LiteralArgumentBuilder<CommandSourceStack> get() {
      LiteralArgumentBuilder<CommandSourceStack> command = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("nv")
         .requires(source -> source.hasPermission(2));
      ((LiteralArgumentBuilder)command.executes(
            c -> toggleNightVision(c, List.of(((CommandSourceStack)c.getSource()).getPlayer()), sp -> !sp.hasEffect(MobEffects.NIGHT_VISION))
         ))
         .then(
            ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("players", EntityArgument.players())
                     .executes(c -> toggleNightVision(c, EntityArgument.getPlayers(c, "players"), sp -> !sp.hasEffect(MobEffects.NIGHT_VISION))))
                  .then(Commands.literal("on").executes(c -> toggleNightVision(c, EntityArgument.getPlayers(c, "players"), sp -> true))))
               .then(Commands.literal("off").executes(c -> toggleNightVision(c, EntityArgument.getPlayers(c, "players"), sp -> false)))
         );
      return command;
   }

   private static int toggleNightVision(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, Predicate<ServerPlayer> shouldGiveEffect) {
      int count = 0;

      for (ServerPlayer player : players) {
         if (player != null) {
            if (shouldGiveEffect.test(player)) {
               if (player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false))) {
                  count++;
               }
            } else if (player.removeEffect(MobEffects.NIGHT_VISION)) {
               count++;
            }
         }
      }

      return count;
   }
}
