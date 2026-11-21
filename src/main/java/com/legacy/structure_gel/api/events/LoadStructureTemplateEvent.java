package com.legacy.structure_gel.api.events;

import com.legacy.structure_gel.core.StructureGelMod;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

public class LoadStructureTemplateEvent extends Event {
   private final ResourceLocation id;
   private final List<Palette> palettes;
   private final List<StructureEntityInfo> entityInfoList;
   private final Vec3i size;

   public LoadStructureTemplateEvent(ResourceLocation id, List<Palette> palettes, List<StructureEntityInfo> entityInfoList, Vec3i size) {
      this.id = id;
      this.palettes = palettes;
      this.entityInfoList = entityInfoList;
      this.size = size;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public List<Palette> getPalettes() {
      return this.palettes;
   }

   public List<StructureEntityInfo> getEntityInfoList() {
      return this.entityInfoList;
   }

   public Vec3i getSize() {
      return this.size;
   }

   public Optional<StructureBlockInfo> getBlock(Palette palette, BlockPos pos) {
      if (this.isInBounds(pos)) {
         for (StructureBlockInfo info : palette.blocks()) {
            if (info.pos().equals(pos)) {
               return Optional.of(info);
            }
         }
      } else {
         StructureGelMod.LOGGER
            .warn("Attempted to get a block from a StructureTemplate outside of its size bounds. Template: {}, Pos: {}, Size: {}", this.id, pos, this.size);
      }

      return Optional.empty();
   }

   public boolean hasBlock(Palette palette, BlockPos pos, Predicate<StructureBlockInfo> predicate) {
      return this.getBlock(palette, pos).map(predicate::test).orElse(false);
   }

   public void setBlock(BlockPos pos, BlockState state, CompoundTag tag) {
      for (Palette palette : this.getPalettes()) {
         this.setBlock(palette, pos, state, tag);
      }
   }

   public void setBlock(Palette palette, BlockPos pos, BlockState state, CompoundTag tag) {
      this.setBlock(palette, new StructureBlockInfo(pos, state, tag));
   }

   public void setBlock(Palette palette, StructureBlockInfo structureBlockInfo) {
      BlockPos placePos = structureBlockInfo.pos();
      if (this.isInBounds(placePos)) {
         List<StructureBlockInfo> blocks = palette.blocks();
         blocks.removeIf(info -> info.pos().equals(placePos));
         blocks.add(structureBlockInfo);
      } else {
         StructureGelMod.LOGGER
            .warn("Attempted to add a block to a StructureTemplate outside of its size bounds. Template: {}, Pos: {}, Size: {}", this.id, placePos, this.size);
      }
   }

   public void setJigsaw(BlockState jigsawState, BlockPos pos, Consumer<JigsawBlockEntity> jigsawConsumer) {
      CompoundTag tag = this.getJigsawTag(jigsawState, pos, jigsawConsumer);

      for (Palette palette : this.getPalettes()) {
         this.setBlock(palette, pos, jigsawState, tag);
      }
   }

   public void setJigsaw(Palette palette, BlockState jigsawState, BlockPos pos, Consumer<JigsawBlockEntity> jigsawConsumer) {
      this.setBlock(palette, pos, jigsawState, this.getJigsawTag(jigsawState, pos, jigsawConsumer));
   }

   private CompoundTag getJigsawTag(BlockState jigsawState, BlockPos pos, Consumer<JigsawBlockEntity> jigsawConsumer) {
      JigsawBlockEntity jigsaw = new JigsawBlockEntity(pos, jigsawState);
      jigsawConsumer.accept(jigsaw);
      return jigsaw.saveWithoutMetadata();
   }

   public void addEntity(Vec3 pos, BlockPos blockPos, CompoundTag nbt) {
      this.addEntity(new StructureEntityInfo(pos, blockPos, nbt));
   }

   public void addEntity(StructureEntityInfo structureEntityInfo) {
      BlockPos placePos = structureEntityInfo.blockPos;
      if (this.isInBounds(placePos)) {
         this.entityInfoList.add(structureEntityInfo);
      } else {
         StructureGelMod.LOGGER
            .warn("Attempted to add an entity to a StructureTemplate outside of its size bounds. Template: {}, Pos: {}, Size: {}", this.id, placePos, this.size);
      }
   }

   public boolean isInBounds(BlockPos pos) {
      return pos.getX() < this.size.getX() && pos.getY() < this.size.getY() && pos.getZ() < this.size.getZ();
   }

   public BlockPos moveInBounds(BlockPos pos) {
      return new BlockPos(
         Mth.clamp(pos.getX(), 0, this.size.getX() - 1), Mth.clamp(pos.getY(), 0, this.size.getY() - 1), Mth.clamp(pos.getZ(), 0, this.size.getZ() - 1)
      );
   }
}
