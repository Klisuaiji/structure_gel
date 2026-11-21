package com.legacy.structure_gel.core.capability.level;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.SGConfig;
import com.legacy.structure_gel.core.StructureGelMod;
import com.legacy.structure_gel.core.item.building_tool.ActionHistory;
import com.legacy.structure_gel.core.item.building_tool.CapturedBlocks;
import com.legacy.structure_gel.core.network.SGPacketHandler;
import com.legacy.structure_gel.core.network.s_to_c.EditClipboardPacket;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

public class BuildingToolPlayerData extends SavedData
{
	final ServerLevel level;
	final String playerName;
	final ActionHistory actionHistory;
	final Object2ObjectLinkedOpenHashMap<BoundingBox, CapturedBlocks> clipboard;

	private BuildingToolPlayerData(ServerLevel level, String playerName)
	{
		this(level, playerName, new ActionHistory(playerName), new Object2ObjectLinkedOpenHashMap<>());
	}

	private BuildingToolPlayerData(ServerLevel level, String playerName, ActionHistory actionHistory, Object2ObjectLinkedOpenHashMap<BoundingBox, CapturedBlocks> clipboard)
	{
		this.level = level;
		this.playerName = playerName;
		this.actionHistory = actionHistory;
		this.clipboard = clipboard;
	}

	public static String getID(String playerName)
	{
		return StructureGelMod.MODID + "/building_tool/players/" + playerName;
	}

	private static SavedData.Factory<BuildingToolPlayerData> factory(ServerLevel level, String playerName)
	{
		return new SavedData.Factory<>(() -> new BuildingToolPlayerData(level, playerName), (tag, lookup) -> BuildingToolPlayerData.load(level, tag, lookup));
	}

	public static BuildingToolPlayerData get(ServerLevel level, String playerName)
	{
		BuildingToolPlayerData ret = level.getDataStorage().computeIfAbsent(factory(level, playerName), getID(playerName));
		BuildingToolWorldData.get(level).addPlayer(playerName);
		return ret;
	}

	public Player getPlayer(Level level)
	{
		for (Player player : level.players())
			if (player.getGameProfile().getName().equals(this.playerName))
				return player;
		return null;
	}

	public ActionHistory getActionHistory()
	{
		return this.actionHistory;
	}

	@Nullable
	public CapturedBlocks getCapturedBlocks(BoundingBox bounds, String owner)
	{
		var ret = this.clipboard.getAndMoveToFirst(bounds);
		if (ret != null)
			this.setDirty();
		return ret;
	}

	public void addToClipboard(BoundingBox bounds, CapturedBlocks captured)
	{
		this.clipboard.putAndMoveToFirst(bounds, captured);
		BoundingBox removeKey = BoundingBox.infinite();
		if (this.clipboard.size() > SGConfig.COMMON.getBuildingToolMaxClipboard())
		{
			removeKey = this.clipboard.lastKey();
			this.clipboard.removeLast();
		}

		if (this.getPlayer(this.level) instanceof ServerPlayer player)
			SGPacketHandler.sendToClient(new EditClipboardPacket(player.getGameProfile().getName(), bounds, captured, removeKey), player);

		this.setDirty();

		/*System.out.println("----------");
		for (var entry : this.clipboard.object2ObjectEntrySet())
			System.out.println(entry);*/
	}

	public void addToClipboardDirect(BoundingBox bounds, CapturedBlocks captured, String owner)
	{
		if (!owner.equals(this.playerName))
			return;
		this.clipboard.putAndMoveToFirst(bounds, captured);
		this.setDirty();
	}

	public void removeFromClipboard(BoundingBox bounds, String owner)
	{
		if (!owner.equals(this.playerName))
			return;
		this.clipboard.remove(bounds);
		this.setDirty();
	}

	public void clearClipboard()
	{
		this.clipboard.clear();
		this.setDirty();
	}

	private static final String PLAYER_KEY = "player", ACTION_HISTORY_KEY = "action_history",
			CLIPBOARD_KEY = "clipboard", BOUNDS_KEY = "bounds", CAPTURED_KEY = "captured";

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup)
	{
		tag.putString(PLAYER_KEY, this.playerName);

		if (!this.actionHistory.isExpired(this.level))
			tag.put(ACTION_HISTORY_KEY, ActionHistory.CODEC.encodeStart(NbtOps.INSTANCE, this.actionHistory).getOrThrow(s -> new IllegalStateException("couldn't serialize action history for " + this.playerName + ": " + s)));

		if (!this.clipboard.isEmpty())
		{
			ListTag clipboardList = new ListTag();
			for (var entry : this.clipboard.object2ObjectEntrySet())
			{
				CompoundTag clipboardTag = new CompoundTag();
				BoundingBox b = entry.getKey();
				clipboardTag.putIntArray(BOUNDS_KEY, new int[] { b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ() });

				clipboardTag.put(CAPTURED_KEY, entry.getValue().toCompressedTag(lookup));

				clipboardList.add(clipboardTag);
			}
			tag.put(CLIPBOARD_KEY, clipboardList);
		}
		return tag;
	}

	private static BuildingToolPlayerData load(ServerLevel level, CompoundTag tag, HolderLookup.Provider lookup)
	{
		String playerName = tag.getString(PLAYER_KEY);

		ActionHistory actionHistory = tag.contains(ACTION_HISTORY_KEY, Tag.TAG_COMPOUND) ? ActionHistory.CODEC.decode(NbtOps.INSTANCE, tag.getCompound(ACTION_HISTORY_KEY)).getOrThrow(s -> new IllegalStateException("couldn't deserialize action history: " + s)).getFirst() : new ActionHistory(playerName);

		Object2ObjectLinkedOpenHashMap<BoundingBox, CapturedBlocks> clipboard = new Object2ObjectLinkedOpenHashMap<>();

		if (tag.contains(CLIPBOARD_KEY, Tag.TAG_LIST))
		{
			ListTag clipboardList = tag.getList(CLIPBOARD_KEY, Tag.TAG_COMPOUND);
			for (Tag t : clipboardList)
			{
				if (t instanceof CompoundTag)
				{
					CompoundTag clipboardTag = (CompoundTag) t;
					int[] boundsArray = clipboardTag.getIntArray(BOUNDS_KEY);
					BoundingBox bounds = new BoundingBox(boundsArray[0], boundsArray[1], boundsArray[2], boundsArray[3], boundsArray[4], boundsArray[5]);

					CapturedBlocks captured = CapturedBlocks.fromCompressedTag(lookup, clipboardTag.getCompound(CAPTURED_KEY));

					clipboard.put(bounds, captured);
				}
			}
		}

		return new BuildingToolPlayerData(level, playerName, actionHistory, clipboard);
	}

	@Override
	public boolean isDirty()
	{
		return super.isDirty() || this.actionHistory.isDirty(); // Make sure we save when action history is dirty
	}

	@Override
	public CompoundTag save(HolderLookup.Provider lookup)
	{
		CompoundTag tag = super.save(lookup);
		this.actionHistory.setDirty(false);
		return tag;
	}

	public static class BuildingToolClientData extends BuildingToolPlayerData
	{
		final Object2ObjectLinkedOpenHashMap<String, Object2ObjectLinkedOpenHashMap<BoundingBox, CapturedBlocks>> otherPlayerClipboards;

		public BuildingToolClientData(String playerName)
		{
			super(null, playerName);
			this.otherPlayerClipboards = new Object2ObjectLinkedOpenHashMap<>();
		}

		@Override
		@Nullable
		public CapturedBlocks getCapturedBlocks(BoundingBox bounds, String owner)
		{
			if (!this.playerName.equals(owner))
			{
				var other = this.otherPlayerClipboards.get(owner);
				if (other != null)
				{
					var captured = other.get(bounds);
					if (captured != null)
						return captured;
				}
			}
			return super.getCapturedBlocks(bounds, owner);
		}
		
		@Override
		public void addToClipboardDirect(BoundingBox bounds, CapturedBlocks captured, String owner)
		{
			if (!this.playerName.equals(owner))
			{
				var other = this.otherPlayerClipboards.computeIfAbsent(owner, s -> new Object2ObjectLinkedOpenHashMap<>());
				if (other != null)
				{
					other.putAndMoveToFirst(bounds, captured);
				}
			}
			super.addToClipboardDirect(bounds, captured, owner);
		}

		@Override
		public void removeFromClipboard(BoundingBox bounds, String owner)
		{
			if (!this.playerName.equals(owner))
			{
				var other = this.otherPlayerClipboards.get(owner);
				if (other != null)
					other.remove(bounds);
			}
			super.removeFromClipboard(bounds, owner);
		}
		
		@Override
		public void clearClipboard()
		{
			this.otherPlayerClipboards.clear();
			super.clearClipboard();
		}

		// ServerLevel is null so this doesn't work anyway
		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup)
		{
			return tag;
		}

		// ServerLevel is null so this doesn't work anyway
		@Override
		public CompoundTag save(HolderLookup.Provider lookup)
		{
			return new CompoundTag();
		}
	}
}
