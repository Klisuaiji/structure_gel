package com.legacy.structure_gel.core.client.widget;

import com.legacy.blue_skies.compat.*;
import com.legacy.structure_gel.api.client.gui.LayerWidgetHolder;
import com.legacy.structure_gel.core.util.SGText;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.neoforged.neoforge.common.util.Lazy;

public class SuggestionEditBox extends EditBox {
   private final SuggestionEditBox.SuggestionComponent<?> suggestions;

   public <T> SuggestionEditBox(
      Font font, int x, int y, int width, int height, Component name, int maxSuggestionLines, Supplier<Collection<T>> suggestions, Function<T, String> toString
   ) {
      super(font, x, y, width, height, name);
      this.suggestions = new SuggestionEditBox.SuggestionComponent<>(this, suggestions, toString, maxSuggestionLines);
      this.setResponder(s -> {});
   }

   public SuggestionEditBox(Font font, int x, int y, int width, int height, Component name, int maxSuggestionLines, Collection<?> suggestions) {
      this(font, x, y, width, height, name, maxSuggestionLines, () -> suggestions, Object::toString);
   }

   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      if (this.isFocused() && this.isActive()) {
         this.suggestions.x = this.getX() - 1;
         this.suggestions.y = this.getY() + this.getHeight() + 1;
         this.suggestions.render(graphics, mouseX, mouseY, partialTicks);
      }
   }

   public void setFocused(boolean isFocused) {
      super.setFocused(isFocused);
      if (isFocused) {
         this.suggestions.update(this.getValue());
      }
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      boolean isOverText = super.isMouseOver(mouseX, mouseY);
      return !this.isFocused() || !isOverText && !this.suggestions.isMouseOver(mouseX, mouseY) ? isOverText : true;
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      return this.isFocused() && this.suggestions.mouseScrolled(mouseX, mouseY, deltaX, deltaY) && this.isActive()
         ? true
         : super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
      return this.isFocused() && this.suggestions.isMouseOver(mouseX, mouseY) && this.isActive()
         ? this.suggestions.mouseClicked(mouseX, mouseY, clickType)
         : super.mouseClicked(mouseX, mouseY, clickType);
   }

   public boolean keyPressed(int keyCode, int keyB, int modifiers) {
      if (this.isActive()) {
         if (keyCode == 256 && this.isFocused()) {
            this.setFocused(false);
            return true;
         }

         if (this.suggestions.keyPressed(keyCode, keyB, modifiers)) {
            return true;
         }
      }

      return super.keyPressed(keyCode, keyB, modifiers);
   }

   public void setResponder(Consumer<String> responder) {
      super.setResponder(s -> {
         responder.accept(s);
         this.suggestions.update(this.getValue());
      });
   }

   public boolean isHoveredOrFocused() {
      return super.isHoveredOrFocused() && LayerWidgetHolder.LayerAwareWidget.isTop(this);
   }

   public static class SuggestionComponent<T> implements Renderable, GuiEventListener {
      private final Minecraft mc = Minecraft.getInstance();
      private final EditBox parent;
      private int maxLines;
      private final int absoluteMaxLines;
      private final Supplier<Collection<T>> allOptions;
      private final Function<T, String> toStringFunc;
      public int x;
      public int y;
      private int height = 10;
      private int width = 10;
      private List<String> currentOptions = new ArrayList<>();
      private int lineRenderStart = 0;
      private int selectedLine = 0;

      public SuggestionComponent(EditBox parent, Supplier<Collection<T>> suggestions, Function<T, String> toString, int maxLines) {
         this.parent = parent;
         this.x = parent.getX() - 1;
         this.y = parent.getY() + parent.getHeight() + 1;
         this.allOptions = suggestions;
         this.toStringFunc = toString;
         this.absoluteMaxLines = maxLines;
         this.maxLines = maxLines;
         this.update("");
      }

      public SuggestionComponent(EditBox parent, Collection<T> suggestions, Function<T, String> toString, int maxLines) {
         this(parent, Lazy.of(() -> suggestions), toString, maxLines);
      }

      public boolean isFocused() {
         return false;
      }

      public void setFocused(boolean isFocused) {
      }

      public List<String> retrieveAll() {
         List<String> all = this.allOptions.get().stream().map(this.toStringFunc).collect(Collectors.toList());
         this.maxLines = Math.min(all.size(), this.absoluteMaxLines);
         return all;
      }

      public void update(String input) {
         this.currentOptions.clear();
         List<String> all = this.retrieveAll();
         if (input.isEmpty()) {
            this.currentOptions.addAll(all);
         } else {
            all.stream()
               .filter(s -> s.startsWith(input) || s.contains(":") && s.substring(s.indexOf(":") + 1).startsWith(input))
               .forEach(this.currentOptions::add);
            all.stream().filter(s -> !this.currentOptions.contains(s)).filter(s -> s.contains(input)).forEach(this.currentOptions::add);
         }

         this.currentOptions.sort(String::compareToIgnoreCase);
         int size = this.currentOptions.size();
         List<String> putToTop = new ArrayList<>();

         for (int i = 0; i < size; i++) {
            String s = this.currentOptions.get(i);
            if (LegacyRL.isValid(s)) {
               ResourceLocation loc = ResourceLocation.parse(s);
               if (loc.getPath().startsWith(input) || loc.getPath().equals(input) || s.equals(input)) {
                  putToTop.add(putToTop.size(), s);
               }
            } else if (s.startsWith(input)) {
               putToTop.add(putToTop.size(), s);
            }
         }

         this.currentOptions.removeAll(putToTop);
         this.currentOptions.addAll(0, putToTop);
         int maxWidth = 0;

         for (String op : this.currentOptions) {
            int w = this.mc.font.width(op);
            if (w > maxWidth) {
               maxWidth = w;
            }
         }

         this.width = maxWidth + 2;
         int lines = Math.min(this.maxLines, this.currentOptions.size());
         this.height = 9 * lines + lines * 2 + 4;
         this.selectedLine = 0;
         this.lineRenderStart = 0;
      }

      @Nullable
      public String getSelected() {
         return this.selectedLine < this.currentOptions.size() && this.selectedLine > -1 ? this.currentOptions.get(this.selectedLine) : null;
      }

      public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         PoseStack poseStack = graphics.pose();
         int size = this.currentOptions.size();
         if (size >= 1) {
            if (this.maxLines > 0) {
               poseStack.pushPose();
               poseStack.translate(0.0F, 0.0F, 10.0F);
               graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, ARGB32.color(200, 10, 10, 10));
               Font font = this.mc.font;

               for (int line = 0; line < this.maxLines; line++) {
                  int renderLine = line + this.lineRenderStart;
                  if (renderLine < size) {
                     graphics.drawString(
                        font,
                        this.currentOptions.get(renderLine),
                        this.x + 1,
                        this.y + 4 + line * (9 + 2),
                        renderLine == this.selectedLine ? ChatFormatting.AQUA.getColor() : ChatFormatting.DARK_GREEN.getColor()
                     );
                  }
               }

               if (size > this.maxLines) {
                  int startX = this.x + this.width;
                  int h = 6;
                  int startY = this.y + (int)((double)this.selectedLine / size * (this.height - h));
                  graphics.fill(startX, startY, startX + 2, startY + 9 + h, Integer.MAX_VALUE);
               }

               poseStack.popPose();
            }

            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, -10.0F);
            graphics.drawString(this.mc.font, this.getSelected(), this.x + 5, this.y - this.parent.getHeight() / 2 - 5, SGText.TRANSPARENT_GRAY);
            poseStack.popPose();
         }
      }

      public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
         if (this.currentOptions.size() > 1) {
            int dir = deltaY < 0.0 ? -1 : 1;
            if (dir < 0) {
               this.moveSelectorDown();
            } else if (dir > 0) {
               this.moveSelectorUp();
            }
         }

         return true;
      }

      public boolean keyPressed(int keyCode, int keyB, int modifiers) {
         if (keyCode == 258) {
            if (!this.parent.getValue().isBlank()) {
               String selectedVal = this.getSelected();
               if (selectedVal != null && !this.parent.getValue().equals(selectedVal)) {
                  this.parent.setValue(selectedVal);
                  return true;
               }
            }
         } else {
            if (keyCode == 264) {
               this.moveSelectorDown();
               return true;
            }

            if (keyCode == 265) {
               this.moveSelectorUp();
               return true;
            }
         }

         return false;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int clickType) {
         int clickedLine = (int)((mouseY - this.y - 2.0) / (9 + 2));
         this.selectedLine = Math.min(Math.max(0, clickedLine), this.maxLines - 1) + this.lineRenderStart;
         String selected = this.getSelected();
         if (selected != null) {
            this.parent.setValue(selected);
            return true;
         } else {
            return false;
         }
      }

      public boolean isMouseOver(double mouseX, double mouseY) {
         return mouseX > this.x && mouseX < this.x + this.width && mouseY > this.y && mouseY < this.y + this.height;
      }

      public void moveSelectorDown() {
         if (this.selectedLine < this.currentOptions.size() - 1) {
            this.selectedLine++;
            if (this.selectedLine > this.lineRenderStart + this.maxLines - 1) {
               this.lineRenderStart++;
            }
         } else {
            this.selectedLine = 0;
            this.lineRenderStart = 0;
         }
      }

      public void moveSelectorUp() {
         if (this.selectedLine > 0) {
            this.selectedLine--;
            if (this.selectedLine < this.lineRenderStart) {
               this.lineRenderStart--;
            }
         } else {
            this.selectedLine = this.currentOptions.size() - 1;
            if (this.selectedLine > this.lineRenderStart + this.maxLines - 1) {
               this.lineRenderStart = this.currentOptions.size() - this.maxLines;
            }
         }
      }
   }
}
