package com.legacy.structure_gel.core.client.screen.building_tool;

import com.google.common.collect.UnmodifiableIterator;
import com.legacy.structure_gel.api.client.gui.LayeredScreen;
import com.legacy.structure_gel.core.client.ClientProxy;
import com.legacy.structure_gel.core.client.ClientUtil;
import com.legacy.structure_gel.core.client.widget.PropertyEditBox;
import com.legacy.structure_gel.core.client.widget.RenderWidget;
import com.legacy.structure_gel.core.client.widget.TexturedEditBox;
import com.legacy.structure_gel.core.client.widget.TooltipImageButton;
import com.legacy.structure_gel.core.item.BlockPaletteItem;
import com.legacy.structure_gel.core.item.building_tool.ToolModeProperty;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.c_to_s.ExportPalettePacket;
import com.legacy.structure_gel.core.network.c_to_s.RequestRegistryKeysPacket;
import com.legacy.structure_gel.core.registry.SGRegistry;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.SimpleWeightedRandomList.Builder;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class EditPaletteScreen extends LayeredScreen {
   private final BuildingToolScreen parent;
   private static final int MAX_VISIBLE_AT_ONCE = 7;
   private List<EditPaletteScreen.PaletteEntryGroup> paletteEntryWidgets = new ArrayList<>();
   private TooltipImageButton scrollUpButton;
   private TooltipImageButton scrollDownButton;
   private int displayIndexStart = 0;
   private int addIndex = 0;
   private int removeCooldown = 0;

   public EditPaletteScreen(BuildingToolScreen parent) {
      super(Component.empty());
      this.parent = parent;
   }

   protected void init() {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      List<Wrapper<BlockState>> paletteList = this.parent.getPalette().isEmpty()
         ? SimpleWeightedRandomList.builder().add(Blocks.AIR.defaultBlockState(), 1).build().unwrap()
         : this.parent.getPalette().unwrap();
      this.paletteEntryWidgets = new ArrayList<>();

      for (int i = 0; i < paletteList.size(); i++) {
         Wrapper<BlockState> wrapper = paletteList.get(i);
         this.addPaletteEntry((BlockState)wrapper.getData(), wrapper.getWeight().asInt(), false);
      }

      TooltipImageButton addButton = this.addRenderableWidget(
         new TooltipImageButton(centerX + 185, centerY - 11, 16, 16, 0, 112, BuildingToolScreen.WIDGETS, b -> this.addPaletteEntry(null, 1, true))
      );
      addButton.setTooltip(Tooltip.create(SGText.ADD_LABEL));
      this.scrollUpButton = this.addRenderableWidget(
         new TooltipImageButton(centerX + 185, centerY - 11 - 20, 16, 16, 0, 208, BuildingToolScreen.WIDGETS, b -> this.scrollUp())
      );
      this.scrollDownButton = this.addRenderableWidget(
         new TooltipImageButton(centerX + 185, centerY - 11 + 20, 16, 16, 16, 208, BuildingToolScreen.WIDGETS, b -> this.scrollDown())
      );
      TooltipImageButton exportButton = this.addRenderableWidget(
         new TooltipImageButton(this.scrollUpButton.getX(), this.scrollUpButton.getY() - 30, 16, 16, 240, 96, BuildingToolScreen.WIDGETS, b -> {
            ItemStack paletteItem = SGRegistry.Items.BLOCK_PALETTE.getDefaultInstance();
            BlockPaletteItem.setPalette(paletteItem, this.getPalette());
            SGPacketHandler.sendToServer(new ExportPalettePacket(paletteItem));
         })
      );
      exportButton.setTooltip(Tooltip.create(SGText.EXPORT_LABEL));
      this.repositionPaletteEntries();
   }

   private void addPaletteEntry(BlockState state, int weight, boolean reposition) {
      int texSize = 256;
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int stateEditWidth = 206;
      int index = this.addIndex++;
      TooltipImageButton removeButton = this.addWidget(
         new TooltipImageButton(centerX - 190, centerY, 16, 16, 16, 112, BuildingToolScreen.WIDGETS, b -> this.removePaletteEntry(index))
      );
      removeButton.setTooltip(Tooltip.create(SGText.REMOVE_LABEL));
      TexturedEditBox stateEdit = this.addWidget(
         new TexturedEditBox(
            this.minecraft.font,
            centerX - 30 - stateEditWidth / 2,
            centerY,
            stateEditWidth,
            24,
            50,
            48,
            texSize,
            texSize,
            BuildingToolScreen.WIDGETS,
            BuildingToolScreen.BLOCK_PALETTE_TEXT
         )
      );
      stateEdit.setSuggestions(this::getBlockSuggestions);
      stateEdit.setMaxLength(300);
      stateEdit.setResponder(s -> {
         int color = 14737632;

         try {
            BlockResult result = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), s, false);
            if (!result.blockState().getBlock().isEnabled(this.minecraft.level.enabledFeatures())) {
               color = 14703708;
            }
         } catch (CommandSyntaxException e) {
            color = 14703708;
         }

         stateEdit.setTextColor(color, color);
      });
      if (state != null) {
         stateEdit.setValue(serializeState(state.is(Blocks.VOID_AIR) ? Blocks.AIR.defaultBlockState() : state));
      }

      weight = state == null ? 1 : weight;
      PropertyEditBox<Integer> weightEdit = this.addWidget(new PropertyEditBox<>(this.minecraft.font, centerX + 79, centerY, ToolModeProperty.WEIGHT, weight));
      RenderWidget display = (RenderWidget)this.addRenderableOnly(
         RenderWidget.builder(this.font, centerX - 163, stateEdit.getY() + 4, 24, 24).setCustomRender((graphics, mouseX, mouseY, partialTick) -> {
            Optional<ItemStack> stack = this.parent.getItemForBlock(stateEdit.getValue());
            if (stack.isPresent()) {
               graphics.renderItem(stack.get(), centerX - 163 + 4, stateEdit.getY() + 4);
            }
         }).build()
      );
      EditPaletteScreen.PaletteEntryGroup paletteGroup = new EditPaletteScreen.PaletteEntryGroup(index, display, stateEdit, weightEdit, removeButton);
      this.paletteEntryWidgets.add(paletteGroup);
      if (reposition) {
         if (this.paletteEntryWidgets.size() > 7) {
            this.scrollDown();
         } else {
            this.repositionPaletteEntries();
         }
      }
   }

   public static String serializeState(BlockState state) {
      StringBuilder strBuilder = new StringBuilder(state.getBlockHolder().unwrapKey().map(key -> key.location().toString()).orElse("air"));
      if (!state.getProperties().isEmpty()) {
         BlockState defaultState = state.getBlock().defaultBlockState();
         List<String> props = new ArrayList<>();
         UnmodifiableIterator addComma = state.getValues().entrySet().iterator();

         while (addComma.hasNext()) {
            Entry<Property<?>, Comparable<?>> entry = (Entry<Property<?>, Comparable<?>>)addComma.next();
            if (!defaultState.getValue(entry.getKey()).equals(entry.getValue())) {
               props.add(getPropertyString(entry.getKey(), "=", entry.getValue()));
            }
         }

         if (!props.isEmpty()) {
            strBuilder.append('[');
            boolean addCommax = false;

            for (String s : props) {
               if (addCommax) {
                  strBuilder.append(',');
               }

               strBuilder.append(s);
               addCommax = true;
            }

            strBuilder.append(']');
         }
      }

      return strBuilder.toString();
   }

   public static <T extends Comparable<T>> String getPropertyString(Property<T> prop, String spacer, Comparable<?> val) {
      return prop.getName() + spacer + prop.getName(val);
   }

   private Collection<String> getBlockSuggestions() {
      List<ResourceKey<?>> keys = ClientUtil.REGISTRY_KEYS.get(Registries.BLOCK);
      if (keys == null) {
         SGPacketHandler.sendToServer(new RequestRegistryKeysPacket(Registries.BLOCK));
         return Collections.emptyList();
      } else {
         return keys.stream().map(r -> r.location().toString()).toList();
      }
   }

   private void removePaletteEntry(int id) {
      if (this.removeCooldown <= 0) {
         int positionInList = 0;

         for (int i = 0; i < this.paletteEntryWidgets.size(); i++) {
            if (this.paletteEntryWidgets.get(i).id == id) {
               positionInList = i;
               break;
            }
         }

         EditPaletteScreen.PaletteEntryGroup entry = this.paletteEntryWidgets.remove(positionInList);
         this.removeWidget(entry.display);
         this.removeWidget(entry.stateEdit);
         this.removeWidget(entry.weightEdit);
         this.removeWidget(entry.removeButton);
         if (this.displayIndexStart > 0) {
            this.scrollUp();
         } else {
            this.repositionPaletteEntries();
         }

         this.removeCooldown = 2;
      }
   }

   private void repositionPaletteEntries() {
      int centerY = this.height / 2;
      int offset = 30;
      int totalVisible = Math.min(this.paletteEntryWidgets.size(), 7);
      float baseY = centerY - totalVisible / 2.0F * offset;

      for (EditPaletteScreen.PaletteEntryGroup entry : this.paletteEntryWidgets) {
         entry.setVisible(false);
      }

      for (int i = this.displayIndexStart; i < this.displayIndexStart + 7 && i < this.paletteEntryWidgets.size(); i++) {
         int pos = i - this.displayIndexStart;
         EditPaletteScreen.PaletteEntryGroup entry = this.paletteEntryWidgets.get(i);
         entry.setVisible(true);
         entry.move((int)(offset * pos + baseY));
      }

      if (this.paletteEntryWidgets.size() > 7) {
         this.scrollUpButton.active = this.displayIndexStart > 0;
         this.scrollDownButton.active = this.displayIndexStart < this.paletteEntryWidgets.size() - 7;
      } else {
         this.scrollDownButton.active = false;
         this.scrollUpButton.active = false;
      }
   }

   private void scrollUp() {
      if (this.displayIndexStart > 0) {
         this.displayIndexStart--;
      }

      this.repositionPaletteEntries();
   }

   private void scrollDown() {
      if (this.displayIndexStart < this.paletteEntryWidgets.size() - 7) {
         this.displayIndexStart++;
      }

      this.repositionPaletteEntries();
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);

      for (int i = this.paletteEntryWidgets.size() - 1; i > -1; i--) {
         EditPaletteScreen.PaletteEntryGroup entry = this.paletteEntryWidgets.get(i);
         if (entry.isVisible()) {
            entry.render(graphics, mouseX, mouseY, partialTick);
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
         return true;
      }

      if (deltaY > 0.0) {
         this.scrollUp();
      }

      if (deltaY < 0.0) {
         this.scrollDown();
      }

      return true;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
      boolean passed = super.mouseClicked(mouseX, mouseY, clickType);
      if (!passed) {
         this.clearFocus();
      }

      return passed;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      Key key = Type.KEYSYM.getOrCreate(keyCode);
      int escape = 256;
      int tab = 258;
      int backspace = 259;
      boolean pressedEnter = keyCode == 257 || keyCode == 335;

      for (EditPaletteScreen.PaletteEntryGroup entry : this.paletteEntryWidgets) {
         if (entry.stateEdit.isFocused()) {
            if (escape == keyCode) {
               entry.stateEdit.setFocused(false);
            }

            if (pressedEnter) {
               this.back();
               return true;
            }

            if (tab == keyCode) {
               super.keyPressed(keyCode, scanCode, modifiers);
               return true;
            }

            return entry.stateEdit.keyPressed(keyCode, scanCode, modifiers);
         }

         if (entry.weightEdit.isFocused()) {
            if (escape == keyCode) {
               entry.weightEdit.setFocused(false);
            }

            if (pressedEnter) {
               this.back();
               return true;
            }

            if (tab == keyCode) {
               super.keyPressed(keyCode, scanCode, modifiers);
               return true;
            }

            return entry.weightEdit.keyPressed(keyCode, scanCode, modifiers);
         }
      }

      if (escape != keyCode
         && !this.minecraft.options.keyInventory.isActiveAndMatches(key)
         && !((KeyMapping)ClientProxy.BUILDING_TOOL_KEY.get()).isActiveAndMatches(key)
         && !pressedEnter
         && backspace != keyCode) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }

      this.back();
      return true;
   }

   private void back() {
      this.parent.setPalette(this.getPalette());
      BuildingToolScreen.playClickSound();
      this.minecraft.setScreen(this.parent);
   }

   private SimpleWeightedRandomList<BlockState> getPalette() {
      Builder<BlockState> statesBuilder = SimpleWeightedRandomList.builder();

      for (EditPaletteScreen.PaletteEntryGroup entry : this.paletteEntryWidgets) {
         Optional<Pair<BlockState, Integer>> opData = entry.parse();
         if (opData.isPresent()) {
            Pair<BlockState, Integer> data = opData.get();
            if (((BlockState)data.getFirst()).getBlock().isEnabled(this.minecraft.level.enabledFeatures())) {
               statesBuilder.add((BlockState)data.getFirst(), (Integer)data.getSecond());
            }
         }
      }

      return statesBuilder.build();
   }

   public void tick() {
      this.parent.tick();
      if (this.removeCooldown > 0) {
         this.removeCooldown--;
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void changeFocus(ComponentPath path) {
      super.changeFocus(path);
   }

   private class PaletteEntryGroup {
      final int id;
      final RenderWidget display;
      final TexturedEditBox stateEdit;
      final PropertyEditBox<Integer> weightEdit;
      final TooltipImageButton removeButton;
      private boolean visible = true;

      private PaletteEntryGroup(int id, RenderWidget display, TexturedEditBox stateEdit, PropertyEditBox<Integer> weightEdit, TooltipImageButton removeButton) {
         this.id = id;
         this.display = display;
         this.stateEdit = stateEdit;
         this.weightEdit = weightEdit;
         this.removeButton = removeButton;
      }

      public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         this.stateEdit.render(graphics, mouseX, mouseY, partialTick);
         this.weightEdit.render(graphics, mouseX, mouseY, partialTick);
         this.removeButton.render(graphics, mouseX, mouseY, partialTick);
         EditPaletteScreen screen = EditPaletteScreen.this;
         int centerX = screen.width / 2;
         int widgetY = this.stateEdit.getY();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableBlend();
         graphics.blit(BuildingToolScreen.WIDGETS, centerX - 163, widgetY, 0, 160, 24, 24);
         Optional<Pair<BlockState, Integer>> parsed = this.parse();
         if (parsed.isPresent()) {
            BlockState state = (BlockState)parsed.get().getFirst();
            BlockState defaultState = state.getBlock().defaultBlockState();
            StringBuilder tooltip = new StringBuilder();
            tooltip.append(state.getBlock().getName().getString());
            UnmodifiableIterator var12 = state.getValues().entrySet().iterator();

            while (var12.hasNext()) {
               Entry<Property<?>, Comparable<?>> e = (Entry<Property<?>, Comparable<?>>)var12.next();
               Property<? extends Comparable<?>> key = (Property<? extends Comparable<?>>)e.getKey();
               Comparable<?> val = e.getValue();
               if (!defaultState.getValue(key).equals(val)) {
                  tooltip.append("\n" + SGText.BULLET_POINT.getString() + EditPaletteScreen.getPropertyString(key, " = ", val));
               }
            }

            this.display.setTooltip(Tooltip.create(Component.literal(tooltip.toString()).withStyle(ChatFormatting.GRAY)));
         } else {
            this.display.setTooltip(null);
         }

         this.display.render(graphics, mouseX, mouseY, partialTick);
      }

      public void setVisible(boolean visible) {
         this.display.setVisible(visible);
         this.stateEdit.visible = visible;
         this.weightEdit.visible = visible;
         this.removeButton.visible = visible;
         this.visible = visible;
      }

      public boolean isVisible() {
         return this.visible;
      }

      public void move(int newY) {
         this.display.setY(newY);
         this.stateEdit.setY(newY);
         this.weightEdit.setY(newY);
         this.removeButton.setY(newY + 4);
      }

      public Optional<Pair<BlockState, Integer>> parse() {
         try {
            BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), this.stateEdit.getValue(), false).blockState();
            int weight = ToolModeProperty.WEIGHT.read(this.weightEdit.getValue());
            return Optional.of(Pair.of(state, weight));
         } catch (Exception var3) {
            return Optional.empty();
         }
      }
   }
}
