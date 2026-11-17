package com.example.permadeath.server;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class PermadeathCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("permadeath")
                        .requires(src -> src.hasPermissionLevel(2))

                        // /permadeath test  -> reproduce la animación sin morir
                        .then(CommandManager.literal("test").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            PermadeathManager.broadcastAnimation(ctx.getSource().getServer(),
                                    p.getUuidAsString(), p.getName().getString());
                            return 1;
                        }))

                        // /permadeath revive <jugador>
                        .then(CommandManager.literal("revive")
                                .then(CommandManager.argument("jugador", GameProfileArgumentType.gameProfile())
                                        .executes(ctx -> {
                                            Collection<GameProfile> profiles =
                                                    GameProfileArgumentType.getProfileArgument(ctx, "jugador");
                                            int count = 0;
                                            for (GameProfile profile : profiles) {
                                                if (PermadeathManager.revive(ctx.getSource().getServer(), profile.getId())) {
                                                    count++;
                                                }
                                            }
                                            final int revived = count;
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("Jugadores revividos: " + revived), true);
                                            return revived;
                                        })))));
    }
}
