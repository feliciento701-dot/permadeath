package com.example.permadeath.server;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Guarda en el mundo qué jugadores ya murieron (sobrevive a reinicios). */
public class PermadeathState extends PersistentState {
    private final Set<UUID> dead = new HashSet<>();

    public static final PersistentState.Type<PermadeathState> TYPE =
            new PersistentState.Type<>(PermadeathState::new, PermadeathState::fromNbt, null);

    public static PermadeathState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "permadeath_state");
    }

    public boolean isDead(UUID id) {
        return dead.contains(id);
    }

    public void markDead(UUID id) {
        if (dead.add(id)) markDirty();
    }

    public boolean revive(UUID id) {
        boolean removed = dead.remove(id);
        if (removed) markDirty();
        return removed;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (UUID id : dead) list.add(NbtString.of(id.toString()));
        nbt.put("dead", list);
        return nbt;
    }

    public static PermadeathState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        PermadeathState state = new PermadeathState();
        NbtList list = nbt.getList("dead", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            state.dead.add(UUID.fromString(list.getString(i)));
        }
        return state;
    }
}
