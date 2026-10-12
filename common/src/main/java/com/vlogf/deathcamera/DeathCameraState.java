package com.vlogf.deathcamera;

import net.minecraft.client.CameraType;

/**
 * Общее состояние орбиты. Используется и тиком, и миксином камеры.
 * Только числа — без классов загрузчика, поэтому лежит в common.
 */
public final class DeathCameraState {
    private DeathCameraState() {}

    public static boolean orbiting = false;
    /** Точка смерти отдельными числами, чтобы не таскать Vec3 между маппингами. */
    public static double deathX;
    public static double deathY;
    public static double deathZ;
    public static boolean hasDeathPos = false;
    /** Угол на начало текущего тика (для интерполяции между кадрами). */
    public static double prevAngleDeg = 0.0;
    /** Угол на конец текущего тика. */
    public static double angleDeg = 0.0;
    /** Текущая скорость (плавно разгоняется до значения из конфига). */
    public static double currentSpeedDeg = 0.0;
    public static CameraType prevCameraType = null;
    public static boolean forcedPerspective = false;
    /** Сглаженная дистанция камеры (для коллизии). */
    public static double smoothRadius = 3.5;
    /** Сглаженная позиция камеры (финальное демпфирование против дрожания). */
    public static double camX;
    public static double camY;
    public static double camZ;
    public static boolean hasCamPos = false;
    /** Счётчик кадров «чисто» подряд (гистерезис выхода из коллизии). */
    public static int clearFrames = 0;
    /** Тиков с начала смерти (для анимации FOV). */
    public static int animTicks = 0;
    /** Тиков перелёта камеры в точку орбиты (20 = перелёт завершён). */
    public static int blendTicks = 0;
    /** Текущий анимированный FOV. */
    public static double deathFov = 90.0;
    /** Стартовый FOV текущей смерти (из настройки). */
    public static double fovStart = 90.0;
    /** Камера в момент смерти (откуда летим в точку орбиты). */
    public static double blendFromX;
    public static double blendFromY;
    public static double blendFromZ;
    public static double blendFromYaw;
    public static double blendFromPitch;

    /** Сглаженный угол для текущего кадра. */
    public static double smoothAngle(double partialTick) {
        double delta = angleDeg - prevAngleDeg;
        // Корректный переход через 0/360.
        while (delta > 180.0) {
            delta -= 360.0;
        }
        while (delta < -180.0) {
            delta += 360.0;
        }
        return prevAngleDeg + delta * partialTick;
    }

    /**
     * Сглаженный прогресс перелёта (0..1) для текущего кадра.
     * Как smoothAngle: тики дают грубые ступени, partialTick сглаживает.
     */
    public static double smoothBlend(double partialTick, double lengthTicks) {
        double t = Math.min(1.0, (blendTicks + partialTick) / Math.max(1.0, lengthTicks));
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    /** Сглаженный FOV для текущего кадра (та же идея, что у угла). */
    public static double smoothDeathFov(double partialTick) {
        double durationTicks = Math.max(1.0, com.vlogf.deathcamera.DeathCameraConfig.FOV_DURATION_SEC * 20.0);
        double t = Math.min(1.0, (animTicks + partialTick) / durationTicks);
        double e = t * t * (3.0 - 2.0 * t);
        return fovStart + (110.0 - fovStart) * e;
    }

    public static void reset() {
        orbiting = false;
        hasDeathPos = false;
        prevAngleDeg = 0.0;
        angleDeg = 0.0;
        currentSpeedDeg = 0.0;
        prevCameraType = null;
        forcedPerspective = false;
        smoothRadius = 3.5;
        hasCamPos = false;
        clearFrames = 0;
        animTicks = 0;
        blendTicks = 0;
        deathFov = 90.0;
        fovStart = 90.0;
    }
}
