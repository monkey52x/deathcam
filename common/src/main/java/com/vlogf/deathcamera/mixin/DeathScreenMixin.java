package com.vlogf.deathcamera.mixin;

import com.vlogf.deathcamera.DeathCameraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Shadow
    @Final
    private List<Button> exitButtons;

    /**
     * Не ставит одиночную игру на паузу на экране смерти,
     * поэтому чанки / мобы / частицы продолжают обновляться.
     */
    @Inject(method = "isPauseScreen", at = @At("HEAD"), cancellable = true)
    private void deathcamera$disablePause(CallbackInfoReturnable<Boolean> cir) {
        if (DeathCameraConfig.KEEP_CHUNKS_UPDATING) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Убирает красный градиент на экране смерти — мир видно чисто.
     */
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void deathcamera$skipRedOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (DeathCameraConfig.REMOVE_RED_SCREEN) {
            ci.cancel();
        }
    }

    /**
     * Положение кнопок: меньше и рядом друг с другом (слева/справа от центра),
     * подняты над полосой опыта. Отдельный тумблер от компактных текстов.
     * init вызывается и при ресайзе — раскладка применяется заново сама.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void deathcamera$compactButtons(CallbackInfo ci) {
        if (!DeathCameraConfig.COMPACT_DEATH_SCREEN || this.exitButtons.size() < 2) {
            return;
        }
        // Порядок в списке детерминирован: сначала «Возродиться», потом «В меню».
        Button respawnButton = this.exitButtons.get(0);
        Button titleButton = this.exitButtons.get(1);
        // Размер экрана = размер окна (Screen.init берёт их оттуда же).
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        // Низ кнопок выше хотбара и опыта.
        int y = screenHeight - DeathCameraConfig.BUTTONS_BOTTOM_MARGIN;
        respawnButton.setWidth(150);
        respawnButton.setX(centerX - 155);
        respawnButton.setY(y);
        titleButton.setWidth(150);
        titleButton.setX(centerX + 5);
        titleButton.setY(y);
    }

    @Unique
    private boolean deathcamera$textsShifted;

    /**
     * Сдвигаем тексты (заголовок, причина, счёт) выше.
     * Инжект после super.render: фон и кнопки уже нарисованы и не сдвигаются.
     */
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
                    shift = At.Shift.AFTER))
    private void deathcamera$shiftTexts(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.deathcamera$textsShifted = false;
        if (!DeathCameraConfig.COMPACT_DEATH_SCREEN) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, -Math.max(0, DeathCameraConfig.TEXTS_SHIFT_UP), 0.0F);
        this.deathcamera$textsShifted = true;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void deathcamera$unshiftTexts(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.deathcamera$textsShifted) {
            graphics.pose().popPose();
            this.deathcamera$textsShifted = false;
        }
    }
}
