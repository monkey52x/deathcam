package com.vlogf.deathcamera.mixin;

import com.vlogf.deathcamera.DeathCameraConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Корень проблемы «мобы исчезают через 1-2 сек»:
 * ванила удаляет ServerPlayer на 20-м тике смерти (LivingEntity.tickDeath),
 * а удаление снимает все чанковые тикеты игрока и рассылает destroy-пакеты
 * мобам вокруг трупа. Замораживаем deathTime на 19 — игрок остаётся,
 * чанки и мобы на месте.
 * Миксин висит на LivingEntity (где метод объявлен), но отмена срабатывает
 * только для ServerPlayer — мобы умирают как обычно.
 * Сколько держать — DeathCameraConfig.DEATH_KEEP_SECONDS:
 * 0 = как ванила (не держать), -1 = без ограничений, иначе N секунд,
 * после чего заморозка отпускается и ванила удаляет игрока штатно.
 * На респаун не влияет: это то же состояние, что и быстрое возрождение
 * раньше 20-го тика (штатный путь ванилы).
 * Лежит в общей секции конфига: работает и в одиночной игре, и на
 * выделенном сервере (если мод стоит и там).
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Shadow
    public int deathTime;

    /** Сколько тиков уже проморожено, по UUID игрока. Чистим при отпускании. */
    private static final Map<UUID, Integer> FROZEN_TICKS = new HashMap<>();

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void deathcamera$keepServerPlayer(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer)) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        // Клиентская сторона смертью игрока не управляет (там свой миксин).
        if (self.level().isClientSide()) {
            return;
        }
        int keepSeconds = DeathCameraConfig.DEATH_KEEP_SECONDS;
        if (keepSeconds == 0 || !DeathCameraConfig.KEEP_CHUNKS_UPDATING || self.deathTime < 19) {
            return;
        }
        if (keepSeconds > 0) {
            UUID id = self.getUUID();
            int frozen = FROZEN_TICKS.getOrDefault(id, 0);
            if (frozen >= keepSeconds * 20) {
                // Время вышло: отпускаем, ванила удаляет игрока штатно.
                FROZEN_TICKS.remove(id);
                return;
            }
            FROZEN_TICKS.put(id, frozen + 1);
        }
        // keepSeconds < 0: держим бесконечно.
        ci.cancel();
    }
}
