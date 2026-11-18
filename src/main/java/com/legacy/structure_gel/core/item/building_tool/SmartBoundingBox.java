package com.legacy.structure_gel.core.item.building_tool;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public final class SmartBoundingBox {
   private final SmartBoundingBox.CornerType cornerA;
   private final SmartBoundingBox.CornerType cornerB;
   private int ax;
   private int ay;
   private int az;
   private int bx;
   private int by;
   private int bz;

   public SmartBoundingBox(int ax, int ay, int az, int bx, int by, int bz) {
      this.ax = ax;
      this.ay = ay;
      this.az = az;
      this.bx = bx;
      this.by = by;
      this.bz = bz;
      this.cornerA = SmartBoundingBox.CornerType.getFromPositions(ax, ay, az, bx, by, bz);
      this.cornerB = this.cornerA.opposite();
   }

   public static SmartBoundingBox fromCorners(Vec3i a, Vec3i b) {
      return new SmartBoundingBox(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
   }

   public static SmartBoundingBox from(SmartBoundingBox bounds) {
      return fromCorners(bounds.getPosA(), bounds.getPosB());
   }

   public static SmartBoundingBox zero() {
      return new SmartBoundingBox(0, 0, 0, 0, 0, 0);
   }

   public void set(BoundingBox newBounds) {
      this.setPosA(this.cornerA.getPosFromBounds(newBounds));
      this.setPosB(this.cornerB.getPosFromBounds(newBounds));
   }

   public Vec3i getPosA() {
      return this.getCorner(this.cornerA);
   }

   public void setPosA(Vec3i a) {
      this.setCorner(this.cornerA, a);
   }

   public Vec3i getPosB() {
      return this.getCorner(this.cornerB);
   }

   public void setPosB(Vec3i b) {
      this.setCorner(this.cornerB, b);
   }

   public Vec3i getMin() {
      return this.getCorner(SmartBoundingBox.CornerType.NNN);
   }

   public void setMin(Vec3i min) {
      this.setCorner(SmartBoundingBox.CornerType.NNN, min);
   }

   public Vec3i getMax() {
      return this.getCorner(SmartBoundingBox.CornerType.PPP);
   }

   public void setMax(Vec3i max) {
      this.setCorner(SmartBoundingBox.CornerType.PPP, max);
   }

   public int getXSpan() {
      return this.getCorner(SmartBoundingBox.CornerType.PPP).getX() - this.getCorner(SmartBoundingBox.CornerType.NNN).getX() + 1;
   }

   public int getYSpan() {
      return this.getCorner(SmartBoundingBox.CornerType.PPP).getY() - this.getCorner(SmartBoundingBox.CornerType.NNN).getY() + 1;
   }

   public int getZSpan() {
      return this.getCorner(SmartBoundingBox.CornerType.PPP).getZ() - this.getCorner(SmartBoundingBox.CornerType.NNN).getZ() + 1;
   }

   public SmartBoundingBox.Corner[] allCorners() {
      SmartBoundingBox.CornerType[] cornerTypes = SmartBoundingBox.CornerType.values();
      SmartBoundingBox.Corner[] corners = new SmartBoundingBox.Corner[8];

      for (int i = 0; i < 8; i++) {
         corners[i] = new SmartBoundingBox.Corner(cornerTypes[i], cornerTypes[i].getPosFromBounds(this.ax, this.ay, this.az, this.bx, this.by, this.bz));
      }

      return corners;
   }

   public SmartBoundingBox.Corner[] selectionPointCorners() {
      return new SmartBoundingBox.Corner[]{
         new SmartBoundingBox.Corner(this.cornerA, this.cornerA.getPosFromBounds(this.ax, this.ay, this.az, this.bx, this.by, this.bz)),
         new SmartBoundingBox.Corner(this.cornerB, this.cornerB.getPosFromBounds(this.ax, this.ay, this.az, this.bx, this.by, this.bz))
      };
   }

   public Vec3i getCorner(SmartBoundingBox.CornerType corner) {
      return corner.getPosFromBounds(this.ax, this.ay, this.az, this.bx, this.by, this.bz);
   }

   public void setCorner(SmartBoundingBox.CornerType corner, Vec3i pos) {
      if (this.cornerA.xPositive == corner.xPositive) {
         this.ax = pos.getX();
      } else if (this.cornerB.xPositive == corner.xPositive) {
         this.bx = pos.getX();
      }

      if (this.cornerA.yPositive == corner.yPositive) {
         this.ay = pos.getY();
      } else if (this.cornerB.yPositive == corner.yPositive) {
         this.by = pos.getY();
      }

      if (this.cornerA.zPositive == corner.zPositive) {
         this.az = pos.getZ();
      } else if (this.cornerB.zPositive == corner.zPositive) {
         this.bz = pos.getZ();
      }
   }

   public void moveTo(Vec3i pos) {
      this.moveTo(SmartBoundingBox.CornerType.NNN, pos);
   }

   public void moveTo(SmartBoundingBox.CornerType corner, Vec3i pos) {
      Vec3i a = this.getCorner(corner);
      Vec3i b = this.getCorner(corner.opposite());
      int dx = b.getX() - a.getX();
      int dy = b.getY() - a.getY();
      int dz = b.getZ() - a.getZ();
      this.setCorner(corner, pos);
      this.setCorner(corner.opposite(), pos.offset(dx, dy, dz));
   }

   public BoundingBox bounds() {
      return BoundingBox.fromCorners(this.getPosA(), this.getPosB());
   }

   public AABB aabb() {
      return AABB.of(this.bounds());
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof SmartBoundingBox bb
         ? this.ax == bb.ax && this.ay == bb.ay && this.az == bb.az && this.bx == bb.bx && this.by == bb.by && this.bz == bb.bz
         : false;
   }

   public static class Corner {
      public final SmartBoundingBox.CornerType type;
      public int x;
      public int y;
      public int z;

      public Corner(SmartBoundingBox.CornerType type, int x, int y, int z) {
         this.type = type;
         this.x = x;
         this.y = y;
         this.z = z;
      }

      public Corner(SmartBoundingBox.CornerType type, Vec3i pos) {
         this(type, pos.getX(), pos.getY(), pos.getZ());
      }

      public Vec3i getPos() {
         return new Vec3i(this.x, this.y, this.z);
      }

      public Vec3i getPoint() {
         return new Vec3i(this.type.xPositive ? this.x + 1 : this.x, this.type.yPositive ? this.y + 1 : this.y, this.type.zPositive ? this.z + 1 : this.z);
      }
   }

   public static enum CornerType {
      NNN(false, false, false),
      NNP(false, false, true),
      NPN(false, true, false),
      NPP(false, true, true),
      PNN(true, false, false),
      PNP(true, false, true),
      PPN(true, true, false),
      PPP(true, true, true);

      final boolean xPositive;
      final boolean yPositive;
      final boolean zPositive;

      private CornerType(boolean xPositive, boolean yPositive, boolean zPositive) {
         this.xPositive = xPositive;
         this.yPositive = yPositive;
         this.zPositive = zPositive;
      }

      public static SmartBoundingBox.CornerType getFromPositions(int x, int y, int z, int otherX, int otherY, int otherZ) {
         int lowX = x < otherX ? 0 : 4;
         int lowY = y < otherY ? 0 : 2;
         int lowZ = z < otherZ ? 0 : 1;
         return values()[lowX | lowY | lowZ];
      }

      public SmartBoundingBox.CornerType opposite() {
         return values()[7 - this.ordinal()];
      }

      public Vec3i getPosFromBounds(BoundingBox bb) {
         return this.getPosFromBounds(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ());
      }

      public Vec3i getPosFromBounds(int ax, int ay, int az, int bx, int by, int bz) {
         return new Vec3i(this.getCoord(this.xPositive, ax, bx), this.getCoord(this.yPositive, ay, by), this.getCoord(this.zPositive, az, bz));
      }

      private int getCoord(boolean isPositive, int a, int b) {
         return isPositive ? Math.max(a, b) : Math.min(a, b);
      }
   }
}
