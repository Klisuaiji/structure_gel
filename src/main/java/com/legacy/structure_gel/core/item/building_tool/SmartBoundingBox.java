package com.legacy.structure_gel.core.item.building_tool;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public final class SmartBoundingBox
{
	private final CornerType cornerA, cornerB;
	private int ax, ay, az, bx, by, bz;

	public SmartBoundingBox(int ax, int ay, int az, int bx, int by, int bz)
	{
		this.ax = ax;
		this.ay = ay;
		this.az = az;
		this.bx = bx;
		this.by = by;
		this.bz = bz;

		this.cornerA = CornerType.getFromPositions(ax, ay, az, bx, by, bz);
		this.cornerB = this.cornerA.opposite();
	}

	public static SmartBoundingBox fromCorners(Vec3i a, Vec3i b)
	{
		return new SmartBoundingBox(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
	}

	public static SmartBoundingBox from(SmartBoundingBox bounds)
	{
		return SmartBoundingBox.fromCorners(bounds.getPosA(), bounds.getPosB());
	}

	public static SmartBoundingBox zero()
	{
		return new SmartBoundingBox(0, 0, 0, 0, 0, 0);
	}

	public void set(BoundingBox newBounds)
	{
		this.setPosA(cornerA.getPosFromBounds(newBounds));
		this.setPosB(cornerB.getPosFromBounds(newBounds));
	}

	public Vec3i getPosA()
	{
		return this.getCorner(this.cornerA);
	}

	public void setPosA(Vec3i a)
	{
		this.setCorner(this.cornerA, a);
	}

	public Vec3i getPosB()
	{
		return this.getCorner(this.cornerB);
	}

	public void setPosB(Vec3i b)
	{
		this.setCorner(this.cornerB, b);
	}

	public Vec3i getMin()
	{
		return this.getCorner(CornerType.NNN);
	}

	public void setMin(Vec3i min)
	{
		this.setCorner(CornerType.NNN, min);
	}

	public Vec3i getMax()
	{
		return this.getCorner(CornerType.PPP);
	}

	public void setMax(Vec3i max)
	{
		this.setCorner(CornerType.PPP, max);
	}

	public int getXSpan()
	{
		return this.getCorner(CornerType.PPP).getX() - this.getCorner(CornerType.NNN).getX() + 1;
	}

	public int getYSpan()
	{
		return this.getCorner(CornerType.PPP).getY() - this.getCorner(CornerType.NNN).getY() + 1;
	}

	public int getZSpan()
	{
		return this.getCorner(CornerType.PPP).getZ() - this.getCorner(CornerType.NNN).getZ() + 1;
	}

	public Corner[] allCorners()
	{
		CornerType[] cornerTypes = CornerType.values();
		Corner[] corners = new Corner[8];
		for (int i = 0; i < 8; i++)
			corners[i] = new Corner(cornerTypes[i], cornerTypes[i].getPosFromBounds(ax, ay, az, bx, by, bz));
		return corners;
	}

	public Corner[] selectionPointCorners()
	{
		return new Corner[] { new Corner(this.cornerA, this.cornerA.getPosFromBounds(ax, ay, az, bx, by, bz)), new Corner(this.cornerB, this.cornerB.getPosFromBounds(ax, ay, az, bx, by, bz)) };
	}

	public Vec3i getCorner(CornerType corner)
	{
		return corner.getPosFromBounds(ax, ay, az, bx, by, bz);
	}

	public void setCorner(CornerType corner, Vec3i pos)
	{
		if (this.cornerA.xPositive == corner.xPositive)
			this.ax = pos.getX();
		else if (this.cornerB.xPositive == corner.xPositive)
			this.bx = pos.getX();

		if (this.cornerA.yPositive == corner.yPositive)
			this.ay = pos.getY();
		else if (this.cornerB.yPositive == corner.yPositive)
			this.by = pos.getY();

		if (this.cornerA.zPositive == corner.zPositive)
			this.az = pos.getZ();
		else if (this.cornerB.zPositive == corner.zPositive)
			this.bz = pos.getZ();
	}

	public void moveTo(Vec3i pos)
	{
		this.moveTo(CornerType.NNN, pos);
	}

	public void moveTo(CornerType corner, Vec3i pos)
	{
		Vec3i a = this.getCorner(corner);
		Vec3i b = this.getCorner(corner.opposite());
		int dx = b.getX() - a.getX();
		int dy = b.getY() - a.getY();
		int dz = b.getZ() - a.getZ();
		this.setCorner(corner, pos);
		this.setCorner(corner.opposite(), pos.offset(dx, dy, dz));
	}

	public BoundingBox bounds()
	{
		return BoundingBox.fromCorners(this.getPosA(), this.getPosB());
	}

	public AABB aabb()
	{
		return AABB.of(this.bounds());
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof SmartBoundingBox bb ? ax == bb.ax && ay == bb.ay && az == bb.az && bx == bb.bx && by == bb.by && bz == bb.bz : false;
	}

	public static class Corner
	{
		public final CornerType type;
		public int x, y, z;

		public Corner(CornerType type, int x, int y, int z)
		{
			this.type = type;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public Corner(CornerType type, Vec3i pos)
		{
			this(type, pos.getX(), pos.getY(), pos.getZ());
		}

		public Vec3i getPos()
		{
			return new Vec3i(x, y, z);
		}

		public Vec3i getPoint()
		{
			return new Vec3i(type.xPositive ? x + 1 : x, type.yPositive ? y + 1 : y, type.zPositive ? z + 1 : z);
		}
	}

	public static enum CornerType implements StringRepresentable
	{
		NNN(false, false, false),
		NNP(false, false, true),
		NPN(false, true, false),
		NPP(false, true, true),
		PNN(true, false, false),
		PNP(true, false, true),
		PPN(true, true, false),
		PPP(true, true, true);

		public static final Codec<CornerType> CODEC = StringRepresentable.fromEnum(CornerType::values);
		public static final StreamCodec<ByteBuf, CornerType> STREAM_CODEC = ByteBufCodecs.INT.map(CornerType::byId, CornerType::ordinal);

		final String name;
		final boolean xPositive, yPositive, zPositive;

		CornerType(boolean xPositive, boolean yPositive, boolean zPositive)
		{
			this.name = (xPositive ? "p" : "n") + (yPositive ? "p" : "n") + (zPositive ? "p" : "n");
			this.xPositive = xPositive;
			this.yPositive = yPositive;
			this.zPositive = zPositive;
		}

		@Override
		public String getSerializedName()
		{
			return this.name;
		}
		
		public static CornerType byId(int id)
		{
			CornerType[] vals = CornerType.values();
			if (id > 0 && id < vals.length)
				return vals[id];
			return CornerType.NNN;
		}

		public static CornerType getFromPositions(int x, int y, int z, int otherX, int otherY, int otherZ)
		{
			int lowX = x < otherX ? 0 : 4;
			int lowY = y < otherY ? 0 : 2;
			int lowZ = z < otherZ ? 0 : 1;
			return CornerType.values()[lowX | lowY | lowZ];
		}

		public CornerType opposite()
		{
			return CornerType.values()[7 - this.ordinal()];
		}

		public Vec3i getPosFromBounds(BoundingBox bb)
		{
			return this.getPosFromBounds(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ());
		}

		public Vec3i getPosFromBounds(int ax, int ay, int az, int bx, int by, int bz)
		{
			return new Vec3i(this.getCoord(xPositive, ax, bx), this.getCoord(yPositive, ay, by), this.getCoord(zPositive, az, bz));
		}

		private int getCoord(boolean isPositive, int a, int b)
		{
			return isPositive ? Math.max(a, b) : Math.min(a, b);
		}
	}
}
