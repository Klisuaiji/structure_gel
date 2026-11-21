package com.legacy.structure_gel.core.client.widget;

import com.legacy.structure_gel.api.data_handler.parsing.DataParser;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.client.screen.DataHandlerScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CycleButton.Builder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class DataParamsList extends ContainerObjectSelectionList<DataParamsList.Entry> {
   private static final Map<Class<?>, DataParamsList.WidgetFactory> WIDGET_FACTORIES = (Map<Class<?>, DataParamsList.WidgetFactory>)Util.make(
      new HashMap(), map -> {
         map.put(Boolean.class, DataParamsList.WidgetFactory::booleanButton);
         map.put(Direction.class, DataParamsList.WidgetFactory::directionButton);
         map.put(BlockState.class, (mc, parser) -> DataParamsList.WidgetFactory.registryEditBox(mc, parser, BuiltInRegistries.BLOCK));
         map.put(ItemStack.class, (mc, parser) -> DataParamsList.WidgetFactory.registryEditBox(mc, parser, BuiltInRegistries.ITEM));
         map.put(ResourceKey.class, DataParamsList.WidgetFactory::registryEditBox);
      }
   );
   public final DataHandlerScreen parentScreen;
   @Nullable
   private DataParamsList.Entry hoveredEntry = null;
   private boolean isValid = false;
   public boolean showScrollBar = true;
   public boolean renderTopAndBottom = true;

   public DataParamsList(DataHandlerScreen screen, Minecraft mc, DataParser dataParser) {
      super(mc, screen.width + 55, screen.height - 88, 52, 25);
      this.parentScreen = screen;
      this.setRenderBackground(false);
      this.createEntries(dataParser);
   }

   public int getMaxScroll() {
      return this.showScrollBar ? super.getMaxScroll() : 0;
   }

   public Map<String, String> values() {
      Map<String, String> entries = new HashMap<>();

      for (DataParamsList.Entry entry : this.children()) {
         String value = entry.getValue();
         if (!value.isBlank()) {
            DataParser.Parser<?> parser = entry.parser;
            Object parsedVal = parser.parse(value, this.minecraft.level);
            if (parsedVal != null) {
               entries.put(parser.key, value);
            }
         }
      }

      return entries;
   }

   public List<DataParamsList.Entry> childrenClone() {
      return new ArrayList<>(this.children());
   }

   public void createEntries(DataParser dataParser) {
      this.clearEntries();
      if (dataParser != null) {
         List<DataParser.Parser<?>> parsers = dataParser.getParsers();

         for (int i = 0; i < parsers.size(); i++) {
            DataParser.Parser<?> parser = parsers.get(i);
            DataParamsList.WidgetFactory defaultVal;
            if (DataParamsList.WidgetFactory.canMakeEnumButton(parser.type)) {
               defaultVal = (mc, p) -> DataParamsList.WidgetFactory.hackyEnumButton(mc, p, parser.type);
            } else {
               defaultVal = DataParamsList.WidgetFactory::suggestionEditBox;
            }

            DataParamsList.Entry entry = new DataParamsList.Entry(WIDGET_FACTORIES.getOrDefault(parser.type, defaultVal).create(this.minecraft, parser), parser);
            this.addEntry(entry);
            if (entry.widget instanceof CycleButton cycle && parser.defaultVal != null) {
               cycle.setValue(parser.defaultVal instanceof StringRepresentable s ? s.getSerializedName() : parser.defaultVal.toString());
            }
         }
      }
   }

   public void setEntries(List<DataParamsList.Entry> entries) {
      this.clearEntries();
      if (entries != null) {
         entries.forEach(x$0 -> this.addEntry(x$0));
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      if (this.parentScreen.nameEdit.isVisible()) {
         return false;
      }

      for (DataParamsList.Entry e : this.children()) {
         if (e.widget instanceof SuggestionEditBox suggestionEditBox
            && suggestionEditBox.isFocused()
            && suggestionEditBox.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
         }
      }

      return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
      if (this.parentScreen.nameEdit.isVisible()) {
         return false;
      } else {
         boolean ret = super.mouseClicked(mouseX, mouseY, clickType);
         if (mouseY > this.getY() && mouseY < this.getBottom()) {
            boolean clickedChild = this.getEntryAtPosition(mouseX, mouseY) != null;
            boolean clickedScrollBar = clickType == 0 && mouseX >= this.getScrollbarPosition() && mouseX < this.getScrollbarPosition() + 6;
            return clickedChild || clickedScrollBar;
         } else {
            return ret;
         }
      }
   }

   public void tick() {
      boolean isValid = true;

      for (DataParamsList.Entry entry : this.children()) {
         if (!entry.isValid()) {
            isValid = false;
            break;
         }
      }

      this.isValid = isValid;
   }

   public boolean isValid() {
      return this.isValid;
   }

   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      if (this.renderTopAndBottom) {
         PoseStack poseStack = graphics.pose();
         int dirtScale = 32;
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.0F, -1.0F);
         graphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
         graphics.blit(Screen.BACKGROUND_LOCATION, this.getX(), 0, 0.0F, 0.0F, this.getRight(), this.getY(), dirtScale, dirtScale);
         graphics.blit(
            Screen.BACKGROUND_LOCATION,
            this.getX(),
            this.getBottom(),
            0.0F,
            this.parentScreen.height - this.getBottom(),
            this.getWidth(),
            this.parentScreen.height - this.getBottom(),
            dirtScale,
            dirtScale
         );
         graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         int gradientDist = 4;
         graphics.fillGradient(RenderType.guiOverlay(), this.getX(), this.getY(), this.getRight(), this.getY() + gradientDist, -16777216, 0, 0);
         graphics.fillGradient(RenderType.guiOverlay(), this.getX(), this.getBottom() - gradientDist, this.getRight(), this.getBottom(), 0, -16777216, 0);
         poseStack.popPose();
      }

      if (!this.parentScreen.nameEdit.isVisible()) {
         for (DataParamsList.Entry entry : this.children()) {
            if (entry.widget.isFocused()) {
               entry.widget.render(graphics, mouseX, mouseY, partialTicks);
            }
         }
      }

      if (this.hoveredEntry != null) {
         this.parentScreen.renderTooltip(graphics, this.hoveredEntry.tooltips, mouseX, mouseY + 10);
         this.hoveredEntry = null;
      }
   }

   public class Entry extends net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<DataParamsList.Entry> {
      public final AbstractWidget widget;
      public final DataParser.Parser<?> parser;
      public final Component renderLabel;
      public final List<Component> tooltips = new ArrayList<>();
      protected boolean isValid = true;

      public Entry(AbstractWidget widget, DataParser.Parser<?> parser) {
         this.widget = widget;
         this.parser = parser;
         String raw = parser.key;
         String labelStranslationKey = parser.getTranlsationKey("label");
         MutableComponent translatedLabel = Component.translatable(labelStranslationKey);
         if (translatedLabel.getString().equals(labelStranslationKey)) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < raw.length(); i++) {
               int prev = i - 1;
               char c = raw.charAt(i);
               if (i != 0 && (prev <= -1 || Character.isAlphabetic(raw.charAt(prev)))) {
                  builder.append(c == '_' ? ' ' : c);
               } else {
                  builder.append(Character.toUpperCase(c));
               }
            }

            this.renderLabel = Component.literal(builder.toString());
         } else {
            this.renderLabel = translatedLabel;
         }

         this.tooltips.add(Component.literal(this.renderLabel.getString()).append(Component.literal(" (" + raw + ")")).withStyle(ChatFormatting.DARK_GREEN));
         this.createTranslatedTooltip("description", ChatFormatting.GREEN);
         if (parser.getExample() != null) {
            this.createRawTooltip("example", parser.getExample(), ChatFormatting.GREEN);
         }

         if (parser instanceof DataParser.NumberParser<?> numberParser) {
            this.createRawTooltip("range", numberParser.getMin() + " - " + numberParser.getMax(), ChatFormatting.GREEN);
         }

         this.createRawTooltip("default", parser.getDefaultValName(DataParamsList.this.minecraft.level), ChatFormatting.AQUA);
         this.createRawTooltip("type", parser.typeName, ChatFormatting.BLUE);
      }

      @Nullable
      private void createRawTooltip(String key, String value, ChatFormatting color) {
         this.tooltips
            .add(
               Component.translatable("gui.structure_gel.data_handler." + key)
                  .append(Component.literal(": "))
                  .withStyle(color)
                  .append(Component.literal(value).withStyle(ChatFormatting.GRAY))
            );
      }

      @Nullable
      private void createTranslatedTooltip(String key, ChatFormatting color) {
         String translationKey = this.parser.getTranlsationKey(key);
         MutableComponent translatedComponent = Component.translatable(translationKey);
         if (!translatedComponent.getString().equals(translationKey)) {
            this.tooltips
               .add(
                  Component.translatable("gui.structure_gel.data_handler." + key)
                     .append(Component.literal(": "))
                     .withStyle(color)
                     .append(translatedComponent.withStyle(ChatFormatting.GRAY))
               );
         }
      }

      public List<? extends GuiEventListener> children() {
         return List.of(this.widget);
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of(this.widget);
      }

      public void render(
         GuiGraphics graphics, int index, int rowTop, int rowLeft, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isHovered, float partialTicks
      ) {
         DataParamsList dataParamsList = DataParamsList.this;
         if (!dataParamsList.parentScreen.nameEdit.isVisible()) {
            int white = 16777215;
            int red = 12189696;
            Font font = dataParamsList.minecraft.font;
            int labelWidth = font.width(this.renderLabel);
            int maxWidth = 95;
            String labelToDraw = labelWidth > maxWidth ? font.substrByWidth(this.renderLabel, maxWidth).getString() + "..." : this.renderLabel.getString();
            graphics.drawString(font, labelToDraw, rowLeft - 76, rowTop + rowHeight / 2 - 4, this.isValid ? white : red);
            this.widget.setX(rowLeft + 30);
            this.widget.setY(rowTop);
            if (!this.widget.isFocused()) {
               this.widget.render(graphics, mouseX, mouseY, partialTicks);
            }

            if (!this.isValid) {
               graphics.drawString(font, "✗", rowLeft - 89, rowTop + rowHeight / 2 - 4, red);
            }

            if (mouseX > rowLeft - 70 && mouseX < rowLeft + 30 && mouseY > rowTop - rowHeight + 20 && mouseY < rowTop + 20) {
               dataParamsList.hoveredEntry = this;
            }
         }
      }

      public boolean isValid() {
         if (this.widget instanceof EditBox editBox) {
            String val = editBox.getValue();
            this.isValid = val != null && (val.isEmpty() || this.parser.validator.test(val, DataParamsList.this.minecraft.level));
            if (!this.isValid) {
               return false;
            }
         }

         return true;
      }

      public String getValue() {
         if (this.widget instanceof CycleButton<?> button) {
            return button.getValue().toString().toLowerCase(Locale.ENGLISH);
         } else if (this.widget instanceof EditBox editBox) {
            return editBox.getValue();
         } else {
            StructureGelMod.LOGGER.error("Unhandled widget type for DataParamsList.Entry.getValue(): " + this.widget.getClass().getCanonicalName());
            return "unknown";
         }
      }

      public void setValue(String value) {
         if (this.widget instanceof CycleButton button) {
            button.setValue(value);
         } else if (this.widget instanceof EditBox editBox) {
            editBox.setValue(value);
         } else {
            StructureGelMod.LOGGER.error("Unhandled widget type for DataParamsList.Entry.setValue(): " + this.widget.getClass().getCanonicalName());
         }
      }
   }

   public interface WidgetFactory {
      AbstractWidget create(Minecraft var1, DataParser.Parser<?> var2);

      static <T> AbstractWidget suggestionEditBox(Minecraft mc, DataParser.Parser<?> parser, Supplier<Collection<T>> suggestions, Function<T, String> toString) {
         SuggestionEditBox editBox = new SuggestionEditBox(mc.font, 0, 0, 190, 20, Component.literal(""), 10, suggestions, toString);
         editBox.setMaxLength(512);
         return editBox;
      }

      private static AbstractWidget suggestionEditBox(Minecraft mc, DataParser.Parser<?> parser) {
         return suggestionEditBox(mc, parser, parser::getSuggestions, parser.getSuggestionToString());
      }

      static AbstractWidget registryEditBox(Minecraft mc, DataParser.Parser<?> parser, Registry<?> registry) {
         FeatureFlagSet features = mc.level.enabledFeatures();
         return suggestionEditBox(
            mc,
            parser,
            () -> registry.entrySet()
               .stream()
               .filter(e -> !(e.getValue() instanceof FeatureElement feature && !feature.isEnabled(features)))
               .map(e -> ((ResourceKey)e.getKey()).location())
               .toList(),
            ResourceLocation::toString
         );
      }

      static <T> AbstractWidget registryEditBox(Minecraft mc, DataParser.Parser<?> parser) {
         return parser instanceof DataParser.ResourceKeyParser<?> registryParser
            ? suggestionEditBox(
               mc,
               registryParser,
               () -> registryParser.getSuggestions().stream().map(ResourceKey::location).<String>map(ResourceLocation::toString).toList(),
               Function.identity()
            )
            : suggestionEditBox(mc, parser);
      }

      private static AbstractWidget booleanButton(Minecraft mc, DataParser.Parser<?> parser) {
         return cycleButton(mc, parser, "true", "false");
      }

      private static <T extends Enum<T> & StringRepresentable> AbstractWidget enumButton(Minecraft mc, DataParser.Parser<?> parser, Class<T> clazz) {
         return cycleButton(mc, parser, rec$ -> rec$.getSerializedName(), clazz.getEnumConstants());
      }

      private static boolean canMakeEnumButton(Class<?> clazz) {
         return Enum.class.isAssignableFrom(clazz) && StringRepresentable.class.isAssignableFrom(clazz);
      }

      private static <T extends Enum<T> & StringRepresentable> AbstractWidget hackyEnumButton(Minecraft mc, DataParser.Parser<?> parser, Class<?> clazz) {
         if (!canMakeEnumButton(clazz)) {
            throw new IllegalArgumentException("clazz (" + clazz.getSimpleName() + ") must be an Enum and " + StringRepresentable.class.getSimpleName());
         }

         Class<T> realClazz = (Class<T>)clazz;
         return enumButton(mc, parser, realClazz);
      }

      private static AbstractWidget directionButton(Minecraft mc, DataParser.Parser<?> parser) {
         return cycleButton(
            mc, parser, Direction::getSerializedName, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
         );
      }

      static <T extends Enum<T>> AbstractWidget cycleButton(Minecraft mc, DataParser.Parser<?> parser, Function<T, String> toString, T... values) {
         return cycleButton(mc, parser, Arrays.<T>stream(values).map(toString).toArray(String[]::new));
      }

      static AbstractWidget cycleButton(Minecraft mc, DataParser.Parser<?> parser, String... values) {
         Builder<String> builder = CycleButton.builder(
            s -> Component.literal(s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1).toLowerCase(Locale.ENGLISH))
         );
         builder.displayOnlyValue();
         builder.withValues(values);
         return builder.create(0, 0, 60, 20, Component.literal(""));
      }
   }
}
