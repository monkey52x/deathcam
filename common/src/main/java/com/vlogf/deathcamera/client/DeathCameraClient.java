package com.vlogf.deathcamera.client;

import com.vlogf.deathcamera.DeathCameraConfig;
import com.vlogf.deathcamera.DeathCameraState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/**
 * Логика мода, общая для Fabric и NeoForge.
 * Вызывается из тика клиента в лоадер-модулях.
 * Имена официальные (Mojang): для Fabric их перемаппит Loom.
 */
public final class DeathCameraClient {
    private DeathCameraClient() {}

    public static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            stopOrbit(client);
            return;
        }

        if (isDead(client)) {
            if (ensureStarted(client)) {
                continueOrbit(client);
            }
        } else {
            stopOrbit(client);
        }
    }

    /** Мёртв ли игрок (здоровье, флаг или уже открытый экран смерти). */
    public static boolean isDead(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }
        return client.player.isDeadOrDying()
                || client.player.getHealth() <= 0.0F
                || client.screen instanceof DeathScreen;
    }

    /**
     * Старт смерти: запомнить точку, перечитать конфиг, включить F5.
     * Идемпотентен — вызывается и из тика, и из рендера (renderLevel HEAD),
     * чтобы не было ванильного кадра между пакетом смерти и первым тиком.
     * Возвращает true, если орбита/статика активна и нужен continueOrbit.
     */
    public static boolean ensureStarted(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }
        if (!DeathCameraState.orbiting) {
            DeathCameraState.orbiting = true;
            DeathCameraState.deathX = client.player.position().x;
            DeathCameraState.deathY = client.player.position().y;
            DeathCameraState.deathZ = client.player.position().z;
            DeathCameraState.hasDeathPos = true;
            DeathCameraState.prevCameraType = client.options.getCameraType();
            DeathCameraState.forcedPerspective = false;
            // Запоминаем камеру как есть — оттуда полетим в точку орбиты.
            try {
                var camera = client.gameRenderer.getMainCamera();
                var pos = camera.getPosition();
                DeathCameraState.blendFromX = pos.x;
                DeathCameraState.blendFromY = pos.y;
                DeathCameraState.blendFromZ = pos.z;
                DeathCameraState.blendFromYaw = camera.getYRot();
                DeathCameraState.blendFromPitch = camera.getXRot();
            } catch (Exception ignored) {
                DeathCameraState.blendFromX = DeathCameraState.deathX;
                DeathCameraState.blendFromY = DeathCameraState.deathY + DeathCameraConfig.HEIGHT;
                DeathCameraState.blendFromZ = DeathCameraState.deathZ;
                DeathCameraState.blendFromYaw = 0.0;
                DeathCameraState.blendFromPitch = 0.0;
            }
            // Стартовый угол орбиты: из вида от 3-го лица — ближайший к текущей
            // позиции камеры с небольшим разбросом, из первого лица — случайный.
            // Для статичной случайной точки (орбита выкл) — всегда случайный.
            boolean randomStatic = !DeathCameraConfig.ENABLE_ORBIT
                    && DeathCameraConfig.STATIC_MODE == DeathCameraConfig.StaticMode.RANDOM_POINT;
            double startAngle;
            if (DeathCameraState.prevCameraType == CameraType.FIRST_PERSON || randomStatic) {
                startAngle = Math.random() * 360.0;
            } else {
                double dx = DeathCameraState.blendFromX - DeathCameraState.deathX;
                double dz = DeathCameraState.blendFromZ - DeathCameraState.deathZ;
                if (dx * dx + dz * dz < 0.25) {
                    startAngle = Math.random() * 360.0;
                } else {
                    startAngle = Math.toDegrees(Math.atan2(dz, dx)) + (Math.random() * 20.0 - 10.0);
                }
            }
            DeathCameraState.prevAngleDeg = startAngle;
            DeathCameraState.angleDeg = startAngle;
            DeathCameraState.currentSpeedDeg = 0.0;
            DeathCameraState.smoothRadius = 3.5;
            DeathCameraState.animTicks = 0;
            // Из первого лица — сразу в точку (без перелёта), из F5 — плавный
            // сдвиг за TRANSITION_DURATION_SEC. Правило общее, и для орбиты,
            // и для статичной случайной точки, и для глайда LOCK.
            int fullBlend = Math.max(1, (int) Math.round(DeathCameraConfig.TRANSITION_DURATION_SEC * 20.0));
            DeathCameraState.blendTicks =
                    DeathCameraState.prevCameraType == CameraType.FIRST_PERSON ? fullBlend : 0;
            // FOV стартует со значения из настройки, дальше плавно до 110.
            DeathCameraState.fovStart = Math.min(100, Math.max(30, DeathCameraConfig.FOV_START));
            DeathCameraState.deathFov = DeathCameraState.fovStart;
            DeathCameraConfig.reload();
            forceThirdPersonView(client);
            return true;
        }
        return DeathCameraState.orbiting;
    }

    private static void continueOrbit(Minecraft client) {
        if (!DeathCameraState.hasDeathPos) {
            DeathCameraState.deathX = client.player.position().x;
            DeathCameraState.deathY = client.player.position().y;
            DeathCameraState.deathZ = client.player.position().z;
            DeathCameraState.hasDeathPos = true;
        }

        if (DeathCameraState.animTicks < 72000) {
            DeathCameraState.animTicks++;
        }
        int blendLen = Math.max(1, (int) Math.round(DeathCameraConfig.TRANSITION_DURATION_SEC * 20.0));
        if (DeathCameraState.blendTicks < blendLen) {
            DeathCameraState.blendTicks++;
        }

        // Кинематографичный FOV: от стартового значения плавно до 110.
        double fovDurationTicks = Math.max(1.0, DeathCameraConfig.FOV_DURATION_SEC * 20.0);
        double fovT = Math.min(1.0, DeathCameraState.animTicks / fovDurationTicks);
        double fovEase = fovT * fovT * (3.0 - 2.0 * fovT);
        DeathCameraState.deathFov = DeathCameraState.fovStart + (110.0 - DeathCameraState.fovStart) * fovEase;

        // 2. Включить вид от 3-го лица (re-check каждый тик — игрок мог нажать F5).
        forceThirdPersonView(client);

        // 3. Плавный разгон до скорости из конфига + поворот угла (если орбита вкл).
        // Без принудительного вида от 3-го лица орбита считается выключенной.
        if (DeathCameraConfig.ENABLE_ORBIT && DeathCameraConfig.FORCE_THIRD_PERSON) {
            // Разгон зафиксирован (бывшая настройка smoothRamp).
            double ramp = 0.06;
            DeathCameraState.prevAngleDeg = DeathCameraState.angleDeg;
            DeathCameraState.currentSpeedDeg +=
                    (DeathCameraConfig.SPEED_DEG_PER_TICK - DeathCameraState.currentSpeedDeg) * ramp;
            DeathCameraState.angleDeg += DeathCameraState.currentSpeedDeg;
            if (DeathCameraState.angleDeg >= 360.0) {
                DeathCameraState.angleDeg -= 360.0;
                DeathCameraState.prevAngleDeg -= 360.0;
            }
            if (DeathCameraState.angleDeg < 0.0) {
                DeathCameraState.angleDeg += 360.0;
                DeathCameraState.prevAngleDeg += 360.0;
            }
        }
    }

    private static void forceThirdPersonView(Minecraft client) {
        if (DeathCameraConfig.FORCE_THIRD_PERSON && client.options != null) {
            if (client.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                DeathCameraState.forcedPerspective = true;
            }
        }
    }

    private static void stopOrbit(Minecraft client) {
        if (!DeathCameraState.orbiting) {
            return;
        }
        boolean restore = DeathCameraState.forcedPerspective
                && client != null
                && client.options != null
                && DeathCameraState.prevCameraType != null;
        CameraType prev = DeathCameraState.prevCameraType;
        DeathCameraState.reset();

        // Вернуть камеру как была до смерти
        if (restore) {
            client.options.setCameraType(prev);
        }
    }
}
