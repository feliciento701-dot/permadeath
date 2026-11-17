package com.example.permadeath;

import com.example.permadeath.net.DeathPayload;
import com.example.permadeath.server.PermadeathCommands;
import com.example.permadeath.server.PermadeathManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermadeathMod implements ModInitializer {
    public static final String MOD_ID = "permadeath";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Inicializando Permadeath 2...");
        // El paquete se registra en ambos lados (servidor -> cliente)
        PayloadTypeRegistry.playS2C().register(DeathPayload.ID, DeathPayload.CODEC);
        PermadeathManager.register();
        PermadeathCommands.register();
    }
}
