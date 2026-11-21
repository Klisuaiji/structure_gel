package com.legacy.structure_gel.core.client;

import java.io.IOException;

import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public class SGShaders
{
	@javax.annotation.Nullable
	public static ShaderInstance WORLD_WIREFRAME;

	public static void init(RegisterShadersEvent event)
	{
		try
		{
			event.registerShader(
					new ShaderInstance(event.getResourceProvider(),
							StructureGelMod.locate("core/world_wireframe").toString(),
							DefaultVertexFormat.POSITION_COLOR),
					shader -> WORLD_WIREFRAME = shader);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Failed to load Structure Gel world wireframe shader", e);
		}
	}
}
