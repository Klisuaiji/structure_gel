package com.legacy.structure_gel.core.commands;

import com.legacy.structure_gel.core.capability.level.BuildingToolPlayerData;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BuildingToolCommand {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_LEVEL_PLAYERS = (context, builder) -> SharedSuggestionProvider.suggest(
      ActionHistory.getHistoryOwners(((CommandSourceStack)context.getSource()).getLevel()), builder
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_ALL_PLAYERS = (context, builder) -> SharedSuggestionProvider.suggest(
      ActionHistory.getHistoryOwners(((CommandSourceStack)context.getSource()).getServer()), builder
   );

   public static LiteralArgumentBuilder<CommandSourceStack> get() {
      LiteralArgumentBuilder<CommandSourceStack> command = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("building_tool")
         .requires(source -> source.hasPermission(2));
      command.then(
         ((LiteralArgumentBuilder)Commands.literal("undo")
               .executes(context -> undo(context, ((CommandSourceStack)context.getSource()).getPlayerOrException().getGameProfile().getName(), 1)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("player", StringArgumentType.word())
                     .suggests(SUGGEST_LEVEL_PLAYERS)
                     .executes(context -> undo(context, StringArgumentType.getString(context, "player"), 1)))
                  .then(
                     Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> undo(context, StringArgumentType.getString(context, "player"), IntegerArgumentType.getInteger(context, "amount")))
                  )
            )
      );
      command.then(
         ((LiteralArgumentBuilder)Commands.literal("redo")
               .executes(context -> redo(context, ((CommandSourceStack)context.getSource()).getPlayerOrException().getGameProfile().getName(), 1)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("player", StringArgumentType.word())
                     .suggests(SUGGEST_LEVEL_PLAYERS)
                     .executes(context -> redo(context, StringArgumentType.getString(context, "player"), 1)))
                  .then(
                     Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> redo(context, StringArgumentType.getString(context, "player"), IntegerArgumentType.getInteger(context, "amount")))
                  )
            )
      );
      command.then(
         ((LiteralArgumentBuilder)Commands.literal("clear_history")
               .executes(context -> clearActions(context, ((CommandSourceStack)context.getSource()).getPlayerOrException().getGameProfile().getName())))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("player", StringArgumentType.word())
                     .suggests(SUGGEST_ALL_PLAYERS)
                     .executes(context -> clearActions(context, StringArgumentType.getString(context, "player"))))
                  .then(
                     Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(
                           context -> clearActions(
                              context, StringArgumentType.getString(context, "player"), DimensionArgument.getDimension(context, "dimension")
                           )
                        )
                  )
            )
      );
      command.then(
         ((LiteralArgumentBuilder)Commands.literal("clear_clipboard")
               .executes(context -> clearClipboard(context, ((CommandSourceStack)context.getSource()).getPlayerOrException().getGameProfile().getName())))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("player", StringArgumentType.word())
                     .suggests(SUGGEST_ALL_PLAYERS)
                     .executes(context -> clearClipboard(context, StringArgumentType.getString(context, "player"))))
                  .then(
                     Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(
                           context -> clearClipboard(
                              context, StringArgumentType.getString(context, "player"), DimensionArgument.getDimension(context, "dimension")
                           )
                        )
                  )
            )
      );
      return command;
   }

   private static int undo(CommandContext<CommandSourceStack> context, String playerName, int amount) {
      return undoOrRedo(context, playerName, amount, true);
   }

   private static int redo(CommandContext<CommandSourceStack> context, String playerName, int amount) {
      return undoOrRedo(context, playerName, amount, false);
   }

   private static int undoOrRedo(CommandContext<CommandSourceStack> context, String playerName, int amount, boolean undo) {
      ActionHistory history = ActionHistory.get(playerName, ((CommandSourceStack)context.getSource()).getLevel());
      if (history.isEmpty()) {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal(playerName + " has no action history."));
         return 0;
      }

      String doing = undo ? "undo" : "redo";
      Consumer<ServerPlayer> action = undo ? history::undo : history::redo;
      if (amount > 1) {
         ((CommandSourceStack)context.getSource())
            .sendSuccess(() -> Component.literal("Attempting to " + doing + " " + amount + " actions from " + playerName), true);
      } else {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Attempting to " + doing + " " + playerName + "'s last action"), true);
      }

      try {
         ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();

         for (int i = 0; i < amount; i++) {
            action.accept(player);
         }

         return 1;
      } catch (CommandSyntaxException e) {
         ((CommandSourceStack)context.getSource())
            .sendFailure(Component.literal("This command must be ran by a player since they need to be in the dimension of the action."));
         return 0;
      }
   }

   private static int clearActions(CommandContext<CommandSourceStack> context, String playerName) {
      for (ServerLevel level : ((CommandSourceStack)context.getSource()).getServer().getAllLevels()) {
         ActionHistory history = ActionHistory.get(playerName, level);
         history.clear();
      }

      ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Cleared action history for " + playerName), true);
      return 1;
   }

   private static int clearActions(CommandContext<CommandSourceStack> context, String playerName, ServerLevel level) {
      ActionHistory history = ActionHistory.get(playerName, level);
      history.clear();
      ((CommandSourceStack)context.getSource())
         .sendSuccess(() -> Component.literal("Cleared action history for " + playerName + " in " + level.dimension().location()), true);
      return 1;
   }

   private static int clearClipboard(CommandContext<CommandSourceStack> context, String playerName) {
      for (ServerLevel level : ((CommandSourceStack)context.getSource()).getServer().getAllLevels()) {
         BuildingToolPlayerData data = BuildingToolPlayerData.get(level, playerName);
         data.clearClipboard();
      }

      ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Cleared clipboard for " + playerName), true);
      return 1;
   }

   private static int clearClipboard(CommandContext<CommandSourceStack> context, String playerName, ServerLevel level) {
      BuildingToolPlayerData data = BuildingToolPlayerData.get(level, playerName);
      data.clearClipboard();
      ((CommandSourceStack)context.getSource())
         .sendSuccess(() -> Component.literal("Cleared clipboard for " + playerName + " in " + level.dimension().location()), true);
      return 1;
   }
}
