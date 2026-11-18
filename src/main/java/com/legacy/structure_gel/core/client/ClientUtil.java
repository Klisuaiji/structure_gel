package com.legacy.structure_gel.core.client;

import com.legacy.structure_gel.core.util.Internal;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
@Internal
public class ClientUtil {
   public static final Map<ResourceKey<? extends Registry<?>>, List<ResourceKey<?>>> REGISTRY_KEYS = new HashMap<>();
   public static Map<ResourceKey<TrimMaterial>, Map<ArmorMaterial, String>> colorPaletteOverrides = new HashMap<>();
   public static Map<ResourceKey<TrimMaterial>, Integer> materialLight = new HashMap<>();

   @Nullable
   public static String getCustomColorPaletteSuffix(ArmorTrim trim, ArmorMaterial armorMaterial) {
      Optional<ResourceKey<TrimMaterial>> key = trim.material().unwrapKey();
      if (key.isPresent()) {
         Map<ArmorMaterial, String> overrides = colorPaletteOverrides.get(key.get());
         if (overrides != null) {
            return overrides.get(armorMaterial);
         }
      }

      return null;
   }

   public static int getMaterialBrightness(ArmorTrim trim) {
      Optional<ResourceKey<TrimMaterial>> key = trim.material().unwrapKey();
      return key.isPresent() ? materialLight.getOrDefault(key.get(), -1) : -1;
   }

   public static BlockHitResult rayTrace(Level level, Player player) {
      Vec3 eyePos = player.getEyePosition(1.0F);
      float pi = (float) Math.PI;
      float radian = pi / 180.0F;
      float pitch = player.getXRot() * radian;
      float yaw = player.getYRot() * radian;
      float cosYaw = Mth.cos(-yaw - pi);
      float sinYaw = Mth.sin(-yaw - pi);
      float cosPitch = -Mth.cos(-pitch);
      float sinPitch = Mth.sin(-pitch);
      double playerReach = player.getAttribute((Attribute)NeoForgeMod.BLOCK_REACH.value()).getValue();
      Vec3 endPos = eyePos.add(sinYaw * cosPitch * playerReach, sinPitch * playerReach, cosYaw * cosPitch * playerReach);
      return level.clip(new ClipContext(eyePos, endPos, Block.COLLIDER, Fluid.NONE, player));
   }

   public static void renderName(Component displayName, BlockGetter level, BlockPos pos, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (mc.getEntityRenderDispatcher().distanceToSqr(mc.player) <= 4096.0) {
            poseStack.pushPose();
            Vec3 offset = null;
            Direction offsetDir = null;

            for (Direction dir : Direction.orderedByNearest(Minecraft.getInstance().player)) {
               BlockPos normal = new BlockPos(dir.getOpposite().getNormal());
               if (level.getBlockState(pos.offset(normal)).isAir()) {
                  offset = Vec3.atCenterOf(normal);
                  offsetDir = dir;
                  break;
               }
            }

            if (offset == null || level.getBlockState(pos.above()).isAir() && offsetDir.getAxis().isHorizontal()) {
               offset = BlockPos.ZERO.above().getCenter();
            }

            poseStack.translate(offset.x(), offset.y(), offset.z());
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(-0.025F, -0.025F, 0.025F);
            Matrix4f lastMatrix = poseStack.last().pose();
            Font fontRenderer = mc.font;
            float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            int alpha = (int)(backgroundOpacity * 255.0F) << 24;
            float centerString = -fontRenderer.width(displayName) / 2;
            fontRenderer.drawInBatch(displayName, centerString, 0.0F, 553648127, false, lastMatrix, buffer, DisplayMode.SEE_THROUGH, alpha, packedLight);
            fontRenderer.drawInBatch(displayName, centerString, 0.0F, -1, false, lastMatrix, buffer, DisplayMode.NORMAL, 0, packedLight);
            poseStack.popPose();
         }
      }
   }
}
