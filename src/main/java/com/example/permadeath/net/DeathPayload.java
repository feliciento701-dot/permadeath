package com.example.permadeath.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> clientes: "este jugador acaba de morir, reproduce la animación". */
public record DeathPayload(String uuid, String name) implements CustomPayload {
    public static final CustomPayload.Id<DeathPayload> ID =
            new CustomPayload.Id<>(Identifier.of("permadeath", "death"));

    public static final PacketCodec<RegistryByteBuf, DeathPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, DeathPayload::uuid,
            PacketCodecs.STRING, DeathPayload::name,
            DeathPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
