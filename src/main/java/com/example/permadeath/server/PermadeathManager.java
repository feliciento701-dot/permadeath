package com.example.permadeath.server;

import com.example.permadeath.net.DeathPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PermadeathManager {
    /** Ticks que se deja ver la animación antes de expulsar (240 = 12 s). */
    public static final int KICK_DELAY_TICKS = 240;

    /** Jugadores muertos que aún no han sido expulsados: UUID -> ticks restantes. */
    private static final Map<UUID, Integer> pendingKick = new HashMap<>();

    public static void register() {
        // 1) Muerte de un jugador
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                onPlayerDeath(player);
            }
        });

        // 2) Si pulsa "Reaparecer" durante la cuenta atrás, queda como espectador
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            MinecraftServer server = newPlayer.getServer();
            if (server != null && PermadeathState.get(server).isDead(newPlayer.getUuid())) {
                newPlayer.changeGameMode(GameMode.SPECTATOR);
            }
        });

        // 3) Un muerto no puede volver a entrar
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (PermadeathState.get(server).isDead(handler.player.getUuid())) {
                handler.disconnect(kickMessage());
            }
        });

        // 4) Cuenta atrás de la expulsión
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (pendingKick.isEmpty()) return;
            Iterator<Map.Entry<UUID, Integer>> it = pendingKick.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> entry = it.next();
                int left = entry.getValue() - 1;
                if (left <= 0) {
                    it.remove();
                    ServerPlayerEntity p = server.getPlayerManager().getPlayer(entry.getKey());
                    if (p != null) p.networkHandler.disconnect(kickMessage());
                } else {
                    entry.setValue(left);
                }
            }
        });
    }

    private static void onPlayerDeath(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        PermadeathState state = PermadeathState.get(server);
        if (state.isDead(player.getUuid())) return;
        state.markDead(player.getUuid());

        String name = player.getName().getString();
        Text deathMessage = Text.literal("[VACÍO] " + name
                        + " ha agotado sus fragmentos de alma. El cráneo ha reclamado su destino.")
                .formatted(Formatting.DARK_RED, Formatting.BOLD);
        server.getPlayerManager().broadcast(deathMessage, false);

        broadcastAnimation(server, player.getUuidAsString(), name);
        pendingKick.put(player.getUuid(), KICK_DELAY_TICKS);
    }

    /** Envía la animación a todos los jugadores conectados (también al propio muerto). */
    public static void broadcastAnimation(MinecraftServer server, String uuid, String name) {
        DeathPayload payload = new DeathPayload(uuid, name);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** Quita la marca de muerte (comando /permadeath revive). */
    public static boolean revive(MinecraftServer server, UUID id) {
        pendingKick.remove(id);
        boolean was = PermadeathState.get(server).revive(id);
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
        if (online != null) online.changeGameMode(GameMode.SURVIVAL);
        return was;
    }

    private static Text kickMessage() {
        return Text.literal("ESTABLECIENDO CONDENA\n\nTu travesía en este mundo ha terminado.\nEl juicio del vacío es irreversible.")
                .formatted(Formatting.DARK_RED, Formatting.BOLD);
    }
}
