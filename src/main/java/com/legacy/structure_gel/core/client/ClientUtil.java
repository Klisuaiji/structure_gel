package com.legacy.structure_gel.core.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.legacy.structure_gel.core.util.Internal;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Internal
public class ClientUtil
{
	public static final Map<ResourceKey<? extends Registry<?>>, List<ResourceKey<?>>> REGISTRY_KEYS = new HashMap<>();

	public static Map<ResourceKey<TrimMaterial>, Integer> materialLight = new HashMap<>();

	public static int getMaterialBrightness(ArmorTrim trim)
	{
		var key = trim.material().unwrapKey();
		if (key.isPresent())
		{
			return materialLight.getOrDefault(key.get(), -1);
		}
		return -1;
	}

	/**
	 * Raytrace from the player's eyes outward
	 */
	public static BlockHitResult rayTrace(Level level, Player player)
	{
		Vec3 eyePos = player.getEyePosition(1.0F);
		float pi = (float) Math.PI;
		float radian = pi / 180F;
		float pitch = player.getXRot() * radian;
		float yaw = player.getYRot() * radian;
		float cosYaw = Mth.cos(-yaw - pi);
		float sinYaw = Mth.sin(-yaw - pi);
		float cosPitch = -Mth.cos(-pitch);
		float sinPitch = Mth.sin(-pitch);
		double playerReach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();
		Vec3 endPos = eyePos.add(sinYaw * cosPitch * playerReach, sinPitch * playerReach, cosYaw * cosPitch * playerReach);
		return level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
	}

	/**
	 * Renders a nameplate at the given buffer position.
	 */
	public static void renderName(Component displayName, BlockGetter level, BlockPos pos, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		Minecraft mc = Minecraft.getInstance();

		if (mc.player == null || mc.level == null)
			return;

		if (mc.getEntityRenderDispatcher().distanceToSqr(mc.player) <= 4096.0D)
		{
			poseStack.pushPose();
			Vec3 offset = null;
			Direction offsetDir = null;
			for (Direction dir : Direction.orderedByNearest(Minecraft.getInstance().player))
			{
				BlockPos normal = new BlockPos(dir.getOpposite().getNormal());
				if (level.getBlockState(pos.offset(normal)).isAir())
				{
					offset = Vec3.atCenterOf(normal);
					offsetDir = dir;
					break;
				}
			}
			if (offset == null || (level.getBlockState(pos.above()).isAir() && offsetDir.getAxis().isHorizontal()))
				offset = BlockPos.ZERO.above().getCenter();

			poseStack.translate(offset.x(), offset.y(), offset.z());
			poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
			poseStack.scale(-0.025F, -0.025F, 0.025F);
			Matrix4f lastMatrix = poseStack.last().pose();
			Font fontRenderer = mc.font;
			float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
			int alpha = (int) (backgroundOpacity * 255.0F) << 24;
			float centerString = (float) (-fontRenderer.width(displayName) / 2);
			fontRenderer.drawInBatch(displayName, centerString, 0.0F, 553648127, false, lastMatrix, buffer, Font.DisplayMode.SEE_THROUGH, alpha, packedLight);
			fontRenderer.drawInBatch(displayName, centerString, 0.0F, -1, false, lastMatrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
			poseStack.popPose();
		}
	}
}
