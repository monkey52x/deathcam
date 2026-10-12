package com.vlogf.deathcamera.mixin;

import com.vlogf.deathcamera.DeathCameraConfig;
import com.vlogf.deathcamera.DeathCameraState;
import com.vlogf.deathcamera.client.DeathCameraClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Убирает ванильную тряску и наклон камеры смерти, пока активна орбита:
 * они складываются с вращением и дают дёргание вверх-вниз.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * Ранний старт смерти: пакет смерти может открыть DeathScreen между тиком
     * и рендером — тогда кадр идёт с ванильной F1-камерой (~20мс вспышка).
     * Стартуем здесь же, до camera.setup ( detached посчитается уже правильным),
     * тик позже подхватит continue-часть. Дешёвый: пока не стартанули —
     * пара проверок, после — один флаг.
     */
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void deathcamera$earlyStartOnDeath(DeltaTracker tracker, CallbackInfo ci) {
        if (!DeathCameraState.orbiting) {
            Minecraft client = Minecraft.getInstance();
            if (DeathCameraClient.isDead(client)) {
                DeathCameraClient.ensureStarted(client);
            }
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void deathcamera$noShakeOnOrbit(PoseStack poses, float partialTick, CallbackInfo ci) {
        // Только когда камеру реально забираем (орбита/статика): иначе
        // (вращение выкл + force выкл + F1) остаётся честная ванилла,
        // работают только настройки экрана.
        if (isDeadOrOrbiting() && cameraTakenOver()) {
            ci.cancel();
        }
    }

    /**
     * FOV на время смерти — только когда камеру реально забираем.
     * Ванильный getFov при смерти сужает FOV (~7% за секунду, см. death-ветку
     * в GameRenderer): со включённым синематиком — свип к 110 (только F1),
     * без него — стабильный игровой FOV. Без перехвата (вращение выкл +
     * force выкл + F1) остаётся чистая ванилла.
     * Опции игры не трогаем, поэтому после возрождения FOV сам
     * становится как был — возвращать ничего не нужно.
     */
    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void deathcamera$cinematicFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (!cameraTakenOver()) {
            return;
        }
        if (isFirstPersonDeath()) {
            if (DeathCameraConfig.CINEMATIC_FOV) {
                // partialTick сглаживает ступени тиков — как у угла орбиты.
                cir.setReturnValue(DeathCameraState.smoothDeathFov(partialTick));
            } else {
                cir.setReturnValue(stableFov());
            }
        } else if (isDeadOrOrbiting()) {
            cir.setReturnValue(stableFov());
        }
    }

    /**
     * Забираем ли камеру: только когда включён вид от 3-го лица при смерти.
     * force=off — пиксель-в-пиксель ванилла (камера, FOV, наклон),
     * работают только настройки экрана и скрытие трупа.
     * От тика не зависит — действует с кадра 0.
     */
    private static boolean cameraTakenOver() {
        return DeathCameraConfig.FORCE_THIRD_PERSON;
    }

    /** Игровой FOV без ванильной death-модификации. */
    private static double stableFov() {
        try {
            return Minecraft.getInstance().options.fov().get().doubleValue();
        } catch (Exception ignored) {
            return 70.0;
        }
    }

    /**
     * Смерть от первого лица: идёт орбита, начатая из F1, либо игрок только
     * что умер в F1 (тик орбиты ещё не отработал).
     */
    private static boolean isFirstPersonDeath() {
        if (DeathCameraState.orbiting) {
            return DeathCameraState.prevCameraType == CameraType.FIRST_PERSON;
        }
        try {
            Minecraft client = Minecraft.getInstance();
            return client.player != null
                    && (client.player.isDeadOrDying() || client.player.getHealth() <= 0.0F
                            || client.screen instanceof DeathScreen)
                    && client.options.getCameraType() == CameraType.FIRST_PERSON;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Мёртв уже сейчас (даже если тик орбиты ещё не отработал) или орбита идёт. */
    private static boolean isDeadOrOrbiting() {
        if (DeathCameraState.orbiting) {
            return true;
        }
        try {
            Minecraft client = Minecraft.getInstance();
            return client.player != null
                    && (client.player.isDeadOrDying() || client.player.getHealth() <= 0.0F
                            || client.screen instanceof DeathScreen);
        } catch (Exception ignored) {
            return false;
        }
    }
}
