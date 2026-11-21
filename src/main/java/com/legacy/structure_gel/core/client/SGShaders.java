package com.legacy.structure_gel.core.client;

import com.legacy.structure_gel.core.StructureGelMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public class SGShaders
{
	public static final ShaderProgram WORLD_WIREFRAME = register("world_wireframe", DefaultVertexFormat.POSITION_COLOR);

	public static void init(RegisterShadersEvent event)
	{
		event.registerShader(WORLD_WIREFRAME);
	}

	private static ShaderProgram register(String name, VertexFormat vertexFormat)
	{
		return register(name, vertexFormat, ShaderDefines.EMPTY);
	}

	private static ShaderProgram register(String name, VertexFormat vertexFormat, ShaderDefines shaderDefs)
	{
		return new ShaderProgram(StructureGelMod.locate("core/" + name), vertexFormat, shaderDefs);
	}
}
