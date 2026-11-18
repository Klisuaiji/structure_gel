package com.legacy.structure_gel.core.commands;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.SendStructureInfoToClientPacket;
import com.legacy.structure_gel.core.network.s_to_c.ToggleViewBoundsPacket;
import com.legacy.structure_gel.core.structure.StructureInfo;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class ViewBoundsCommand {
   public static LiteralArgumentBuilder<CommandSourceStack> get() {
      LiteralArgumentBuilder<CommandSourceStack> command = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("viewbounds")
         .requires(source -> source.hasPermission(2));
      ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)command.executes(ViewBoundsCommand::on))
               .then(Commands.literal("on").executes(ViewBoundsCommand::on)))
            .then(Commands.literal("off").executes(ViewBoundsCommand::off)))
         .then(Commands.literal("refresh").executes(ViewBoundsCommand::on));
      return command;
   }

   private static int on(CommandContext<CommandSourceStack> context) {
      return toggle((CommandSourceStack)context.getSource(), true);
   }

   private static int off(CommandContext<CommandSourceStack> context) {
      return toggle((CommandSourceStack)context.getSource(), false);
   }

   private static int toggle(CommandSourceStack source, boolean state) {
      Entity entity = source.getEntity();
      if (entity instanceof ServerPlayer) {
         SGPacketHandler.sendToClient(new ToggleViewBoundsPacket(state), (ServerPlayer)entity);
         if (state) {
            refresh(source);
         }

         return 1;
      } else {
         return 0;
      }
   }

   private static int refresh(CommandSourceStack source) {
      if (!(source.getEntity() instanceof ServerPlayer player)) {
         return 0;
      } else {
         ServerLevel level = source.getLevel();
         ResourceLocation dimensionName = level.dimension().location();
         StructureManager manager = level.structureManager();
         String playerName = player.getName().getString();
         StructureGelMod.LOGGER.log("Gathering structure bounding boxes for {} in {}", playerName, dimensionName);
         Map<BoundingBox, StructureInfo> structureInfos = new HashMap<>();
         int dist = 10;

         for (int x = -10; x < 10; x++) {
            for (int y = -10; y < 10; y++) {
               for (int z = -10; z < 10; z++) {
                  Map<Structure, LongSet> startsByStructure = manager.getAllStructuresAt(
                     BlockPos.containing(source.getPosition()).offset(x * 16, y * 16, z * 16)
                  );

                  for (Entry<Structure, LongSet> entry : startsByStructure.entrySet()) {
                     List<StructureStart> starts = new ArrayList<>();
                     manager.fillStartsForStructure(entry.getKey(), entry.getValue(), starts::add);

                     for (int i = starts.size() - 1; i > -1; i--) {
                        StructureStart start = starts.get(i);
                        BoundingBox fullBounds = start.getBoundingBox();
                        if (!structureInfos.containsKey(fullBounds)) {
                           structureInfos.put(fullBounds, new StructureInfo(dimensionName, start));
                        }
                     }
                  }
               }
            }
         }

         StructureGelMod.LOGGER.log("Sending {} structure infos to {}", structureInfos.size(), playerName);
         structureInfos.values().forEach(info -> SGPacketHandler.sendToClient(new SendStructureInfoToClientPacket(info), player));
         return 1;
      }
   }
}
