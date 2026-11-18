package com.legacy.structure_gel.core.item;

import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.util.PaletteTooltip;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class BlockPaletteItem extends Item {
   public BlockPaletteItem(Properties prop) {
      super(prop);
   }

   public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
      return FMLEnvironment.dist == Dist.CLIENT
         ? getPalleteTooltip(stack, Minecraft.getInstance().level)
         : Optional.ofNullable(ServerLifecycleHooks.getCurrentServer())
            .<ServerLevel>map(MinecraftServer::overworld)
            .map(l -> getPalleteTooltip(stack, l).orElse(null));
   }

   public static Optional<TooltipComponent> getPalleteTooltip(ItemStack stack, LevelReader level) {
      WeightedRandomList<Wrapper<BlockState>> palette = getPalette(stack, level);
      return palette.isEmpty() ? Optional.empty() : Optional.of(new PaletteTooltip(palette));
   }

   public static WeightedRandomList<Wrapper<BlockState>> getPalette(ItemStack stack, LevelReader level) {
      return BuildingToolItem.getPalette(stack, level);
   }

   public static void setPalette(ItemStack stack, WeightedRandomList<Wrapper<BlockState>> palette) {
      BuildingToolItem.setPalette(stack, palette);
   }
}
