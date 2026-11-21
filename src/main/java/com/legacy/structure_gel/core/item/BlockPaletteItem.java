package com.legacy.structure_gel.core.item;

import java.util.Optional;

import com.legacy.structure_gel.core.data_components.BlockPalette;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolItem;
import com.legacy.structure_gel.core.item.util.PaletteTooltip;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class BlockPaletteItem extends Item
{
	public BlockPaletteItem(Properties prop)
	{
		super(prop);
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack)
	{
		// Level isn't passed in. So we do a bit of trickery
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			return getPalleteTooltip(stack, net.minecraft.client.Minecraft.getInstance().level);
		}
		else
		{
			return Optional.ofNullable(ServerLifecycleHooks.getCurrentServer()).map(MinecraftServer::overworld).map(l -> getPalleteTooltip(stack, l).orElse(null));
		}
	}

	public static Optional<TooltipComponent> getPalleteTooltip(ItemStack stack, LevelReader level)
	{
		var palette = getPalette(stack, level);
		return palette.isEmpty() ? Optional.empty() : Optional.of(new PaletteTooltip(palette));
	}

	public static BlockPalette getPalette(ItemStack stack, LevelReader level)
	{
		return BuildingToolItem.getPalette(stack, level);
	}

	public static void setPalette(ItemStack stack, BlockPalette palette)
	{
		//palette = randomPalette();
		BuildingToolItem.setPalette(stack, palette);
	}
	
	// This is for debugging purposes only
	/*private static SimpleWeightedRandomList<BlockState> randomPalette()
	{
		var builder = SimpleWeightedRandomList.<BlockState>builder();
		RandomSource rand = RandomSource.create();
		for (int i = 0; i < 200; i++)
		{
			builder.add(BuiltInRegistries.BLOCK.getRandom(rand).get().value().defaultBlockState(), rand.nextIntBetweenInclusive(1, 1000));
		}
		return builder.build();
	}*/
}
