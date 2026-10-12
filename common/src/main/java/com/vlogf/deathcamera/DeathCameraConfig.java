package com.vlogf.deathcamera;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Настройки мода, файл config/deathcamera.json.
 * Перечитывается при каждой смерти, так что правится без перезапуска игры.
 * Этот класс не зависит от загрузчика — общий для Fabric и NeoForge.
 */
public final class DeathCameraConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;

    /** Крутить камеру вокруг трупа. Если выкл — режим из staticMode. */
    public static boolean ENABLE_ORBIT = true;

    /**
     * Что делать с камерой, когда вращение выключено.
     * FOLLOW — ванилла от 3-го лица; LOCK_ON_CORPSE — та же позиция,
     * но взгляд всегда на труп; RANDOM_POINT — мгновенная случайная
     * точка как у орбиты, дальше стоит.
     */
    public enum StaticMode {
        FOLLOW,
        LOCK_ON_CORPSE,
        RANDOM_POINT
    }

    public static StaticMode STATIC_MODE = StaticMode.FOLLOW;

    /** Скорость вращения: градусов за тик (20 тиков = 1 сек). 0.45 = круг ~40 сек. */
    public static double SPEED_DEG_PER_TICK = 0.45;

    /** Высота камеры над точкой смерти (кламп -5..5). */
    public static double HEIGHT = 2.2;

    /** Смотреть чуть выше ног (на грудь), чтобы труп был в центре (кламп 1..5). */
    public static double TARGET_HEIGHT_OFFSET = 1.0;

    /** При смерти принудительно включать вид от 3-го лица. */
    public static boolean FORCE_THIRD_PERSON = true;

    /** Не ставить мир на паузу на экране смерти (чанки продолжают обновляться). */
    public static boolean KEEP_CHUNKS_UPDATING = true;

    /** Не показывать свой труп вообще (с первого кадра). */
    public static boolean HIDE_CORPSE = false;

    /** Убрать красный экран при смерти. */
    public static boolean REMOVE_RED_SCREEN = true;

    /**
     * Сколько секунд держать чанки прогруженными после смерти.
     * 0 = как ванила (не держать), -1 = без ограничений.
     */
    public static int DEATH_KEEP_SECONDS = 300;

    /** Компактный экран смерти: кнопки снизу, тексты выше. */
    public static boolean COMPACT_DEATH_SCREEN = false;

    /** Отступ ряда кнопок от низа экрана в пикселях. */
    public static int BUTTONS_BOTTOM_MARGIN = 70;

    /** Насколько пикселей поднять тексты экрана смерти. */
    public static int TEXTS_SHIFT_UP = 18;

    /** Скрывать HUD (хотбар, опыт, прицел) на экране смерти. */
    public static boolean HIDE_HUD = false;

    /** Кинематографичный FOV: плавно расширять до максимума на время смерти. */
    public static boolean CINEMATIC_FOV = true;

    /** Стартовый FOV синиматик-зума (кламп 30-100). */
    public static int FOV_START = 90;

    /** За сколько секунд FOV дойдёт от 90 до максимума. */
    public static double FOV_DURATION_SEC = 6.0;

    /** За сколько секунд камера долетает до точки орбиты. */
    public static double TRANSITION_DURATION_SEC = 1.5;

    private DeathCameraConfig() {}

    public static void init(Path configFile) {
        file = configFile;
        load();
    }

    public static void reload() {
        load();
    }

    public static void load() {
        if (file == null) {
            return;
        }
        try {
            if (!Files.exists(file)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                ENABLE_ORBIT = getBool(root, "enableOrbit", ENABLE_ORBIT);
                STATIC_MODE = getStaticMode(root, "staticMode", STATIC_MODE);
                SPEED_DEG_PER_TICK = getDouble(root, "speedDegPerTick", SPEED_DEG_PER_TICK);
                HEIGHT = getDouble(root, "height", HEIGHT);
                TARGET_HEIGHT_OFFSET = getDouble(root, "targetHeightOffset", TARGET_HEIGHT_OFFSET);
                FORCE_THIRD_PERSON = getBool(root, "forceThirdPerson", FORCE_THIRD_PERSON);
                KEEP_CHUNKS_UPDATING = getBool(root, "keepChunksUpdating", KEEP_CHUNKS_UPDATING);
                HIDE_CORPSE = getBool(root, "hideCorpse", HIDE_CORPSE);
                REMOVE_RED_SCREEN = getBool(root, "removeRedScreen", REMOVE_RED_SCREEN);
                DEATH_KEEP_SECONDS = getInt(root, "deathKeepSeconds", DEATH_KEEP_SECONDS);
                COMPACT_DEATH_SCREEN = getBool(root, "compactDeathScreen", COMPACT_DEATH_SCREEN);
                BUTTONS_BOTTOM_MARGIN = getInt(root, "buttonsBottomMargin", BUTTONS_BOTTOM_MARGIN);
                TEXTS_SHIFT_UP = getInt(root, "textsShiftUp", TEXTS_SHIFT_UP);
                HIDE_HUD = getBool(root, "hideHud", HIDE_HUD);
                CINEMATIC_FOV = getBool(root, "cinematicFov", CINEMATIC_FOV);
                FOV_START = getInt(root, "fovStart", FOV_START);
                FOV_DURATION_SEC = getDouble(root, "fovDurationSec", FOV_DURATION_SEC);
                TRANSITION_DURATION_SEC = getDouble(root, "transitionDurationSec", TRANSITION_DURATION_SEC);
            }
            // Дозаписать отсутствующие ключи, чтобы файл всегда был полным.
            // Старт синиматик-зума жёстко в пределах 30-100.
            FOV_START = Math.min(100, Math.max(30, FOV_START));
            // Значения меньше -1 не имеют смысла (-1 = без ограничений).
            if (DEATH_KEEP_SECONDS < -1) {
                DEATH_KEEP_SECONDS = -1;
            }
            // Границы ручного редактирования (экран дублирует их через setMin/setMax).
            HEIGHT = Math.min(5.0, Math.max(-5.0, HEIGHT));
            TARGET_HEIGHT_OFFSET = Math.min(5.0, Math.max(1.0, TARGET_HEIGHT_OFFSET));
            save();
        } catch (Exception e) {
            System.out.println("[DeathCamera] Failed to load config, using defaults: " + e.getMessage());
        }
    }

    public static void save() throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("enableOrbit", ENABLE_ORBIT);
        root.addProperty("staticMode", STATIC_MODE.name().toLowerCase());
        root.addProperty("speedDegPerTick", SPEED_DEG_PER_TICK);
        root.addProperty("height", HEIGHT);
        root.addProperty("targetHeightOffset", TARGET_HEIGHT_OFFSET);
        root.addProperty("forceThirdPerson", FORCE_THIRD_PERSON);
        root.addProperty("keepChunksUpdating", KEEP_CHUNKS_UPDATING);
        root.addProperty("hideCorpse", HIDE_CORPSE);
        root.addProperty("removeRedScreen", REMOVE_RED_SCREEN);
        root.addProperty("deathKeepSeconds", DEATH_KEEP_SECONDS);
        root.addProperty("compactDeathScreen", COMPACT_DEATH_SCREEN);
        root.addProperty("buttonsBottomMargin", BUTTONS_BOTTOM_MARGIN);
        root.addProperty("textsShiftUp", TEXTS_SHIFT_UP);
        root.addProperty("hideHud", HIDE_HUD);
        root.addProperty("cinematicFov", CINEMATIC_FOV);
        root.addProperty("fovStart", FOV_START);
        root.addProperty("fovDurationSec", FOV_DURATION_SEC);
        root.addProperty("transitionDurationSec", TRANSITION_DURATION_SEC);
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(root, writer);
        }
    }

    /** Сохранение без checked-исключения — для кнопки Save в экране настроек. */
    public static void saveQuiet() {
        try {
            if (file != null) {
                save();
            }
        } catch (IOException e) {
            System.out.println("[DeathCamera] Failed to save config: " + e.getMessage());
        }
    }

    private static double getDouble(JsonObject root, String key, double fallback) {
        try {
            return root.has(key) ? root.get(key).getAsDouble() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int getInt(JsonObject root, String key, int fallback) {
        try {
            return root.has(key) ? root.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static StaticMode getStaticMode(JsonObject root, String key, StaticMode fallback) {
        try {
            if (root.has(key)) {
                return StaticMode.valueOf(root.get(key).getAsString().toUpperCase());
            }
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean getBool(JsonObject root, String key, boolean fallback) {
        try {
            return root.has(key) ? root.get(key).getAsBoolean() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
