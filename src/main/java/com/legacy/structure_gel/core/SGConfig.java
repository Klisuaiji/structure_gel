package com.legacy.structure_gel.core;

import com.legacy.structure_gel.api.config.ConfigBuilder;
import com.legacy.structure_gel.core.util.Internal;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.apache.commons.lang3.tuple.Pair;

@Internal
public class SGConfig {
   public static final SGConfig.Common COMMON;
   public static final SGConfig.Client CLIENT;
   protected static final ModConfigSpec COMMON_SPEC;
   protected static final ModConfigSpec CLIENT_SPEC;

   static {
      Pair<SGConfig.Common, ModConfigSpec> specPairCommon = new Builder().configure(SGConfig.Common::new);
      COMMON_SPEC = (ModConfigSpec)specPairCommon.getRight();
      COMMON = (SGConfig.Common)specPairCommon.getLeft();
      Pair<SGConfig.Client, ModConfigSpec> specPairClient = new Builder().configure(SGConfig.Client::new);
      CLIENT_SPEC = (ModConfigSpec)specPairClient.getRight();
      CLIENT = (SGConfig.Client)specPairClient.getLeft();
   }

   public static class Client {
      private final BooleanValue showStructureBlockInfo;
      public final BooleanValue showAir;
      public final BooleanValue showVoid;
      public final BooleanValue showBarrier;
      public final BooleanValue showLight;
      private final BooleanValue threadBuildingTool;
      private final IntValue maxCloneCompileTime;

      public Client(Builder builder) {
         builder.push("structure_block");
         this.showStructureBlockInfo = ConfigBuilder.makeBoolean(
            builder, "show_structure_block_info", "Displays info on top of Structure Blocks and Jigsaws in world.", true
         );
         builder.push("show_invisible");
         this.showAir = ConfigBuilder.makeBoolean(builder, "air", "", true);
         this.showVoid = ConfigBuilder.makeBoolean(builder, "structure_void", "", true);
         this.showBarrier = ConfigBuilder.makeBoolean(builder, "barrier", "", true);
         this.showLight = ConfigBuilder.makeBoolean(builder, "light", "", true);
         builder.pop();
         builder.pop();
         builder.push("building_tool");
         this.threadBuildingTool = ConfigBuilder.makeBoolean(
            builder, "thread_building_tool", "Makes the render used by the Building Tool operate in a threaded context.", true
         );
         this.maxCloneCompileTime = ConfigBuilder.makeInt(
            builder,
            "max_clone_compile_time",
            "The amount of time (milliseconds) until a clone preview is aborted. This stops your game from being frozen when copying large regions.",
            8000,
            0,
            1000000000
         );
         builder.pop();
      }

      public boolean showStructureBlockInfo() {
         return (Boolean)this.showStructureBlockInfo.get();
      }

      public boolean threadBuildingTool() {
         return (Boolean)this.threadBuildingTool.get();
      }

      public int maxCloneCompileTime() {
         return (Integer)this.maxCloneCompileTime.get();
      }
   }

   public static class Common {
      private final IntValue buildingToolMaxUndos;
      private final IntValue buildingToolMaxClipboard;
      private final IntValue buildingToolHistoryExpiration;
      private final BooleanValue consoleDebug;
      private final BooleanValue advancedGelBehavior;

      public Common(Builder builder) {
         builder.push("building_tool");
         this.buildingToolMaxUndos = ConfigBuilder.makeInt(builder, "max_undos", "The amount of undo operations saved by the Building Tool.", 64, 8, 256);
         this.buildingToolMaxClipboard = ConfigBuilder.makeInt(
            builder, "clipboard_size", "The amount of copied block regions that can be stored at a time with the Clone tool.", 9, 1, 32
         );
         this.buildingToolHistoryExpiration = ConfigBuilder.makeInt(
            builder, "expiration_time", "The amount hours that a player's actions should stay in memory for. Use -1 to save forever.", 24, -1, 240
         );
         builder.pop();
         builder.push("debug");
         this.consoleDebug = ConfigBuilder.makeBoolean(builder, "console_debug", "When true, allows extra debug logging to be printed to the console.", false);
         this.advancedGelBehavior = ConfigBuilder.makeBoolean(
            builder,
            "advanced_gel_behavior",
            "When true:\n - Gel blocks can be clicked through like air when holding items that don't interact with them.\n - Gel blocks can be replaced like air when not holding gel or crouching.\n - Gel blocks automatically replace destroyed neighboring blocks.",
            true
         );
         builder.pop();
      }

      public int getBuildingToolMaxUndos() {
         return (Integer)this.buildingToolMaxUndos.get();
      }

      public int getBuildingToolMaxClipboard() {
         return (Integer)this.buildingToolMaxClipboard.get();
      }

      public boolean consoleDebug() {
         return (Boolean)this.consoleDebug.get();
      }

      public boolean advancedGelBehavior() {
         return (Boolean)this.advancedGelBehavior.get();
      }

      public long buildingToolHistoryExpiration() {
         return ((Integer)this.buildingToolHistoryExpiration.get()).intValue() * 72000L;
      }
   }
}
