package com.legacy.structure_gel.core.mixin.client;

import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(Item.class)
public class ItemMixin
{
	@Inject(at = @At("HEAD"), method = "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;", cancellable = true)
	private void getDisplayName(ItemStack stack, CallbackInfoReturnable<Component> callback)
	{
		if (SGConfig.CLIENT.showStructureBlockInfo())
		{
			Item item = stack.getItem();
			try
			{
				if (item == Items.STRUCTURE_BLOCK || item == Items.JIGSAW || item == SGRegistry.Blocks.DATA_HANDLER.map(Block::asItem))
				{
					// Don't override anvil names
					if (stack.has(DataComponents.CUSTOM_NAME))
						return;

					CustomData beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
					if (beData != null)
					{
						CompoundTag blockEntityTag = beData.copyTag();
						Component displayName = null;
						if (item == Items.STRUCTURE_BLOCK)
						{
							String mode = blockEntityTag.contains("mode") ? blockEntityTag.getString("mode").toLowerCase(Locale.ENGLISH) : "load";
							String text = mode.equals("data") ? (blockEntityTag.contains("metadata") ? blockEntityTag.getString("metadata") : "") : (blockEntityTag.contains("name") ? blockEntityTag.getString("name") : "");
							displayName = Component.translatable("structure_block.mode." + mode).append(Component.literal(text.isBlank() ? " " + Component.translatable(item.getDescriptionId()).getString() : ": \"" + text + "\""));
						}
						else if (item == Items.JIGSAW)
						{
							String text = blockEntityTag.contains("pool") ? blockEntityTag.getString("pool") : "null";
							displayName = Component.literal("Pool: \"" + text + "\"");
						}
						else if (item == SGRegistry.Blocks.DATA_HANDLER.get().asItem())
						{
							if (blockEntityTag.contains("CustomName", Tag.TAG_STRING))
							{
								if (Minecraft.getInstance().level != null)
									displayName = Component.Serializer.fromJson(blockEntityTag.getString("CustomName"), Minecraft.getInstance().level.registryAccess());
							}
						}
						if (displayName != null)
							callback.setReturnValue(displayName);
					}
				}
			}
			catch (Exception e)
			{
				// Silently catch to prevent console spam. 
				// StructureGelMod.logError("Encountered an error getting an item's name");
				// e.printStackTrace();
			}
		}
	}
}
