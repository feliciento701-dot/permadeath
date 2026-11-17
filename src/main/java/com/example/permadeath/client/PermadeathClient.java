package com.example.permadeath.client;

import com.example.permadeath.net.DeathPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.DeathScreen;

public class PermadeathClient implements ClientModInitializer {
    /** Cada vez que la DeathScreen se (re)inicia solo el último listener dibuja. */
    private static int screenGeneration = 0;

    @Override
    public void onInitializeClient() {
        // Paquete del servidor -> arranca la animación
        ClientPlayNetworking.registerGlobalReceiver(DeathPayload.ID, (payload, context) ->
                context.client().execute(() -> DeathAnimation.start(payload.uuid(), payload.name())));

        // Animación sobre la pantalla de muerte
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof DeathScreen) {
                final int generation = ++screenGeneration;
                ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
                    if (generation == screenGeneration) {
                        DeathAnimation.render(drawContext, s.width, s.height);
                    }
                });
            }
        });

        // Animación en el HUD (otros jugadores / tras reaparecer como espectador)
        PermadeathHudOverlay.register();
    }
}
