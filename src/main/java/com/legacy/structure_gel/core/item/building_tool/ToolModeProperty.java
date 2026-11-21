package com.legacy.structure_gel.core.item.building_tool;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.legacy.structure_gel.core.StructureGelMod;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ToolModeProperty<T>
{
	public static final String RANGE_KEY = "info.structure_gel.building_tool.property_range";
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");

	public static final ToolModeProperty.NumberProp<Integer> REACH_DISTANCE = intProperty("reach_distance", -6, 100, 0);
	public static final ToolModeProperty.SelectionProp<BooleanProperty> CAUSE_BLOCK_UPDATES = selectionProperty("cause_block_updates", BooleanProperty.FALSE, BooleanProperty.VALUES);
	public static final ToolModeProperty.NumberProp<Integer> X_SIZE = intProperty("x_size", 1, Integer.MAX_VALUE, 0);
	public static final ToolModeProperty.NumberProp<Integer> Y_SIZE = intProperty("y_size", 1, Integer.MAX_VALUE, 0);
	public static final ToolModeProperty.NumberProp<Integer> Z_SIZE = intProperty("z_size", 1, Integer.MAX_VALUE, 0);

	public static final ToolModeProperty.NumberProp<Integer> WEIGHT = intProperty("weight", 1, 2000, 1);
	public static final ToolModeProperty.NumberProp<Double> INTEGRITY = doubleProperty("integrity", 0.0, 1.0, 1.0);
	public static final ToolModeProperty.NumberProp<Integer> RADIUS = intProperty("radius", 0, 100, 4);
	public static final ToolModeProperty.NumberProp<Integer> MEDIUM_RADIUS = intProperty("radius", 0, 100, 35);
	public static final ToolModeProperty.NumberProp<Integer> LARGE_RADIUS = intProperty("radius", 0, 200, 80);
	public static final ToolModeProperty.NumberProp<Integer> MOVE_DISTANCE = intProperty("move_distance", 1, 100, 1);
	public static final ToolModeProperty.SelectionProp<BooleanProperty> COPY_STATES = selectionProperty("copy_states", BooleanProperty.TRUE, BooleanProperty.VALUES);
	public static final ToolModeProperty.SelectionProp<BooleanProperty> EXTEND_DOWN = selectionProperty("extend_down", BooleanProperty.TRUE, BooleanProperty.VALUES);
	public static final ToolModeProperty.SelectionProp<Replace> REPLACE = selectionProperty("replace", Replace.ALL, Replace.values());
	public static final ToolModeProperty.SelectionProp<Replace> REPLACE_NOT_CLICKED = selectionProperty("replace", Replace.ALL, Replace.ALL, Replace.AIR_AND_LIQUID, Replace.AIR, Replace.REPLACEABLE);
	public static final ToolModeProperty.SelectionProp<Replace> REPLACE_NOT_AIR_CLICKED = selectionProperty("replace", Replace.AIR_AND_LIQUID, Replace.AIR_AND_LIQUID, Replace.AIR, Replace.REPLACEABLE);
	public static final ToolModeProperty.SelectionProp<SGRotation> ROTATION = selectionProperty("rotation", SGRotation.R_0, SGRotation.values());
	public static final ToolModeProperty.SelectionProp<SGMirror> MIRROR = selectionProperty("mirror", SGMirror.NONE, SGMirror.values());
	public static final ToolModeProperty.SelectionProp<BooleanProperty> CUT_TRUE = selectionProperty("cut", BooleanProperty.TRUE, BooleanProperty.VALUES);
	public static final ToolModeProperty.SelectionProp<BooleanProperty> CUT_FALSE = selectionProperty("cut", BooleanProperty.FALSE, BooleanProperty.VALUES);
	public static final ToolModeProperty.SelectionProp<Shape> SHAPE = selectionProperty("shape", Shape.SPHERE, Shape.values());
	public static final ToolModeProperty.SelectionProp<Shape> FILL_SHAPE = selectionProperty("shape", Shape.CUBE, Shape.values());
	public static final ToolModeProperty.SelectionProp<BooleanProperty> FUZZY_TRUE = selectionProperty("fuzzy", BooleanProperty.TRUE, BooleanProperty.VALUES);
	public static final ToolModeProperty.SelectionProp<BooleanProperty> INCLUDE_ENTITIES = selectionProperty("include_entities", BooleanProperty.FALSE, BooleanProperty.VALUES);
	
	private final String key;
	protected final Function<String, T> readFunc;
	protected final Function<T, String> writeFunc;
	protected final Predicate<T> valueTest;
	protected final Supplier<T> defaultVal;
	private final Component nameComponent;
	private final String descKey;

	public ToolModeProperty(String key, Function<String, T> readFunc, Function<T, String> writeFunc, Predicate<T> valueTest, Supplier<T> defaultVal)
	{
		this.key = key;
		this.readFunc = readFunc;
		this.writeFunc = writeFunc;
		this.valueTest = valueTest;
		this.defaultVal = defaultVal;
		this.nameComponent = Component.translatable("info.structure_gel.building_tool.property." + key);
		this.descKey = "info.structure_gel.building_tool.property." + key + ".description";
	}

	public static ToolModeProperty.NumberProp<Double> doubleProperty(String key, double min, double max, double defaultVal)
	{
		return new ToolModeProperty.NumberProp<>(key, Double.class, Double::valueOf, DOUBLE_FORMAT::format, min, max, () -> defaultVal);
	}

	public static ToolModeProperty.NumberProp<Integer> intProperty(String key, int min, int max, int defaultVal)
	{
		return new ToolModeProperty.NumberProp<>(key, Integer.class, Integer::valueOf, i -> Integer.toString(i), min, max, () -> defaultVal);
	}

	@SafeVarargs
	public static <E extends StringRepresentable> ToolModeProperty.SelectionProp<E> selectionProperty(String key, E defaultValue, E... values)
	{
		if (values.length < 1)
			throw new IllegalArgumentException("Cannot create a ToolModeProperty for an enum without any values. Key = " + key);
		Map<String, E> nameMap = new HashMap<>();
		for (E e : values)
			nameMap.put(e.getSerializedName(), e);
		return new ToolModeProperty.SelectionProp<>(key, nameMap::get, StringRepresentable::getSerializedName, nameMap::containsValue, () -> defaultValue, List.of(values));
	}

	public String getKey()
	{
		return this.key;
	}

	public T getDefaultValue()
	{
		return this.defaultVal.get();
	}

	public T read(String input)
	{
		try
		{
			T val = this.readFunc.apply(input);
			if (val != null && this.isValid(val))
				return val;
		}
		catch (Exception e)
		{
			// Silent catch
		}
		return this.defaultVal.get();
	}

	public boolean canRead(String input)
	{
		try
		{
			T val = this.readFunc.apply(input);
			return val != null && this.isValid(val);
		}
		catch (Exception e)
		{
			// Silent catch
		}
		return false;
	}

	public String write(T input)
	{
		return this.writeFunc.apply(input);
	}

	public boolean isValid(T value)
	{
		return this.valueTest.test(value);
	}

	public Component getNameComponent()
	{
		return this.nameComponent;
	}

	public String getDescKey()
	{
		return this.descKey;
	}

	public Component getValueComponent(@Nullable T value)
	{
		return Component.literal(value == null ? "null" : value.toString());
	}

	public static class NumberProp<T extends Number> extends ToolModeProperty<T>
	{
		private final T min, max;
		private final Class<T> numClass;

		public NumberProp(String key, Class<T> numClass, Function<String, T> readFunc, Function<T, String> writeFunc, T min, T max, Supplier<T> defaultVal)
		{
			super(key, readFunc, writeFunc, n -> n.doubleValue() >= min.doubleValue() && n.doubleValue() <= max.doubleValue(), defaultVal);
			this.min = min;
			this.max = max;
			this.numClass = numClass;
		}

		public T min()
		{
			return this.min;
		}

		public T max()
		{
			return this.max;
		}

		public T clamp(T val)
		{
			if (val.doubleValue() < this.min.doubleValue())
				return this.min;
			else if (val.doubleValue() > this.max.doubleValue())
				return this.max;
			return val;
		}

		@Override
		public T read(String input)
		{
			try
			{
				T ret = this.readFunc.apply(input);
				if (ret != null)
				{
					double d = ret.doubleValue();
					if (d < this.min.doubleValue())
						return this.min;
					else if (d > this.max.doubleValue())
						return this.max;
					return ret;
				}
			}
			catch (Exception e)
			{
				// Silent catch
			}
			return this.defaultVal.get();
		}

		public Class<T> getNumberClass()
		{
			return this.numClass;
		}
	}

	public static class SelectionProp<T extends StringRepresentable> extends ToolModeProperty<T>
	{
		private final List<T> allValues;
		private final Function<T, Component> valueComponents = Util.memoize(t -> Component.translatable("info.structure_gel.building_tool.property." + this.getKey() + ".value." + t.getSerializedName()));

		private SelectionProp(String key, Function<String, T> readFunc, Function<T, String> writeFunc, Predicate<T> valueTest, Supplier<T> defaultVal, List<T> allValues)
		{
			super(key, readFunc, writeFunc, valueTest, defaultVal);
			this.allValues = List.copyOf(allValues);
		}

		public List<T> getAllValues()
		{
			return this.allValues;
		}

		@Override
		public Component getValueComponent(@Nullable T value)
		{
			if (value == null)
				return Component.literal("null");
			return this.valueComponents.apply(value);
		}
	}

	public static enum Replace implements StringRepresentable
	{
		ALL("all", true)
		{
			@Override
			public boolean shouldReplace(Level level, BlockState clickedState, BlockPos pos)
			{
				return true;
			}
		},
		AIR("air", false)
		{
			@Override
			public boolean shouldReplace(Level level, BlockState clickedState, BlockPos pos)
			{
				return level.getBlockState(pos).isAir();
			}
		},
		CLICKED_BLOCK("clicked_block", false)
		{
			@Override
			public boolean shouldReplace(Level level, BlockState clickedState, BlockPos pos)
			{
				return !clickedState.isAir() && level.getBlockState(pos).is(clickedState.getBlock());
			}
		},
		AIR_AND_LIQUID("air_and_liquid", false)
		{
			@Override
			public boolean shouldReplace(Level level, BlockState clickedState, BlockPos pos)
			{
				BlockState state = level.getBlockState(pos);
				return state.isAir() || state.liquid();
			}
		},
		REPLACEABLE("replaceable", false)
		{
			@Override
			public boolean shouldReplace(Level level, BlockState clickedState, BlockPos pos)
			{
				BlockState state = level.getBlockState(pos);
				return state.is(BlockTags.REPLACEABLE) || state.canBeReplaced();
			}
		};

		private final String key;
		/**
		 * When true, uses the "Placed" message. When false, uses the "Replaced" message
		 */
		public final boolean usePlaceMessage;

		Replace(String key, boolean usePlaceMessage)
		{
			this.key = key;
			this.usePlaceMessage = usePlaceMessage;
		}

		/**
		 * 
		 * @param level
		 * @param clickedState
		 *            The block that was clicked
		 * @param pos
		 *            The pos to palce a block
		 * @return True if we should place a block at pos
		 */
		public abstract boolean shouldReplace(Level level, BlockState clickedState, BlockPos pos);

		@Override
		public String getSerializedName()
		{
			return this.key;
		}
	}

	public static enum SGRotation implements StringRepresentable
	{
		R_0("r_0", r -> Rotation.NONE),
		R_90("r_90", r -> Rotation.CLOCKWISE_90),
		R_180("r_180", r -> Rotation.CLOCKWISE_180),
		R_270("r_270", r -> Rotation.COUNTERCLOCKWISE_90),
		RANDOM("random", r -> Util.getRandom(Rotation.values(), r));

		private final String key;
		private final Function<RandomSource, Rotation> vanilla;

		SGRotation(String key, Function<RandomSource, Rotation> vanilla)
		{
			this.key = key;
			this.vanilla = vanilla;
		}

		@Override
		public String getSerializedName()
		{
			return this.key;
		}

		public Rotation toVanilla(RandomSource rand)
		{
			return this.vanilla.apply(rand);
		}
	}

	public static enum SGMirror implements StringRepresentable
	{
		NONE("none", r -> Mirror.NONE),
		Z("z", r -> Mirror.FRONT_BACK),
		X("x", r -> Mirror.LEFT_RIGHT),
		RANDOM("random", r -> Util.getRandom(Mirror.values(), r));

		private final String key;
		private final Function<RandomSource, Mirror> vanilla;

		SGMirror(String key, Function<RandomSource, Mirror> vanilla)
		{
			this.key = key;
			this.vanilla = vanilla;
		}

		@Override
		public String getSerializedName()
		{
			return this.key;
		}

		public Mirror toVanilla(RandomSource rand)
		{
			return this.vanilla.apply(rand);
		}
	}

	public static enum Shape implements IPanelSelection
	{
		CUBE("cube")
		{
			@Override
			public boolean isInside(Vec3 vec, double dx, double dy, double dz)
			{
				return true;
			}
		},
		HOLLOW_CUBE("hollow_cube")
		{
			@Override
			public boolean isInside(Vec3 vec, double dx, double dy, double dz)
			{
				// Adjust for even number widths (fill tool)
				double x = vec.x();
				double y = vec.y();
				double z = vec.z();
				if (dx - (int) dx > 0 && x < 0)
					dx--;
				if (dy - (int) dy > 0 && y < 0)
					dy--;
				if (dz - (int) dz > 0 && z < 0)
					dz--;

				return Math.abs(x) >= dx || Math.abs(y) >= dy || Math.abs(z) >= dz;
			}
		},
		CUBE_FRAME("cube_frame")
		{
			@Override
			public boolean isInside(Vec3 vec, double dx, double dy, double dz)
			{
				// Adjust for even number widths (fill tool)
				double x = vec.x();
				double y = vec.y();
				double z = vec.z();
				if (dx - (int) dx > 0 && x < 0)
					dx--;
				if (dy - (int) dy > 0 && y < 0)
					dy--;
				if (dz - (int) dz > 0 && z < 0)
					dz--;

				int walls = 0;
				if (Math.abs(x) >= dx)
					walls++;
				if (Math.abs(y) >= dy)
					walls++;
				if (Math.abs(z) >= dz)
					walls++;
				return walls >= 2;
			}
		},
		SPHERE("sphere")
		{
			@Override
			public boolean isInside(Vec3 vec, double dx, double dy, double dz)
			{
				double x = vec.x();
				double y = vec.y();
				double z = vec.z();
				// Adjust for even number widths (fill tool)
				if (dx - (int) dx > 0 && x > 0)
					x--;
				if (dy - (int) dy > 0 && y > 0)
					y--;
				if (dz - (int) dz > 0 && z > 0)
					z--;
				double r = 0.5;
				return circularDist(x, dx + r) + circularDist(y, dy + r) + circularDist(z, dz + r) < 1.0;
			}
		},
		DIAMOND("diamond")
		{
			@Override
			public boolean isInside(Vec3 vec, double dx, double dy, double dz)
			{
				double x = vec.x();
				double y = vec.y();
				double z = vec.z();
				// Adjust for even number widths (fill tool)
				if (dx - (int) dx > 0 && x > 0)
					x--;
				if (dy - (int) dy > 0 && y > 0)
					y--;
				if (dz - (int) dz > 0 && z > 0)
					z--;

				y = Math.abs(y);
				double yForX = dy / -dx * Math.abs(x) + dy;
				if (y <= yForX)
				{
					double yForZ = dy / -dz * Math.abs(z) + yForX;
					return y <= yForZ;
				}
				return false;
			}
		},
		CYLINDER("cylinder")
		{
			@Override
			public boolean isInside(Vec3 vec, double dx, double dy, double dz)
			{
				return SPHERE.isInside(new Vec3(vec.x(), 0, vec.z()), dx, dy, dz);
			}
		},
		PYRAMID("pyramid")
		{
			@Override
			public boolean isInside(Vec3 vec, double dx, double dy, double dz)
			{
				vec = vec.add(0, -dy, 0);
				dy = dy * 2;

				double x = vec.x();
				double y = vec.y();
				double z = vec.z();
				// Adjust for even number widths (fill tool)
				if (dx - (int) dx > 0 && x > 0)
					x--;
				if (dy - (int) dy > 0 && y > 0)
					y--;
				if (dz - (int) dz > 0 && z > 0)
					z--;

				y = Math.abs(y);
				double yForX = dy / -dx * Math.abs(x) + dy;
				if (y <= yForX)
				{
					double yForZ = dy / -dz * Math.abs(z) + dy;
					return y <= yForZ;
				}
				return false;
			}
		},
		CONE("cone")
		{
			@Override
			public boolean isInside(Vec3 vec, double dx, double dy, double dz)
			{
				vec = vec.add(0, dy, 0);
				dy = dy * 2;

				double x = vec.x();
				double y = vec.y();
				double z = vec.z();
				// Adjust for even number widths (fill tool)
				if (dx - (int) dx > 0 && x > 0)
					x--;
				if (dy - (int) dy > 0 && y > 0)
					y--;
				if (dz - (int) dz > 0 && z > 0)
					z--;

				double scale = y / dy;
				double r = 0.5;
				return circularDist(x, dx * scale + r) + circularDist(z, dz * scale + r) < 1.0;
			}
		};

		private final String key;

		Shape(String key)
		{
			this.key = key;
		}

		@Override
		public ResourceLocation getSpriteFolder()
		{
			return StructureGelMod.locate("building_tool/shapes");
		}

		@Override
		public String getSerializedName()
		{
			return this.key;
		}

		/**
		 * Checks if a position is in the shape, given an origin of 0
		 * 
		 * @param vec
		 *            The position to check
		 * @param dx
		 *            x radius
		 * @param dy
		 *            y radius
		 * @param dz
		 *            z radius
		 * @return
		 */
		public abstract boolean isInside(Vec3 vec, double dx, double dy, double dz);

		/**
		 * Checks if a position is in the shape, given an origin of 0
		 * 
		 * @param vec
		 *            The position to check
		 * @param radius
		 * @return
		 */
		public boolean isInside(Vec3i vec, double radius)
		{
			return this.isInside(Vec3.atLowerCornerOf(vec), radius, radius, radius);
		}

		/**
		 * 
		 * @param pos
		 * @param maxDist
		 * @return A value from 0 - 0.33333. Anything less than 0.33333 is inside.
		 */
		private static double circularDist(double pos, double maxDist)
		{
			return (pos * pos) / (maxDist * maxDist);
		}
	}

	public static record BooleanProperty(boolean value, String key) implements StringRepresentable
	{

		private static final BooleanProperty TRUE = new BooleanProperty(true, "true");
		private static final BooleanProperty FALSE = new BooleanProperty(false, "false");
		private static final BooleanProperty[] VALUES = { TRUE, FALSE };

		public static BooleanProperty from(boolean bool)
		{
			return bool ? TRUE : FALSE;
		}
		
		@Override
		public String getSerializedName()
		{
			return this.key;
		}
	}
	
	public static interface IPanelSelection extends StringRepresentable
	{		
		ResourceLocation getSpriteFolder();
	}
}
