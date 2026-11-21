package com.legacy.structure_gel.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec2;

public class DistanceCommand
{
	private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType(o -> Component.translatable("commands.locate.structure.invalid", o));
	private static final DynamicCommandExceptionType ERROR_BIOME_INVALID = new DynamicCommandExceptionType(o -> Component.translatable("commands.locate.biome.invalid", o));

	public static LiteralArgumentBuilder<CommandSourceStack> get()
	{
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("distance").requires(source -> source.hasPermission(2));

		// structure_gel distance <structure/tag> <sample_size> <teleport_range=max level size by default>

		//@formatter:off
		
		command
			.then(Commands.literal("structure")
				.then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
					.executes(c -> structure(c, ResourceOrTagKeyArgument.getResourceOrTagKey(c, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID), 25))
					.then(Commands.argument("sample_size", IntegerArgumentType.integer(1))
						.executes(c -> structure(c, ResourceOrTagKeyArgument.getResourceOrTagKey(c, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID), c.getArgument("sample_size", Integer.class))))));
		command
			.then(Commands.literal("biome")
				.then(Commands.argument("biome", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.BIOME))
					.executes(c -> biome(c, ResourceOrTagKeyArgument.getResourceOrTagKey(c, "biome", Registries.BIOME, ERROR_BIOME_INVALID), 25))
					.then(Commands.argument("sample_size", IntegerArgumentType.integer(1))
						.executes(c -> biome(c, ResourceOrTagKeyArgument.getResourceOrTagKey(c, "biome", Registries.BIOME, ERROR_BIOME_INVALID), c.getArgument("sample_size", Integer.class))))));
		command
			.then(Commands.literal("vanilla_stats")
				.executes(c -> printVanillaStats(c)));
		//@formatter:on

		return command;
	}

	private static <T> Optional<HolderSet<T>> getHolders(ResourceOrTagKeyArgument.Result<T> result, Registry<T> registry)
	{		
		return result.unwrap().map(
				key -> registry.getHolder(key).map(holder -> (HolderSet<T>) HolderSet.direct(holder)),
				tagKey -> registry.getTag(tagKey).map(tag -> (HolderSet<T>) tag));
	}

	private static int structure(CommandContext<CommandSourceStack> context, ResourceOrTagKeyArgument.Result<Structure> result, int sampleSize) throws CommandSyntaxException
	{
		return distance(context, result, sampleSize, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID, (level, pos, holderSet) -> Optional.ofNullable(level.getChunkSource().getGenerator().findNearestMapStructure(level, holderSet, pos, 100, false)));
	}

	private static int biome(CommandContext<CommandSourceStack> context, ResourceOrTagKeyArgument.Result<Biome> result, int sampleSize) throws CommandSyntaxException
	{
		return distance(context, result, sampleSize, Registries.BIOME, ERROR_BIOME_INVALID, (level, pos, holderSet) -> Optional.ofNullable(level.findClosestBiome3d(holderSet::contains, pos, 6400, 32, 64)));
	}

	private static <T> int distance(CommandContext<CommandSourceStack> context, ResourceOrTagKeyArgument.Result<T> result, int sampleSize, ResourceKey<Registry<T>> registry, DynamicCommandExceptionType error, Search<T> search) throws CommandSyntaxException
	{
		String searchFor = result.asPrintable();
		var source = context.getSource();
		ServerLevel level = source.getLevel();
		int r = (int) (level.getServer().getAbsoluteMaxWorldSize() * 0.9);
		int y = (int) source.getPosition().y;
		HolderSet<T> holderSet = getHolders(result, source.registryAccess().registryOrThrow(registry)).orElseThrow(() -> error.create(searchFor));
		if (holderSet.size() < 1)
		{
			source.sendFailure(Component.literal(searchFor + " contains no values or does not exist."));
			return 0;
		}
		RandomSource rand = level.getRandom();
		int totalDistance = 0;
		int maxDistance = 0;
		int actuallyFound = 0;
		List<Pair<Integer, BlockPos>> failedSearches = new ArrayList<>();
		source.sendSuccess(() -> Component.literal(String.format("Started search for %s with a sample size of %d. This may take a while.", searchFor, sampleSize)), true);
		for (int i = 0; i < sampleSize; i++)
		{
			BlockPos searchPos = new BlockPos(rand.nextInt(r * 2) - r, y, rand.nextInt(r * 2) - r);
			Integer loggedIndex = i + 1;
			source.sendSuccess(() -> Component.literal(String.format("Search %d/%d at (%d, %d, %d)", loggedIndex, sampleSize, searchPos.getX(), searchPos.getY(), searchPos.getZ())).withStyle(ChatFormatting.GRAY), false);
			Optional<Pair<BlockPos, Holder<T>>> pair = search.search(level, searchPos, holderSet);
			if (pair.isPresent())
			{
				actuallyFound++;
				BlockPos p = pair.get().getFirst();
				int distance = (int) Math.sqrt(new Vec2(searchPos.getX(), searchPos.getZ()).distanceToSqr(new Vec2(p.getX(), p.getZ())));
				totalDistance += distance;
				if (distance > maxDistance)
					maxDistance = distance;
			}
			else
			{
				failedSearches.add(Pair.of(i + 1, searchPos));
			}
		}

		int averageDist = actuallyFound > 0 ? totalDistance / actuallyFound : 0;
		source.sendSuccess(() -> Component.literal("Results for " + searchFor).withStyle(ChatFormatting.GREEN), false);
		source.sendSuccess(() -> Component.literal("- Average Distance: ").append(Component.literal("" + averageDist).withStyle(ChatFormatting.GRAY)), false);
		Integer loggedMaxDistance = maxDistance;
		source.sendSuccess(() -> Component.literal("- Max Distance: ").append(Component.literal("" + loggedMaxDistance).withStyle(ChatFormatting.GRAY)), false);
		Integer loggedActuallyFound = actuallyFound;
		source.sendSuccess(() -> Component.literal("- Found: ").append(Component.literal(loggedActuallyFound + "/" + sampleSize).withStyle(ChatFormatting.GRAY)), false);
		if (failedSearches.size() > 0)
		{
			source.sendSuccess(() -> Component.literal("Failed searches").withStyle(ChatFormatting.RED), false);
			for (var fail : failedSearches)
			{
				BlockPos pos = fail.getSecond();
				source.sendSuccess(() -> Component.literal("- Search: ").append(Component.literal("" + fail.getFirst()).withStyle(ChatFormatting.GRAY)).append(Component.literal(", Pos: ")).append(Component.literal(String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())).withStyle(ChatFormatting.GRAY)), false);
			}
		}
		Player player = source.getPlayer();
		if (player != null)
			player.playNotifySound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.MASTER, 1.0F, 1.5F);
		return averageDist;
	}

	private static int printVanillaStats(CommandContext<CommandSourceStack> context)
	{
		var source = context.getSource();

		Map<String, Integer> distances = new TreeMap<>();
		distances.put("Village", 720);
		distances.put("Desert Pyramid", 3500);
		distances.put("Jungle Pyramid", 1937);
		distances.put("Ocean Monument", 1302);
		distances.put("Woodland Mansion", 4492);
		distances.put("Pillager Outpost", 1362);
		distances.put("Ancient City", 1521);
		distances.put("End City", 1164);
		distances.put("Nether Fortress", 418);
		distances.put("Bastion Remnant", 416);

		source.sendSuccess(() -> Component.literal("Average distances of vanilla structures (Recorded in 1.19.2)").withStyle(ChatFormatting.GREEN), false);
		for (var entry : distances.entrySet())
			source.sendSuccess(() -> Component.literal("- " + entry.getKey() + ": ").append(Component.literal("" + entry.getValue()).withStyle(ChatFormatting.GRAY)), false);
		return 1;
	}

	@FunctionalInterface
	private static interface Search<T>
	{
		Optional<Pair<BlockPos, Holder<T>>> search(ServerLevel level, BlockPos pos, HolderSet<T> holderSet);
	}
}
