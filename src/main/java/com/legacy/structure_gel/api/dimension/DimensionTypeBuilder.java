package com.legacy.structure_gel.api.dimension;

import java.util.OptionalLong;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * A way of creating a DimensionType in a builder format. All values are
 * defaulted to what they would be for the overworld.
 *
 * @author Silver_David
 */
public class DimensionTypeBuilder
{
	private OptionalLong fixedTime = OptionalLong.empty();
	private boolean hasSkyLight = true;
	private boolean hasCeiling = false;
	private boolean ultrawarm = false;
	private boolean natural = true;
	private double coordinateScale = 1.0;
	private boolean bedWorks = true;
	private boolean respawnAnchorWorks = false;
	private int minY = -64;
	private int height = 384;
	private int logicalHeight = 384;
	private TagKey<Block> infiniburn = BlockTags.INFINIBURN_OVERWORLD;
	private ResourceLocation effects = BuiltinDimensionTypes.OVERWORLD_EFFECTS;
	private float ambientLight = 0.0F;

	private boolean piglinSafe = false;
	private boolean hasRaids = true;
	private IntProvider monsterSpawnLightTest = UniformInt.of(0, 7);
	private int monsterSpawnBlockLightLimit = 0;

	private DimensionTypeBuilder()
	{

	}

	/**
	 * Creates a new instance of the builder
	 *
	 * @return A new {@link DimensionTypeBuilder}
	 */
	public static DimensionTypeBuilder of()
	{
		return new DimensionTypeBuilder();
	}

	/**
	 * Locks the sun at a specific time if a value is present.
	 * <p>
	 * Empty optional by default.
	 *
	 * @param fixedTime
	 * @return This instance
	 */
	public DimensionTypeBuilder fixedTime(OptionalLong fixedTime)
	{
		this.fixedTime = fixedTime;
		return this;
	}

	/**
	 * Determines if things that require the sky can occur, such as daylight
	 * detectors and phantom spawning
	 * <p>
	 * True by default.
	 *
	 * @param hasSkyLight
	 * @return This instance
	 */
	public DimensionTypeBuilder hasSkyLight(boolean hasSkyLight)
	{
		this.hasSkyLight = hasSkyLight;
		return this;
	}

	/**
	 * Determines a few things in mob spawning for if your world has a ceiling.
	 * <p>
	 * False by default.
	 * 
	 * @param hasCeiling
	 * @return This instance
	 */
	public DimensionTypeBuilder hasCeiling(boolean hasCeiling)
	{
		this.hasCeiling = hasCeiling;
		return this;
	}

	/**
	 * Causes water to evaporate and lava to flow further.
	 * <p>
	 * False by default.
	 *
	 * @param ultrawarm
	 * @return This instance
	 */
	public DimensionTypeBuilder ultrawarm(boolean ultrawarm)
	{
		this.ultrawarm = ultrawarm;
		return this;
	}

	/**
	 * When false, compasses and clocks don't function.<br>
	 * When true, nether portals spawn zombified piglins.
	 * <p>
	 * True by default.
	 *
	 * @param natural
	 * @return This instance
	 */
	public DimensionTypeBuilder natural(boolean natural)
	{
		this.natural = natural;
		return this;
	}

	/**
	 * A multiplier applied to coordinates when traveling between dimensions.
	 * <p>
	 * 1.0 by default.
	 *
	 * @param coordinateScale
	 * @return This instance
	 */
	public DimensionTypeBuilder coordinateScale(double coordinateScale)
	{
		this.coordinateScale = coordinateScale;
		return this;
	}

	/**
	 * Determines if you're allowed to use a bed.
	 * <p>
	 * True by default.
	 *
	 * @param bedWorks
	 * @return This instance
	 */
	public DimensionTypeBuilder bedWorks(boolean bedWorks)
	{
		this.bedWorks = bedWorks;
		return this;
	}

	/**
	 * Determines if you're allowed to use a respawn anchor.
	 * <p>
	 * False by default.
	 *
	 * @param respawnAnchorWorks
	 * @return This instance
	 */
	public DimensionTypeBuilder respawnAnchorWorks(boolean respawnAnchorWorks)
	{
		this.respawnAnchorWorks = respawnAnchorWorks;
		return this;
	}

	/**
	 * Sets the y level at the bottom of the world.
	 * <p>
	 * -64 by default.
	 * 
	 * @param minY
	 * @return
	 */
	public DimensionTypeBuilder minY(int minY)
	{
		this.minY = minY;
		return this;
	}

	/**
	 * Sets the distance between the bottom of the world and the top.
	 * <p>
	 * 384 by default.
	 * 
	 * @param height
	 * @return
	 */
	public DimensionTypeBuilder height(int height)
	{
		this.height = height;
		return this;
	}

	/**
	 * The maximum height that various mechanics are allowed to send the player.
	 * This is to prevent players from getting stuck on the nether roof.
	 * <p>
	 * 384 by default.
	 *
	 * @param logicalHeight
	 * @return This instance
	 */
	public DimensionTypeBuilder logicalHeight(int logicalHeight)
	{
		this.logicalHeight = logicalHeight;
		return this;
	}

	/**
	 * A tag of blocks that are allowed to burn forever in the dimension.
	 * <p>
	 * infiniburn_overworld by default.
	 *
	 * @param infiniburn
	 * @return This instance
	 */
	public DimensionTypeBuilder infiniburn(TagKey<Block> infiniburn)
	{
		this.infiniburn = infiniburn;
		return this;
	}

	/**
	 * Sets what registered effects the dimension has, such as fog.
	 * <p>
	 * Overworld by default.
	 *
	 * @param effects
	 * @return This instance
	 */
	public DimensionTypeBuilder effects(ResourceLocation effects)
	{
		this.effects = effects;
		return this;
	}

	/**
	 * Determins how blocks should render. It's what makes the underside of blocks
	 * brighter in the nether.
	 * <p>
	 * 0 by default.
	 *
	 * @param ambientLight
	 * @return This instance
	 */
	public DimensionTypeBuilder ambientLight(float ambientLight)
	{
		this.ambientLight = ambientLight;
		return this;
	}

	/**
	 * When set to false, piglins will zombify in the dimension.
	 * <p>
	 * False by default.
	 *
	 * @param piglinSafe
	 * @return This instance
	 */
	public DimensionTypeBuilder piglinSafe(boolean piglinSafe)
	{
		this.piglinSafe = piglinSafe;
		return this;
	}

	/**
	 * Determines if players with bad omen can trigger a raid.
	 * <p>
	 * True by default.
	 *
	 * @param hasRaids
	 * @return This instance
	 */
	public DimensionTypeBuilder hasRaids(boolean hasRaids)
	{
		this.hasRaids = hasRaids;
		return this;
	}

	/**
	 * Impacts the probability of a mob spawning based on the brightness at a
	 * location. If the brightness at a location is greater than a random value
	 * within the range passed, spawns are prevented.
	 * <p>
	 * The brightness at a location is calculated as the maximum value between the
	 * block light and the sky light based on the time of day.
	 * <p>
	 * 0 - 7 by default.
	 * 
	 * @param lightRange
	 * @return This instance
	 */
	public DimensionTypeBuilder monsterSpawnLightTest(IntProvider lightRange)
	{
		this.monsterSpawnLightTest = lightRange;
		return this;
	}

	/**
	 * The block light level that will outright prevent mobs from spawning.
	 * <p>
	 * 0 by default.
	 * 
	 * @param monsterSpawnBlockLightLimit
	 * @return This instance
	 */
	public DimensionTypeBuilder monsterSpawnBlockLightLimit(int monsterSpawnBlockLightLimit)
	{
		this.monsterSpawnBlockLightLimit = monsterSpawnBlockLightLimit;
		return this;
	}

	/**
	 * Creates the DimensionType with the provided settings
	 *
	 * @return This instance
	 */
	public DimensionType build()
	{
		return new DimensionType(fixedTime, hasSkyLight, hasCeiling, ultrawarm, natural, coordinateScale, bedWorks, respawnAnchorWorks, minY, height, logicalHeight, infiniburn, effects, ambientLight, new DimensionType.MonsterSettings(piglinSafe, hasRaids, monsterSpawnLightTest, monsterSpawnBlockLightLimit));
	}
}
