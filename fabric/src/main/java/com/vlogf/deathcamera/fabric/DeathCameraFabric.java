package com.vlogf.deathcamera.fabric;

import com.vlogf.deathcamera.DeathCameraConfig;
import com.vlogf.deathcamera.client.DeathCameraClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public class DeathCameraFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DeathCameraConfig.init(FabricLoader.getInstance().getConfigDir().resolve("deathcamera.json"));
        ClientTickEvents.END_CLIENT_TICK.register(DeathCameraClient::onClientTick);
    }
}
