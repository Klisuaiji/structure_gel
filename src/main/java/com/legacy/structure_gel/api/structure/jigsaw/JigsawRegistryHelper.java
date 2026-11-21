package com.legacy.structure_gel.api.structure.jigsaw;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

/**
 * Assists in the registry process for Jigsaw structures to shorten names and
 * give shorter methods. This is used along with {@link JigsawPoolBuilder}
 *
 * @author Silver_David
 */
public class JigsawRegistryHelper
{
	public final String modid;
	public final String prefix;
	public final BootstrapContext<StructureTemplatePool> bootstrapContext;
	public final HolderGetter<StructureProcessorList> processorLookup;
	public final HolderGetter<StructureTemplatePool> templateLookup;

	/**
	 * @param modid
	 * @param bootstrapContext
	 */
	public JigsawRegistryHelper(String modid, BootstrapContext<StructureTemplatePool> bootstrapContext)
	{
		this(modid, "", bootstrapContext);
	}

	/**
	 * @param modid
	 * @param prefix
	 *            Inserted between modid and the name provided for the structure
	 *            file in your JigsawPoolBuilder
	 * @param bootstrapContext
	 */
	public JigsawRegistryHelper(String modid, String prefix, BootstrapContext<StructureTemplatePool> bootstrapContext)
	{
		this.modid = modid;
		this.prefix = prefix;
		this.bootstrapContext = bootstrapContext;
		this.processorLookup = bootstrapContext.lookup(Registries.PROCESSOR_LIST);
		this.templateLookup = bootstrapContext.lookup(Registries.TEMPLATE_POOL);
	}

	public HolderGetter<StructureProcessorList> structureProcessorListLookup()
	{
		return this.processorLookup;
	}

	/**
	 * Creates a new instance with the passed mod id and this instance's prefix
	 *
	 * @param modid
	 * @return A new jigsaw registry helper
	 */
	public JigsawRegistryHelper setModID(String modid)
	{
		return new JigsawRegistryHelper(modid, this.prefix, this.bootstrapContext);
	}

	/**
	 * Creates a new instance with the passed prefix and this instance's modid
	 *
	 * @param prefix
	 * @return A new jigsaw registry helper
	 */
	public JigsawRegistryHelper setPrefix(String prefix)
	{
		return new JigsawRegistryHelper(this.modid, prefix, this.bootstrapContext);
	}

	/**
	 * @return A new jigsaw pool builder using this registry helper
	 */
	public JigsawPoolBuilder poolBuilder()
	{
		return new JigsawPoolBuilder(this);
	}

	public PoolRegisterBuilder registerBuilder()
	{
		return new PoolRegisterBuilder();
	}
	
	public void register(String name, List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> pool)
	{
		this.registerBuilder().pools(pool).register(name);
	}
	
	public void register(String name, JigsawPoolBuilder poolBuilder)
	{
		this.registerBuilder().pools(poolBuilder).register(name);
	}
	
	public void register(String name, String fallback, JigsawPoolBuilder poolBuilder)
	{
		this.registerBuilder().fallback(fallback).pools(poolBuilder).register(name);
	}

	public void register(String name, PoolRegisterBuilder registerBuilder)
	{
		registerBuilder.register(name);
	}
	
	/**
	 * Used to register a structure template pool.
	 * 
	 * @author Silver_David
	 *
	 */
	public class PoolRegisterBuilder
	{
		ResourceLocation fallback = ResourceLocation.tryParse("empty");
		List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> pool = Collections.emptyList();
		StructureTemplatePool.Projection projection = StructureTemplatePool.Projection.RIGID;

		public PoolRegisterBuilder fallback(ResourceLocation fallback)
		{
			this.fallback = fallback;
			return this;
		}

		public PoolRegisterBuilder fallback(String fallback)
		{
			this.fallback = JigsawRegistryHelper.this.locatePiece(fallback);
			return this;
		}

		public PoolRegisterBuilder pools(JigsawPoolBuilder poolBuilder)
		{
			return this.pools(poolBuilder.build());
		}
		
		public PoolRegisterBuilder pools(List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> pool)
		{
			this.pool = pool;
			return this;
		}

		public PoolRegisterBuilder projection(StructureTemplatePool.Projection projection)
		{
			this.projection = projection;
			return this;
		}

		/**
		 * Gets a ResourceLocation for the key input using the modid and prefix as
		 * "modid:prefix+key"
		 */
		public void register(String poolName)
		{
			this.register(JigsawRegistryHelper.this.locatePiece(poolName));
		}

		public void register(ResourceLocation poolName)
		{
			this.register(ResourceKey.create(Registries.TEMPLATE_POOL, poolName)); 
		}
		
		public void register(ResourceKey<StructureTemplatePool> poolName)
		{
			Holder<StructureTemplatePool> fallbackHolder = JigsawRegistryHelper.this.templateLookup.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, this.fallback));
			JigsawRegistryHelper.this.bootstrapContext.register(poolName, new StructureTemplatePool(fallbackHolder, pool, projection));
		}
	}

	/**
	 * Gets a ResourceLocation for the key input using the modid and prefix as
	 * "modid:prefix+key"
	 *
	 * @param key
	 * @return The built resource location
	 */
	protected ResourceLocation locatePiece(String key)
	{
		return ResourceLocation.fromNamespaceAndPath(this.modid, this.prefix + key);
	}
}
