package com.example.permadeath.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;

public class PermadeathHudOverlay {
    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.currentScreen instanceof DeathScreen) return;
            DeathAnimation.render(context, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        });
    }
}
