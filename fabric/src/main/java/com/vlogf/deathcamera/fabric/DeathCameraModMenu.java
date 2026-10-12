package com.vlogf.deathcamera.fabric;

import com.vlogf.deathcamera.client.DeathCameraConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Кнопка настроек в ModMenu. Загружается только если стоит ModMenu
 * (зависимость modCompileOnly — на игре без ModMenu класс не трогается).
 * Без Cloth Config кнопка просто возвращает назад (настройки тогда
 * правятся файлом config/deathcamera.json).
 */
public class DeathCameraModMenu implements ModMenuApi {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory) (parent -> {
            if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
                return DeathCameraConfigScreen.create(parent);
            }
            return parent;
        });
    }
}
