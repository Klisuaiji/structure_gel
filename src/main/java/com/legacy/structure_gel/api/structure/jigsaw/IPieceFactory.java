package com.legacy.structure_gel.api.structure.jigsaw;

import java.util.Optional;

import com.legacy.structure_gel.api.structure.ExtendedJigsawStructure;
import com.legacy.structure_gel.core.structure.jigsaw.VanillaJigsawStructurePiece;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Used by {@link ExtendedJigsawStructure} to create instances of
 * {@link PoolElementStructurePiece} during world generation.
 * <p>
 * Setting a piece factory is done through {@link JigsawCapability}.
 * 
 * @author Silver_David
 *
 */
public interface IPieceFactory
{
	public static IPieceFactory VANILLA = VanillaJigsawStructurePiece::new;

	PoolElementStructurePiece create(Context context);

	public static final record Context(StructureTemplateManager structureManager, StructurePoolElement poolElement, BlockPos pos, int groundLevelDelta, Rotation rotation, BoundingBox bounds, LiquidSettings liquidSettings, Optional<JigsawCapability> jigsawCapability)
	{
		/**
		 * @return An Optional for the capability, casted to the class passed, or null
		 *         if not applicable.
		 */
		public <T extends JigsawCapability> Optional<T> jigsawCapability(Class<T> expectedClass)
		{
			return this.jigsawCapability().map(cap -> expectedClass.isInstance(cap) ? expectedClass.cast(cap) : null);
		}
	}
}