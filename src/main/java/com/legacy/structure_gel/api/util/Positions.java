package com.legacy.structure_gel.api.util;

import org.joml.Vector3d;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;

public class Positions
{
	public static BlockPos blockPos(Position pos)
	{
		return blockPos(pos.x(), pos.y(), pos.z());
	}
	
	public static BlockPos blockPos(Vector3f vec)
	{
		return blockPos(vec.x, vec.y, vec.z);
	}
	
	public static BlockPos blockPos(Vector3d vec)
	{
		return blockPos(vec.x, vec.y, vec.z);
	}
	
	public static BlockPos blockPos(double x, double y, double z)
	{
		return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}
	
	public static BlockPos blockPos(float x, float y, float z)
	{
		return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}
	
	public static BlockPos blockPos(long x, long y, long z)
	{
		return new BlockPos((int) x, (int) y, (int) z);
	}
	
	public static Vec3i vec3i(double x, double y, double z)
	{
		return new Vec3i(Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}
	
	public static Vec3i vec3i(float x, float y, float z)
	{
		return new Vec3i(Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}
	
	public static Vec3i vec3i(long x, long y, long z)
	{
		return new Vec3i((int) x, (int) y, (int) z);
	}
}
