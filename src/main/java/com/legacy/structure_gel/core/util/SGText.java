package com.legacy.structure_gel.core.util;

import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor.ARGB32;

public class SGText {
   public static final int WHITE = 14737632;
   public static final int LABEL_COLOR = 10526880;
   public static final int TRANSPARENT_GRAY = ARGB32.color(128, 128, 128, 128);
   public static final int INVALID_TEXT_COLOR = 14703708;
   public static final Style KEYBIND_STYLE = Style.EMPTY.withColor(parseHex("#5FBABF"));
   public static final Style VALUE_LABEL_STYLE = Style.EMPTY.withColor(parseHex("#A387C6"));
   public static final Style X_LABEL_STYLE = Style.EMPTY.withColor(parseHex("#FF9696"));
   public static final Style Y_LABEL_STYLE = Style.EMPTY.withColor(parseHex("#96FF96"));
   public static final Style Z_LABEL_STYLE = Style.EMPTY.withColor(parseHex("#9696FF"));
   public static final Style UNDERLINE_BOLD = Style.EMPTY.withBold(true).withUnderlined(true);
   public static final MutableComponent NEW_LINE = Component.literal(" ");
   public static final MutableComponent BULLET_POINT = Component.literal("• ");
   public static final MutableComponent TYPE_LABEL = Component.translatable("gui.structure_gel.type_label");
   public static final MutableComponent PROPERTIES_LABEL = Component.translatable("gui.structure_gel.properties_label");
   public static final MutableComponent DISPLAY_NAME_LABEL = Component.translatable("gui.structure_gel.display_name_label");
   public static final MutableComponent OFFSET_LABEL = Component.translatable("gui.structure_gel.offset_label");
   public static final MutableComponent MIN_LABEL = Component.translatable("gui.structure_gel.min_label");
   public static final MutableComponent MAX_LABEL = Component.translatable("gui.structure_gel.max_label");
   public static final MutableComponent LEFT_LABEL = Component.translatable("gui.structure_gel.left_label");
   public static final MutableComponent UP_LABEL = Component.translatable("gui.structure_gel.up_label");
   public static final MutableComponent FORWARD_LABEL = Component.translatable("gui.structure_gel.forward_label");
   public static final MutableComponent ADD_LABEL = Component.translatable("gui.structure_gel.add");
   public static final MutableComponent REMOVE_LABEL = Component.translatable("gui.structure_gel.remove");
   public static final MutableComponent EXPORT_LABEL = Component.translatable("gui.structure_gel.export");
   public static final String WATERLOGGED_KEY = "gui.structure_gel.waterlogged";
   public static final String CONNECT_TO_BLOCKS_KEY = "gui.structure_gel.connect_to_blocks";
   public static final String MARK_POST_PROCESSING_KEY = "gui.structure_gel.mark_post_processing";
   private static final Function<String, Component> KEYBIND_COLOR = Util.memoize(s -> {
      MutableComponent component = Component.empty();
      Style format = Style.EMPTY.withColor(ChatFormatting.GRAY);
      String[] split = s.split(" ");

      for (int i = 0; i < split.length; i++) {
         String word = split[i];
         if (word.startsWith("[")) {
            format = KEYBIND_STYLE;
         }

         component.append(Component.literal(word + (i == split.length - 1 ? "" : " ")).withStyle(format));
         if (word.endsWith("]")) {
            format = Style.EMPTY.withColor(ChatFormatting.GRAY);
         }
      }

      return component;
   });
   private static final Function<String, String> KEYBIND_STRING = Util.memoize(
      s -> s.replace("Middle Button", "Middle Click")
         .replace("Left Button", "Left Click")
         .replace("Right Button", "Right Click")
         .replace("Left Shift", "Shift")
         .replace("Right Shift", "Shift")
         .replace("Left Control", "Control")
         .replace("Right Control", "Control")
   );

   private static final TextColor parseHex(String hex) {
      return (TextColor)TextColor.parseColor(hex).getOrThrow(s -> new IllegalArgumentException(s + " is now a valid hex color"));
   }

   public static Component applyKeybindFilter(Component original) {
      return KEYBIND_COLOR.apply(original.getString());
   }

   public static String keybindString(KeyMapping key) {
      return "[" + remapKeybindString(key.getTranslatedKeyMessage().getString()) + "]";
   }

   public static String remapKeybindString(String input) {
      return KEYBIND_STRING.apply(input);
   }
}
