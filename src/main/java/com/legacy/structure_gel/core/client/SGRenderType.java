package com.legacy.structure_gel.core.client;

import java.util.function.BiFunction;

import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class SGRenderType extends RenderType
{
	public static final RenderStateShard.ShaderStateShard WORLD_WIREFRAME_SHADER = new RenderStateShard.ShaderStateShard(SGShaders.WORLD_WIREFRAME);

	//@formatter:off
	private static final BiFunction<Boolean, Boolean, RenderType> WORLD_WIREFRAME = Util.memoize((transparent, fabulous) -> create(
			StructureGelMod.locate("world_wireframe_solid").toString(), 
			DefaultVertexFormat.POSITION_COLOR, 
			VertexFormat.Mode.QUADS, 
			1536, 
			false, 
			true, 
			RenderType.CompositeState.builder().setShaderState(WORLD_WIREFRAME_SHADER).setTransparencyState(transparent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY).setOutputState(fabulous ? PARTICLES_TARGET : MAIN_TARGET).createCompositeState(false)));
	//@formatter:on

	public static RenderType worldWireframe(boolean transparent)
	{
		return worldWireframe(transparent, true);
	}
	
	public static RenderType worldWireframe(boolean transparent, boolean fabulous)
	{
		return WORLD_WIREFRAME.apply(transparent, fabulous);
	}

	private SGRenderType(String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState)
	{
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
