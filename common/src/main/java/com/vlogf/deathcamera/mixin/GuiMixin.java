package com.vlogf.deathcamera.mixin;

import com.vlogf.deathcamera.DeathCameraConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Скрывает HUD (хотбар, опыт, прицел и т.д.) на экране смерти.
 * Сам экран смерти рисуется отдельно и остаётся видимым.
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void deathcamera$hideHudOnDeath(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        if (DeathCameraConfig.HIDE_HUD
                && Minecraft.getInstance().screen instanceof DeathScreen) {
            ci.cancel();
        }
    }
}
