package com.legacy.structure_gel.core;

import org.apache.commons.lang3.tuple.Pair;

import com.legacy.structure_gel.api.config.ConfigBuilder;
import com.legacy.structure_gel.core.util.Internal;

import net.neoforged.neoforge.common.ModConfigSpec;

@Internal
public class SGConfig
{
	public static final Common COMMON;
	public static final Client CLIENT;
	protected static final ModConfigSpec COMMON_SPEC;
	protected static final ModConfigSpec CLIENT_SPEC;

	static
	{
		Pair<Common, ModConfigSpec> specPairCommon = new ModConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPairCommon.getRight();
		COMMON = specPairCommon.getLeft();

		Pair<Client, ModConfigSpec> specPairClient = new ModConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = specPairClient.getRight();
		CLIENT = specPairClient.getLeft();
	}

	public static class Common
	{
		private final ModConfigSpec.IntValue buildingToolMaxUndos;
		private final ModConfigSpec.IntValue buildingToolMaxClipboard;
		private final ModConfigSpec.IntValue buildingToolHistoryExpiration;

		private final ModConfigSpec.BooleanValue consoleDebug;
		private final ModConfigSpec.BooleanValue advancedGelBehavior;

		public Common(ModConfigSpec.Builder builder)
		{
			builder.push("building_tool");
			this.buildingToolMaxUndos = ConfigBuilder.makeInt(builder, "max_undos", "The amount of undo operations saved by the Building Tool.", 64, 8, 256);
			this.buildingToolMaxClipboard = ConfigBuilder.makeInt(builder, "clipboard_size", "The amount of copied block regions that can be stored at a time with the Clone tool.", 9, 1, 32);
			this.buildingToolHistoryExpiration = ConfigBuilder.makeInt(builder, "expiration_time", "The amount hours that a player's actions should stay in memory for. Use -1 to save forever.", 24, -1, 240);
			builder.pop();

			builder.push("debug");
			this.consoleDebug = ConfigBuilder.makeBoolean(builder, "console_debug", "When true, allows extra debug logging to be printed to the console.", false);
			this.advancedGelBehavior = ConfigBuilder.makeBoolean(builder, "advanced_gel_behavior", "When true:\n - Gel blocks can be clicked through like air when holding items that don't interact with them.\n - Gel blocks can be replaced like air when not holding gel or crouching.\n - Gel blocks automatically replace destroyed neighboring blocks.", true);
			builder.pop();
		}

		public int getBuildingToolMaxUndos()
		{
			return this.buildingToolMaxUndos.get();
		}

		public int getBuildingToolMaxClipboard()
		{
			return this.buildingToolMaxClipboard.get();
		}

		public boolean consoleDebug()
		{
			return this.consoleDebug.get();
		}

		public boolean advancedGelBehavior()
		{
			return this.advancedGelBehavior.get();
		}

		public long buildingToolHistoryExpiration()
		{
			return this.buildingToolHistoryExpiration.get() * 72000L; // 72000 ticks in an hour
		}
	}

	public static class Client
	{
		private final ModConfigSpec.BooleanValue showStructureBlockInfo;
		public final ModConfigSpec.BooleanValue showAir, showVoid, showBarrier, showLight;
		
		private final ModConfigSpec.BooleanValue threadBuildingTool;
		private final ModConfigSpec.IntValue maxCloneCompileTime;

		public Client(ModConfigSpec.Builder builder)
		{
			builder.push("structure_block");
			this.showStructureBlockInfo = ConfigBuilder.makeBoolean(builder, "show_structure_block_info", "Displays info on top of Structure Blocks and Jigsaws in world.", true);
			builder.push("show_invisible");
			this.showAir = ConfigBuilder.makeBoolean(builder, "air", "", true);
			this.showVoid = ConfigBuilder.makeBoolean(builder, "structure_void", "", true);
			this.showBarrier = ConfigBuilder.makeBoolean(builder, "barrier", "", true);
			this.showLight = ConfigBuilder.makeBoolean(builder, "light", "", true);
			builder.pop();
			builder.pop();

			builder.push("building_tool");
			this.threadBuildingTool = ConfigBuilder.makeBoolean(builder, "thread_building_tool", "Makes the render used by the Building Tool operate in a threaded context.", true);
			this.maxCloneCompileTime = ConfigBuilder.makeInt(builder, "max_clone_compile_time", "The amount of time (milliseconds) until a clone preview is aborted. This stops your game from being frozen when copying large regions.", 8000, 0, 1000000000);
			builder.pop();
		}

		public boolean showStructureBlockInfo()
		{
			return this.showStructureBlockInfo.get();
		}

		public boolean threadBuildingTool()
		{
			return this.threadBuildingTool.get();
		}

		public int maxCloneCompileTime()
		{
			return this.maxCloneCompileTime.get();
		}
	}
}
