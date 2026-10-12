package com.vlogf.deathcamera.neoforge;

import com.vlogf.deathcamera.DeathCamera;
import com.vlogf.deathcamera.DeathCameraConfig;
import com.vlogf.deathcamera.client.DeathCameraClient;
import com.vlogf.deathcamera.client.DeathCameraConfigScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Supplier;

/**
 * Мод грузится и на клиенте, и на сервере:
 * - на сервере нужен только LivingEntityMixin (удержание игрока),
 *   весь клиентский код ниже выполняется строго на клиенте.
 * Ссылки на клиентские классы в теле методов безопасны: JVM резолвит
 * их лениво, а на сервере эти ветки не исполняются.
 */
@Mod(DeathCamera.MOD_ID)
public class DeathCameraNeoForge {

    public DeathCameraNeoForge(IEventBus modBus, ModContainer container) {
        // Чистая серверная часть: свой deathcamera.json в config сервера.
        DeathCameraConfig.init(FMLPaths.CONFIGDIR.get().resolve("deathcamera.json"));
        if (FMLEnvironment.dist.isClient()) {
            initClient(container);
        }
    }

    private static void initClient(ModContainer container) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
                DeathCameraClient.onClientTick(Minecraft.getInstance()));
        // Штатная кнопка Config в списке модов. Без Cloth Config кнопка просто
        // возвращает назад (настройки тогда правятся файлом config/deathcamera.json).
        Supplier<IConfigScreenFactory> screenFactory = () -> (c, parent) -> ModList.get().isLoaded("cloth_config")
                ? DeathCameraConfigScreen.create(parent)
                : parent;
        container.registerExtensionPoint(IConfigScreenFactory.class, screenFactory);
    }
}
