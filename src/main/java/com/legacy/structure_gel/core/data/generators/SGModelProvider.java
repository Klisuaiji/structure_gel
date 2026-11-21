package com.legacy.structure_gel.core.data.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.block.DataHandlerBlock;
import com.legacy.structure_gel.core.client.item_model_properties.BuildingToolModeProperty;
import com.legacy.structure_gel.core.client.item_model_properties.StructureModeProperty;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolMode;
import com.legacy.structure_gel.core.item.building_tool.BuildingToolModes;
import com.legacy.structure_gel.core.registry.SGRegistry;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.BlockStateGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.Variant;
import net.minecraft.client.data.models.blockstates.VariantProperties;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.StructureMode;

public class SGModelProvider extends ModelProvider
{

	public SGModelProvider(PackOutput output)
	{
		super(output, StructureGelMod.MODID);
	}

	@Override
	protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels)
	{
		new ItemProv(itemModels.itemModelOutput, itemModels.modelOutput).run();
		new StateProv(blockModels.blockStateOutput, blockModels.itemModelOutput, blockModels.modelOutput).run();
	}

	private static ModelTemplate parent(String name)
	{
		return ModelTemplates.create(name);
	}

	private static class ItemProv extends ItemModelGenerators
	{
		public ItemProv(ItemModelOutput itemOutput, BiConsumer<ResourceLocation, ModelInstance> models)
		{
			super(itemOutput, models);
		}

		@Override
		public void run()
		{
			this.basicItem(SGRegistry.Items.BLOCK_PALETTE.get());
			this.buildingTool(SGRegistry.Items.BUILDING_TOOL.get());
		}

		private void basicItem(Item item)
		{
			this.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}

		private void buildingTool(Item item)
		{
			Map<BuildingToolMode, ItemModel.Unbaked> modelsForMode = new HashMap<>();
			List<SelectItemModel.SwitchCase<BuildingToolMode>> modeSwitch = new ArrayList<>();
			BuildingToolModes.REGISTRY.forEach((key, mode) ->
			{
				ItemModel.Unbaked modeModel = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_" + key.getPath(), ModelTemplates.FLAT_HANDHELD_ITEM));
				modelsForMode.put(mode, modeModel);
				modeSwitch.add(ItemModelUtils.when(mode, modeModel));
			});
			
			this.itemModelOutput.accept(item, ItemModelUtils.select(new BuildingToolModeProperty(), modelsForMode.get(BuildingToolModes.NONE), modeSwitch));
		}
	}

	private static class StateProv extends BlockModelGenerators
	{
		public StateProv(Consumer<BlockStateGenerator> blockOutput, ItemModelOutput itemOutput, BiConsumer<ResourceLocation, ModelInstance> models)
		{
			super(blockOutput, itemOutput, models);
		}

		@Override
		public void run()
		{
			this.createGel(SGRegistry.Blocks.GEL_SPREADER.get());
			this.createGel(SGRegistry.Blocks.RED_GEL.get());
			this.createGel(SGRegistry.Blocks.BLUE_GEL.get());
			this.createGel(SGRegistry.Blocks.GREEN_GEL.get());
			this.createGel(SGRegistry.Blocks.CYAN_GEL.get());
			this.createGel(SGRegistry.Blocks.ORANGE_GEL.get());
			this.createGel(SGRegistry.Blocks.YELLOW_GEL.get());

			this.dataHandler(SGRegistry.Blocks.DATA_HANDLER.get());

			this.dynamicSpawner(SGRegistry.Blocks.DYNAMIC_SPAWNER.get());

			this.structureBlock(Blocks.STRUCTURE_BLOCK);
		}

		public void createGel(Block block)
		{
			this.simpleBlock(block, "translucent");
		}

		public void simpleBlock(Block block, @Nullable String renderType)
		{
			this.createTrivialBlock(block, renderType != null ? TexturedModel.createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL.extend().renderType(ResourceLocation.parse(renderType)).build()) : TexturedModel.CUBE);
		}

		private void dataHandler(Block block)
		{
			ResourceLocation frontTex = TextureMapping.getBlockTexture(block, "_front");
			ResourceLocation backTex = TextureMapping.getBlockTexture(block, "_back");
			ResourceLocation sideTex = TextureMapping.getBlockTexture(block, "_side");
			TextureMapping texturemapping = new TextureMapping().put(TextureSlot.DOWN, sideTex).put(TextureSlot.WEST, sideTex).put(TextureSlot.EAST, sideTex).put(TextureSlot.PARTICLE, frontTex).put(TextureSlot.NORTH, frontTex).put(TextureSlot.SOUTH, backTex).put(TextureSlot.UP, sideTex);
			ResourceLocation model = ModelTemplates.CUBE_DIRECTIONAL.create(block, texturemapping, this.modelOutput);

			ResourceLocation frontClearTex = frontTex.withSuffix("_clear");
			ResourceLocation backClearTex = backTex.withSuffix("_clear");
			ResourceLocation sideClearTex = sideTex.withSuffix("_clear");
			TextureMapping clearTexturemapping = new TextureMapping().put(TextureSlot.DOWN, sideClearTex).put(TextureSlot.WEST, sideClearTex).put(TextureSlot.EAST, sideClearTex).put(TextureSlot.PARTICLE, frontClearTex).put(TextureSlot.NORTH, frontClearTex).put(TextureSlot.SOUTH, backClearTex).put(TextureSlot.UP, sideClearTex);
			ResourceLocation clearModel = ModelTemplates.CUBE_DIRECTIONAL.extend().renderType("translucent").build().createWithSuffix(block, "_clear", clearTexturemapping, this.modelOutput);

			this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block).with(PropertyDispatch.properties(DataHandlerBlock.FACING, DataHandlerBlock.WATERLOGGED).generate((dir, waterlogged) ->
			{
				ResourceLocation m = waterlogged ? clearModel : model;
				switch (dir)
				{
				case DOWN:
					return Variant.variant().with(VariantProperties.MODEL, m).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90);
				case EAST:
					return Variant.variant().with(VariantProperties.MODEL, m).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
				case NORTH:
					return Variant.variant().with(VariantProperties.MODEL, m);
				case SOUTH:
					return Variant.variant().with(VariantProperties.MODEL, m).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
				case UP:
					return Variant.variant().with(VariantProperties.MODEL, m).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270);
				default: // west
					return Variant.variant().with(VariantProperties.MODEL, m).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
				}
			})));
		}

		private void dynamicSpawner(Block block)
		{
			ResourceLocation modelName = ModelTemplates.create("spawner").extend().renderType("cutout").build().create(block, new TextureMapping(), modelOutput);
			MultiVariantGenerator gen = MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, modelName));
			this.blockStateOutput.accept(gen);
		}

		private void structureBlock(Block block)
		{
			Map<StructureMode, ItemModel.Unbaked> modelsForMode = new HashMap<>();
			List<SelectItemModel.SwitchCase<StructureMode>> modeSwitch = new ArrayList<>();
			for (StructureMode mode : StructureMode.values())
			{
				ItemModel.Unbaked modeModel = new BlockModelWrapper.Unbaked(ModelLocationUtils.getModelLocation(block, "_" + mode.getSerializedName()), List.of());
				modelsForMode.put(mode, modeModel);
				modeSwitch.add(ItemModelUtils.when(mode, modeModel));
			}

			this.itemModelOutput.accept(block.asItem(), ItemModelUtils.select(new StructureModeProperty(), modelsForMode.get(StructureMode.LOAD), modeSwitch));

		}
	}
}
