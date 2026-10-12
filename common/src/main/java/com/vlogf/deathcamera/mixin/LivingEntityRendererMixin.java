package com.vlogf.deathcamera.mixin;

import com.vlogf.deathcamera.DeathCameraConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Скрывает свой труп с первого кадра, если включён hideCorpse.
 * Рендер тела отменяется сразу — мигания нет.
 * Других игроков не касается, мод на сервере не нужен.
 * После возрождения условие спадает само.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void deathcamera$hideOwnCorpse(LivingEntity entity, float entityYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!DeathCameraConfig.HIDE_CORPSE) {
            return;
        }
        if (!(entity instanceof LocalPlayer)) {
            return;
        }
        if (entity.isDeadOrDying() || entity.deathTime > 0) {
            ci.cancel();
        }
    }
}
