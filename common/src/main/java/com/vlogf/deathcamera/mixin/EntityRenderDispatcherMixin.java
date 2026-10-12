package com.vlogf.deathcamera.mixin;

import com.vlogf.deathcamera.DeathCameraConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Убирает тень скрытого трупа с первого кадра.
 * Тень рисует EntityRenderDispatcher.renderShadow отдельно от
 * LivingEntityRenderer.render, поэтому отмены тела недостаточно —
 * без этого тёмный эллипс висит до ванильного удаления (~1 сек).
 * Только свой труп (LocalPlayer), других игроков не касается.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void deathcamera$hideOwnCorpseShadow(Entity entity, double x, double y, double z,
            float rotationYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!DeathCameraConfig.HIDE_CORPSE) {
            return;
        }
        if (!(entity instanceof LocalPlayer player)) {
            return;
        }
        if (player.isDeadOrDying() || player.deathTime > 0) {
            ci.cancel();
        }
    }
}
