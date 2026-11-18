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
import net.minecraft.world.level.dimension.DimensionType.MonsterSettings;

public class DimensionTypeBuilder {
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

   private DimensionTypeBuilder() {
   }

   public static DimensionTypeBuilder of() {
      return new DimensionTypeBuilder();
   }

   public DimensionTypeBuilder fixedTime(OptionalLong fixedTime) {
      this.fixedTime = fixedTime;
      return this;
   }

   public DimensionTypeBuilder hasSkyLight(boolean hasSkyLight) {
      this.hasSkyLight = hasSkyLight;
      return this;
   }

   public DimensionTypeBuilder hasCeiling(boolean hasCeiling) {
      this.hasCeiling = hasCeiling;
      return this;
   }

   public DimensionTypeBuilder ultrawarm(boolean ultrawarm) {
      this.ultrawarm = ultrawarm;
      return this;
   }

   public DimensionTypeBuilder natural(boolean natural) {
      this.natural = natural;
      return this;
   }

   public DimensionTypeBuilder coordinateScale(double coordinateScale) {
      this.coordinateScale = coordinateScale;
      return this;
   }

   public DimensionTypeBuilder bedWorks(boolean bedWorks) {
      this.bedWorks = bedWorks;
      return this;
   }

   public DimensionTypeBuilder respawnAnchorWorks(boolean respawnAnchorWorks) {
      this.respawnAnchorWorks = respawnAnchorWorks;
      return this;
   }

   public DimensionTypeBuilder minY(int minY) {
      this.minY = minY;
      return this;
   }

   public DimensionTypeBuilder height(int height) {
      this.height = height;
      return this;
   }

   public DimensionTypeBuilder logicalHeight(int logicalHeight) {
      this.logicalHeight = logicalHeight;
      return this;
   }

   public DimensionTypeBuilder infiniburn(TagKey<Block> infiniburn) {
      this.infiniburn = infiniburn;
      return this;
   }

   public DimensionTypeBuilder effects(ResourceLocation effects) {
      this.effects = effects;
      return this;
   }

   public DimensionTypeBuilder ambientLight(float ambientLight) {
      this.ambientLight = ambientLight;
      return this;
   }

   public DimensionTypeBuilder piglinSafe(boolean piglinSafe) {
      this.piglinSafe = piglinSafe;
      return this;
   }

   public DimensionTypeBuilder hasRaids(boolean hasRaids) {
      this.hasRaids = hasRaids;
      return this;
   }

   public DimensionTypeBuilder monsterSpawnLightTest(IntProvider lightRange) {
      this.monsterSpawnLightTest = lightRange;
      return this;
   }

   public DimensionTypeBuilder monsterSpawnBlockLightLimit(int monsterSpawnBlockLightLimit) {
      this.monsterSpawnBlockLightLimit = monsterSpawnBlockLightLimit;
      return this;
   }

   public DimensionType build() {
      return new DimensionType(
         this.fixedTime,
         this.hasSkyLight,
         this.hasCeiling,
         this.ultrawarm,
         this.natural,
         this.coordinateScale,
         this.bedWorks,
         this.respawnAnchorWorks,
         this.minY,
         this.height,
         this.logicalHeight,
         this.infiniburn,
         this.effects,
         this.ambientLight,
         new MonsterSettings(this.piglinSafe, this.hasRaids, this.monsterSpawnLightTest, this.monsterSpawnBlockLightLimit)
      );
   }
}
