package com.vlogf.deathcamera.mixin;

import com.vlogf.deathcamera.DeathCameraConfig;
import com.vlogf.deathcamera.DeathCameraState;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * После обычного расчета камеры, если игрок мертв и орбита включена —
 * ставим камеру на орбиту вокруг точки смерти и направляем взгляд на труп.
 * Угол интерполируется между тиками, поэтому движение плавное на любом FPS.
 * Коллизия как в ваниле: 8 лучей вокруг, минимум — камера упирается в стену.
 * При выключенной орбите — режимы staticMode (только от 3-го лица):
 * FOLLOW отдаёт камеру ванилле, LOCK_ON_CORPSE глайдит по вертикали
 * на HEIGHT и смотрит на труп, RANDOM_POINT ставит камеру общей
 * постановкой с орбитой, но без вращения.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    /** Сколько кадров подряд должно быть «чисто», чтобы выйти из коллизии. */
    private static final int CLEAR_FRAMES_NEEDED = 3;
    /** Насколько не доходить до стены. */
    private static final double WALL_PADDING = 0.25;

    @Shadow
    public abstract void setPosition(Vec3 pos);

    @Shadow
    public abstract void setPosition(double x, double y, double z);

    @Shadow
    public abstract void setRotation(float yaw, float pitch);

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    @Inject(method = "setup", at = @At("TAIL"))
    private void deathcamera$orbitOnDeath(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        if (!DeathCameraState.orbiting || !DeathCameraState.hasDeathPos) {
            return;
        }
        // Единый предикат смерти как в DeathCameraClient: entity может ещё
        // считаться живой 1-2 кадра, пока DeathScreen уже открыт. Без проверки
        // экрана эти кадры рисуется ванилла (отъезд + наклон), потом телепорт.
        // Основной on/off логики — в DeathCameraClient.
        if (entity == null) {
            return;
        }
        if (entity.isAlive() && !isDeathScreenOpen()) {
            return;
        }

        // Орбита работает только в связке с принудительным видом от 3-го лица:
        // при выключенном forceThirdPerson включённая enableOrbit игнорируется.
        boolean orbitOn = DeathCameraConfig.ENABLE_ORBIT && DeathCameraConfig.FORCE_THIRD_PERSON;
        if (!orbitOn) {
            // Режимы без вращения — только от 3-го лица (detached).
            // Иначе в F1 камера уходила бы при перволичном рендере
            // (рука тянулась к центру экрана).
            if (!detached) {
                return;
            }
            if (DeathCameraConfig.STATIC_MODE == DeathCameraConfig.StaticMode.FOLLOW) {
                return;
            }
            if (DeathCameraConfig.STATIC_MODE == DeathCameraConfig.StaticMode.LOCK_ON_CORPSE) {
                lookAtCorpseFromHere(level, entity, partialTick);
                return;
            }
            // RANDOM_POINT — ниже общей постановкой с орбитой (угол статичен,
            // тик его не крутит; перелёт как у орбиты: из F1 сразу, из F5 плавно).
        }

        // Первый реально отрисованный кадр орбиты: обновить старт перелёта
        // из ванильной позиции ЭТОГО кадра (она уже посчитана выше по методу,
        // мы на TAIL). Иначе blendFrom протухает за пропущенные кадры:
        // видно отъезд ваниллы, потом прыжок в точку старта.
        // Только для орбиты: статичная точка хранит случайный угол из тика.
        if (!DeathCameraState.hasCamPos && orbitOn) {
            try {
                Vec3 vanillaPos = this.getPosition();
                DeathCameraState.blendFromX = vanillaPos.x;
                DeathCameraState.blendFromY = vanillaPos.y;
                DeathCameraState.blendFromZ = vanillaPos.z;
                DeathCameraState.blendFromYaw = this.getYRot();
                DeathCameraState.blendFromPitch = this.getXRot();
            } catch (Exception ignored) {
                // остаёмся на захвате из тика
            }
            // Пересчитать стартовый угол от свежей позиции, иначе орбита
            // стартует не от того, что на экране.
            double freshAngle;
            if (DeathCameraState.prevCameraType == CameraType.FIRST_PERSON) {
                freshAngle = Math.random() * 360.0;
            } else {
                double dx = DeathCameraState.blendFromX - DeathCameraState.deathX;
                double dz = DeathCameraState.blendFromZ - DeathCameraState.deathZ;
                if (dx * dx + dz * dz < 0.25) {
                    freshAngle = Math.random() * 360.0;
                } else {
                    freshAngle = Math.toDegrees(Math.atan2(dz, dx)) + (Math.random() * 20.0 - 10.0);
                }
            }
            DeathCameraState.prevAngleDeg = freshAngle;
            DeathCameraState.angleDeg = freshAngle;
            // Из F5 перезапустить перелёт от свежей точки; из F1 бленд и так
            // завершён (мгновенная постановка в точку) — не трогаем.
            if (DeathCameraState.prevCameraType != CameraType.FIRST_PERSON) {
                DeathCameraState.blendTicks = 0;
            }
        }

        // Радиус зафиксирован (бывшая настройка radius).
        double radius = 3.5;
        double height = DeathCameraConfig.HEIGHT;
        double base = Math.toRadians(DeathCameraState.smoothAngle(partialTick));

        double targetX = DeathCameraState.deathX;
        double targetY = DeathCameraState.deathY + DeathCameraConfig.TARGET_HEIGHT_OFFSET;
        double targetZ = DeathCameraState.deathZ;
        Vec3 target = new Vec3(targetX, targetY, targetZ);

        double useAngle = base;
        double[] dir = direction(useAngle, radius, height);
        double fullLen = dir[3];
        double wantR = fullLen;

        // Коллизия всегда включена (бывшая настройка cameraCollision).
        {
            // Как ванильный getMaxZoom: 8 лучей со сдвигом +-0.1, берём минимум.
            // Ловит и скользящие попадания краем, поэтому камера не проходит в стены.
            double best = fullLen;
            for (int i = 0; i < 8; i++) {
                double ox = ((i & 1) * 2 - 1) * 0.1;
                double oy = (((i >> 1) & 1) * 2 - 1) * 0.1;
                double oz = (((i >> 2) & 1) * 2 - 1) * 0.1;
                Vec3 start = new Vec3(targetX + ox, targetY + oy, targetZ + oz);
                Vec3 end = new Vec3(start.x + dir[0] * fullLen, start.y + dir[1] * fullLen, start.z + dir[2] * fullLen);
                BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
                if (hit.getType() != HitResult.Type.MISS) {
                    double d = hit.getLocation().distanceTo(target);
                    if (d < best) {
                        best = d;
                    }
                }
            }
            if (best < fullLen - 1.0e-4) {
                wantR = Math.max(0.3, best - WALL_PADDING);
            }

            // Сглаживаем дистанцию с гистерезисом: внутрь — быстро
            // (нельзя оказаться в стене ни на кадр), наружу — только после
            // нескольких чистых кадров (не даём дрожать на краях листвы).
            double smooth = DeathCameraState.smoothRadius;
            if (wantR < smooth - 1.0e-3) {
                DeathCameraState.clearFrames = 0;
                smooth += (wantR - smooth) * 0.45;
            } else if (wantR >= fullLen - 0.05) {
                if (++DeathCameraState.clearFrames >= CLEAR_FRAMES_NEEDED) {
                    smooth += (wantR - smooth) * 0.3;
                    if (fullLen - smooth < 0.05) {
                        smooth = fullLen;
                        DeathCameraState.clearFrames = 0;
                    }
                }
            } else {
                DeathCameraState.clearFrames = 0;
                smooth += (wantR - smooth) * 0.3;
            }
            DeathCameraState.smoothRadius = smooth;
            wantR = smooth;
        }

        // Плавный перелёт из камеры момента смерти в точку орбиты.
        // Из F5 это сдвиг, из F1 камера сразу в точке (бленд завершён заранее).
        // partialTick добавлен как у угла — иначе 20 ступеней в секунду.
        double blendLen = Math.max(1.0, DeathCameraConfig.TRANSITION_DURATION_SEC * 20.0);
        double blendEase = DeathCameraState.smoothBlend(partialTick, blendLen);
        double[] dir2 = direction(useAngle, radius, height);
        double tx = DeathCameraState.blendFromX
                + ((targetX + dir2[0] * wantR) - DeathCameraState.blendFromX) * blendEase;
        double ty = DeathCameraState.blendFromY
                + ((targetY + dir2[1] * wantR) - DeathCameraState.blendFromY) * blendEase;
        double tz = DeathCameraState.blendFromZ
                + ((targetZ + dir2[2] * wantR) - DeathCameraState.blendFromZ) * blendEase;
        if (!DeathCameraState.hasCamPos) {
            DeathCameraState.camX = tx;
            DeathCameraState.camY = ty;
            DeathCameraState.camZ = tz;
            DeathCameraState.hasCamPos = true;
        } else {
            DeathCameraState.camX += (tx - DeathCameraState.camX) * 0.55;
            DeathCameraState.camY += (ty - DeathCameraState.camY) * 0.55;
            DeathCameraState.camZ += (tz - DeathCameraState.camZ) * 0.55;
        }

        // Смотрим на грудь трупа с фактической позиции.
        double dx = targetX - DeathCameraState.camX;
        double dy = targetY - DeathCameraState.camY;
        double dz = targetZ - DeathCameraState.camZ;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float lookYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)));
        float lookPitch = distXZ < 1.0e-4 ? -90.0F : (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));
        float yaw = (float) (DeathCameraState.blendFromYaw
                + Mth.wrapDegrees(lookYaw - DeathCameraState.blendFromYaw) * blendEase);
        float pitch = (float) (DeathCameraState.blendFromPitch
                + (lookPitch - DeathCameraState.blendFromPitch) * blendEase);

        this.setPosition(DeathCameraState.camX, DeathCameraState.camY, DeathCameraState.camZ);
        this.setRotation(yaw, pitch);
    }

    private static boolean isDeathScreenOpen() {
        try {
            return Minecraft.getInstance().screen instanceof DeathScreen;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Режим «взгляд на тело»: горизонталь (X/Z) оставляем ванильную,
     * высота глайдит к фиксированной (deathY + HEIGHT) по тому же
     * перелёту, что у орбиты (из F1 — мгновенно, из F5 — плавно
     * за TRANSITION_DURATION_SEC), взгляд — на труп
     * (точка смерти + TARGET_HEIGHT_OFFSET). Подъём защищён от потолка
     * одним вертикальным лучом, иначе под низким потолком был бы чёрный экран.
     */
    private void lookAtCorpseFromHere(BlockGetter level, Entity entity, float partialTick) {
        double targetX = DeathCameraState.deathX;
        double targetY = DeathCameraState.deathY + DeathCameraConfig.TARGET_HEIGHT_OFFSET;
        double targetZ = DeathCameraState.deathZ;
        Vec3 here;
        try {
            here = this.getPosition();
        } catch (Exception ignored) {
            return;
        }
        double blendLen = Math.max(1.0, DeathCameraConfig.TRANSITION_DURATION_SEC * 20.0);
        double ease = DeathCameraState.smoothBlend(partialTick, blendLen);
        // Целевая высота при ванильной горизонтали, с защитой от потолка/пола.
        double wantY = DeathCameraState.deathY + DeathCameraConfig.HEIGHT;
        double destY = wantY;
        try {
            Vec3 from = new Vec3(here.x, here.y, here.z);
            Vec3 to = new Vec3(here.x, wantY, here.z);
            BlockHitResult hit = level.clip(
                    new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != HitResult.Type.MISS) {
                double hy = hit.getLocation().y;
                destY = wantY >= here.y ? Math.min(wantY, hy - 0.25) : Math.max(wantY, hy + 0.25);
            }
        } catch (Exception ignored) {
            // без защиты — как есть
        }
        double y = DeathCameraState.blendFromY + (destY - DeathCameraState.blendFromY) * ease;
        this.setPosition(here.x, y, here.z);
        double dx = targetX - here.x;
        double dy = targetY - y;
        double dz = targetZ - here.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float lookYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)));
        float lookPitch = distXZ < 1.0e-4 ? -90.0F : (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));
        float yaw = (float) (DeathCameraState.blendFromYaw
                + Mth.wrapDegrees(lookYaw - DeathCameraState.blendFromYaw) * ease);
        float pitch = (float) (DeathCameraState.blendFromPitch
                + (lookPitch - DeathCameraState.blendFromPitch) * ease);
        this.setRotation(yaw, pitch);
    }

    /**
     * Единичное направление от трупа при заданном угле.
     * Возвращает {x, y, z, fullLen}, где fullLen — дистанция вдоль него
     * для горизонтальной проекции radius.
     */
    private static double[] direction(double angle, double radius, double height) {
        double slope = (DeathCameraState.deathY + height
                - (DeathCameraState.deathY + DeathCameraConfig.TARGET_HEIGHT_OFFSET)) / radius;
        double len = Math.sqrt(1.0 + slope * slope);
        return new double[]{
                Math.cos(angle) / len,
                slope / len,
                Math.sin(angle) / len,
                radius * len,
        };
    }
}
