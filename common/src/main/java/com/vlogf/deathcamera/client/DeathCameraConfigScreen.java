package com.vlogf.deathcamera.client;

import com.vlogf.deathcamera.DeathCameraConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран настроек в игре (Cloth Config).
 * Общий для Fabric и NeoForge: открывается через ModMenu / кнопку Config в списке модов.
 * Все строки через lang-файлы (en_us, ru_ru).
 * Категории заложены с запасом — позже сюда добавятся видео-превью настроек.
 */
public final class DeathCameraConfigScreen {
    private DeathCameraConfigScreen() {}

    private static Component text(String key) {
        return Component.translatable(key);
    }

    public static Screen create(Screen parent) {
        DeathCameraConfig.load();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(text("deathcamera.config.title"))
                .setSavingRunnable(DeathCameraConfig::saveQuiet);
        ConfigEntryBuilder eb = builder.entryBuilder();

        // --- Камера ---
        ConfigCategory camera = builder.getOrCreateCategory(text("deathcamera.config.category.camera"));
        camera.addEntry(eb.startBooleanToggle(text("deathcamera.config.enableOrbit"), DeathCameraConfig.ENABLE_ORBIT)
                .setDefaultValue(true)
                .setTooltip(text("deathcamera.config.enableOrbit.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.ENABLE_ORBIT = v)
                .build());
        camera.addEntry(eb.startEnumSelector(text("deathcamera.config.staticMode"), DeathCameraConfig.StaticMode.class, DeathCameraConfig.STATIC_MODE)
                .setDefaultValue(DeathCameraConfig.StaticMode.FOLLOW)
                .setEnumNameProvider(v -> text("deathcamera.config.staticMode." + v.name().toLowerCase()))
                .setTooltip(text("deathcamera.config.staticMode.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.STATIC_MODE = v)
                .build());
        camera.addEntry(eb.startBooleanToggle(text("deathcamera.config.forceThirdPerson"), DeathCameraConfig.FORCE_THIRD_PERSON)
                .setDefaultValue(true)
                .setTooltip(text("deathcamera.config.forceThirdPerson.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.FORCE_THIRD_PERSON = v)
                .build());
        camera.addEntry(eb.startDoubleField(text("deathcamera.config.speed"), DeathCameraConfig.SPEED_DEG_PER_TICK)
                .setDefaultValue(0.45)
                .setTooltip(text("deathcamera.config.speed.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.SPEED_DEG_PER_TICK = v)
                .build());
        camera.addEntry(eb.startDoubleField(text("deathcamera.config.transitionDurationSec"), DeathCameraConfig.TRANSITION_DURATION_SEC)
                .setDefaultValue(1.5)
                .setTooltip(text("deathcamera.config.transitionDurationSec.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.TRANSITION_DURATION_SEC = v)
                .build());
        camera.addEntry(eb.startDoubleField(text("deathcamera.config.height"), DeathCameraConfig.HEIGHT)
                .setDefaultValue(2.2)
                .setMin(-5.0)
                .setMax(5.0)
                .setTooltip(text("deathcamera.config.height.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.HEIGHT = v)
                .build());
        camera.addEntry(eb.startDoubleField(text("deathcamera.config.targetHeightOffset"), DeathCameraConfig.TARGET_HEIGHT_OFFSET)
                .setDefaultValue(1.0)
                .setMin(1.0)
                .setMax(5.0)
                .setTooltip(text("deathcamera.config.targetHeightOffset.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.TARGET_HEIGHT_OFFSET = v)
                .build());
        camera.addEntry(eb.startBooleanToggle(text("deathcamera.config.cinematicFov"), DeathCameraConfig.CINEMATIC_FOV)
                .setDefaultValue(true)
                .setTooltip(text("deathcamera.config.cinematicFov.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.CINEMATIC_FOV = v)
                .build());
        camera.addEntry(eb.startIntField(text("deathcamera.config.fovStart"), DeathCameraConfig.FOV_START)
                .setDefaultValue(90)
                .setTooltip(text("deathcamera.config.fovStart.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.FOV_START = v)
                .build());
        camera.addEntry(eb.startDoubleField(text("deathcamera.config.fovDurationSec"), DeathCameraConfig.FOV_DURATION_SEC)
                .setDefaultValue(6.0)
                .setTooltip(text("deathcamera.config.fovDurationSec.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.FOV_DURATION_SEC = v)
                .build());
        // --- Мир ---
        ConfigCategory world = builder.getOrCreateCategory(text("deathcamera.config.category.world"));
        world.addEntry(eb.startBooleanToggle(text("deathcamera.config.keepChunks"), DeathCameraConfig.KEEP_CHUNKS_UPDATING)
                .setDefaultValue(true)
                .setTooltip(text("deathcamera.config.keepChunks.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.KEEP_CHUNKS_UPDATING = v)
                .build());
        world.addEntry(eb.startIntField(text("deathcamera.config.deathKeepSeconds"), DeathCameraConfig.DEATH_KEEP_SECONDS)
                .setDefaultValue(300)
                .setTooltip(text("deathcamera.config.deathKeepSeconds.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.DEATH_KEEP_SECONDS = v)
                .build());
        world.addEntry(eb.startBooleanToggle(text("deathcamera.config.hideCorpse"), DeathCameraConfig.HIDE_CORPSE)
                .setDefaultValue(false)
                .setTooltip(text("deathcamera.config.hideCorpse.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.HIDE_CORPSE = v)
                .build());

        // --- Экран ---
        ConfigCategory overlay = builder.getOrCreateCategory(text("deathcamera.config.category.screen"));
        overlay.addEntry(eb.startBooleanToggle(text("deathcamera.config.removeRedScreen"), DeathCameraConfig.REMOVE_RED_SCREEN)
                .setDefaultValue(true)
                .setTooltip(text("deathcamera.config.removeRedScreen.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.REMOVE_RED_SCREEN = v)
                .build());
        overlay.addEntry(eb.startBooleanToggle(text("deathcamera.config.compactDeathScreen"), DeathCameraConfig.COMPACT_DEATH_SCREEN)
                .setDefaultValue(false)
                .setTooltip(text("deathcamera.config.compactDeathScreen.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.COMPACT_DEATH_SCREEN = v)
                .build());
        overlay.addEntry(eb.startIntField(text("deathcamera.config.buttonsHeight"), DeathCameraConfig.BUTTONS_BOTTOM_MARGIN)
                .setDefaultValue(70)
                .setTooltip(text("deathcamera.config.buttonsHeight.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.BUTTONS_BOTTOM_MARGIN = v)
                .build());
        overlay.addEntry(eb.startIntField(text("deathcamera.config.textsHeight"), DeathCameraConfig.TEXTS_SHIFT_UP)
                .setDefaultValue(18)
                .setTooltip(text("deathcamera.config.textsHeight.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.TEXTS_SHIFT_UP = v)
                .build());
        overlay.addEntry(eb.startBooleanToggle(text("deathcamera.config.hideHud"), DeathCameraConfig.HIDE_HUD)
                .setDefaultValue(false)
                .setTooltip(text("deathcamera.config.hideHud.desc"))
                .setSaveConsumer(v -> DeathCameraConfig.HIDE_HUD = v)
                .build());

        return builder.build();
    }
}
