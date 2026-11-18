package com.legacy.structure_gel.api.structure.jigsaw;

import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class JigsawRegistryHelper {
   public final String modid;
   public final String prefix;
   public final BootstapContext<StructureTemplatePool> bootstrapContext;
   public final HolderGetter<StructureProcessorList> processorLookup;
   public final HolderGetter<StructureTemplatePool> templateLookup;

   public JigsawRegistryHelper(String modid, BootstapContext<StructureTemplatePool> bootstrapContext) {
      this(modid, "", bootstrapContext);
   }

   public JigsawRegistryHelper(String modid, String prefix, BootstapContext<StructureTemplatePool> bootstrapContext) {
      this.modid = modid;
      this.prefix = prefix;
      this.bootstrapContext = bootstrapContext;
      this.processorLookup = bootstrapContext.lookup(Registries.PROCESSOR_LIST);
      this.templateLookup = bootstrapContext.lookup(Registries.TEMPLATE_POOL);
   }

   public HolderGetter<StructureProcessorList> structureProcessorListLookup() {
      return this.processorLookup;
   }

   public JigsawRegistryHelper setModID(String modid) {
      return new JigsawRegistryHelper(modid, this.prefix, this.bootstrapContext);
   }

   public JigsawRegistryHelper setPrefix(String prefix) {
      return new JigsawRegistryHelper(this.modid, prefix, this.bootstrapContext);
   }

   public JigsawPoolBuilder poolBuilder() {
      return new JigsawPoolBuilder(this);
   }

   public JigsawRegistryHelper.PoolRegisterBuilder registerBuilder() {
      return new JigsawRegistryHelper.PoolRegisterBuilder();
   }

   public void register(String name, List<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> pool) {
      this.registerBuilder().pools(pool).register(name);
   }

   public void register(String name, JigsawPoolBuilder poolBuilder) {
      this.registerBuilder().pools(poolBuilder).register(name);
   }

   public void register(String name, String fallback, JigsawPoolBuilder poolBuilder) {
      this.registerBuilder().fallback(fallback).pools(poolBuilder).register(name);
   }

   public void register(String name, JigsawRegistryHelper.PoolRegisterBuilder registerBuilder) {
      registerBuilder.register(name);
   }

   protected ResourceLocation locatePiece(String key) {
      return new ResourceLocation(this.modid, this.prefix + key);
   }

   public class PoolRegisterBuilder {
      ResourceLocation fallback = new ResourceLocation("empty");
      List<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> pool = Collections.emptyList();
      Projection projection = Projection.RIGID;

      public JigsawRegistryHelper.PoolRegisterBuilder fallback(ResourceLocation fallback) {
         this.fallback = fallback;
         return this;
      }

      public JigsawRegistryHelper.PoolRegisterBuilder fallback(String fallback) {
         this.fallback = JigsawRegistryHelper.this.locatePiece(fallback);
         return this;
      }

      public JigsawRegistryHelper.PoolRegisterBuilder pools(JigsawPoolBuilder poolBuilder) {
         return this.pools(poolBuilder.build());
      }

      public JigsawRegistryHelper.PoolRegisterBuilder pools(List<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> pool) {
         this.pool = pool;
         return this;
      }

      public JigsawRegistryHelper.PoolRegisterBuilder projection(Projection projection) {
         this.projection = projection;
         return this;
      }

      public void register(String poolName) {
         this.register(JigsawRegistryHelper.this.locatePiece(poolName));
      }

      public void register(ResourceLocation poolName) {
         this.register(ResourceKey.create(Registries.TEMPLATE_POOL, poolName));
      }

      public void register(ResourceKey<StructureTemplatePool> poolName) {
         Holder<StructureTemplatePool> fallbackHolder = JigsawRegistryHelper.this.templateLookup
            .getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, this.fallback));
         JigsawRegistryHelper.this.bootstrapContext.register(poolName, new StructureTemplatePool(fallbackHolder, this.pool, this.projection));
      }
   }
}
