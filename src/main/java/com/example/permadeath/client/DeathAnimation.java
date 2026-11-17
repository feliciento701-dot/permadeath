package com.example.permadeath.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.UUID;

/**
 * Animación de muerte: viñeta roja, cabeza del jugador muerto girando y semitransparente al fondo,
 * y la calavera con glitch al frente. Todo se dibuja con DrawContext (sin mixins).
 */
public class DeathAnimation {
    private static final Identifier[] SKULL = new Identifier[7];
    static {
        for (int i = 0; i < SKULL.length; i++) {
            SKULL[i] = Identifier.of("permadeath", "textures/gui/skull/frame_" + i + ".png");
        }
    }

    /** {frame, ticks}: misma secuencia que el datapack (45 ticks = 2,25 s). */
    private static final int[][] TIMELINE = {
            {0, 4}, {1, 3}, {0, 2}, {2, 2}, {1, 2}, {3, 3}, {4, 2},
            {3, 2}, {5, 3}, {2, 1}, {4, 1}, {3, 6}, {5, 4}, {6, 10}
    };
    private static final int TIMELINE_TICKS = 45;

    private static final long DURATION_SELF_MS = 12_000;
    private static final long DURATION_OTHERS_MS = 4_500;

    private static boolean active = false;
    private static boolean self = false;
    private static long startMs = 0;
    private static UUID uuid;
    private static String playerName = "";
    private static Identifier skin;

    public static void start(String uuidString, String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        UUID id = UUID.fromString(uuidString);
        // evita reiniciar si el paquete llega duplicado
        if (active && id.equals(uuid) && Util.getMeasuringTimeMs() - startMs < 1000) return;

        uuid = id;
        playerName = name;
        self = mc.player != null && mc.player.getUuid().equals(id);
        skin = resolveSkin(mc);
        startMs = Util.getMeasuringTimeMs();
        active = true;

        float volume = self ? 1.0f : 0.6f;
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.7f, volume));
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, volume));
    }

    private static Identifier resolveSkin(MinecraftClient mc) {
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry != null) return entry.getSkinTextures().texture();
        }
        if (self && mc.player != null) return mc.player.getSkinTextures().texture();
        return DefaultSkinHelper.getSkinTextures(uuid).texture();
    }

    public static void render(DrawContext ctx, int w, int h) {
        if (!active) return;
        long t = Util.getMeasuringTimeMs() - startMs;
        long total = self ? DURATION_SELF_MS : DURATION_OTHERS_MS;
        if (t > total) {
            active = false;
            return;
        }

        float fadeIn = clamp(t / 300f);
        float fadeOut = clamp((total - t) / 1000f);
        float a = fadeIn * fadeOut * (self ? 1.0f : 0.85f);

        drawVignette(ctx, w, h, a);
        drawHead(ctx, w, h, a, t);
        drawSkull(ctx, w, h, a, t);
        drawText(ctx, w, h, a, t);
    }

    // ------------------------------------------------------------------ fondo
    private static void drawVignette(DrawContext ctx, int w, int h, float a) {
        int dark = ((int) (clamp(a) * 0x70) & 0xFF) << 24 | 0x0A0000;
        ctx.fill(0, 0, w, h, dark);
        int edge = ((int) (clamp(a) * 0xB0) & 0xFF) << 24 | 0x300000;
        int clear = 0x00300000;
        ctx.fillGradient(0, 0, w, h / 3, edge, clear);
        ctx.fillGradient(0, h * 2 / 3, w, h, clear, edge);
    }

    // ------------------------------------------------- cabeza girando al fondo
    private static void drawHead(DrawContext ctx, int w, int h, float a, long t) {
        if (skin == null) return;
        int size = (int) (Math.min(w, h) * 0.8f);
        float cx = w / 2f;
        float cy = h / 2f + (float) Math.sin(t / 900.0) * h * 0.015f;
        MatrixStack ms = ctx.getMatrices();

        RenderSystem.enableBlend();
        // 3 copias con retraso angular -> estela fantasmal
        for (int i = 0; i < 3; i++) {
            double angle = t / 1000.0 * 2.1 - i * 0.35;
            float cos = (float) Math.cos(angle);
            boolean front = cos >= 0;
            float shade = 0.45f + 0.55f * Math.abs(cos);
            float alpha = a * 0.30f * (1f - i * 0.35f);

            ms.push();
            ms.translate(cx, cy, 0);
            ms.scale(Math.max(0.03f, Math.abs(cos)), 1f, 1f);

            RenderSystem.setShaderColor(shade, shade * 0.82f, shade * 0.82f, alpha);
            int half = size / 2;
            ctx.drawTexture(skin, -half, -half, size, size, front ? 8 : 24, 8, 8, 8, 64, 64);
            // capa exterior (casco/pelo), ligeramente más grande
            int big = (int) (size * 1.1f);
            ctx.drawTexture(skin, -big / 2, -big / 2, big, big, front ? 40 : 56, 8, 8, 8, 64, 64);
            ms.pop();
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------- calavera
    private static void drawSkull(DrawContext ctx, int w, int h, float a, long t) {
        int tick = (int) (t / 50L);
        int frame = -1;
        int acc = 0;
        for (int[] step : TIMELINE) {
            if (tick < acc + step[1]) {
                frame = step[0];
                break;
            }
            acc += step[1];
        }
        if (frame < 0) return;

        float k = 1f;
        if (tick < 6) k = tick / 6f;
        else if (tick > TIMELINE_TICKS - 10) k = (TIMELINE_TICKS - tick) / 10f;
        float alpha = clamp(a * k);

        int size = (int) (Math.min(w, h) * 0.62f * (1f + 0.02f * (float) Math.sin(t / 60.0)));
        int x = (w - size) / 2;
        int y = (int) (h * 0.46f - size / 2f);

        boolean glitch = frame == 2 || frame == 4;
        float noise = ((tick * 7919) % 13 - 6) / 6f;   // -1 .. 1, determinista
        if (glitch) {
            x += (int) (noise * size * 0.03f);
            y += (int) (-noise * size * 0.01f);
        }

        RenderSystem.enableBlend();
        if (glitch) {
            int off = (int) (size * 0.025f);
            RenderSystem.setShaderColor(1f, 0.15f, 0.15f, alpha * 0.55f);
            ctx.drawTexture(SKULL[frame], x - off, y, size, size, 0, 0, 256, 256, 256, 256);
            RenderSystem.setShaderColor(0.15f, 1f, 1f, alpha * 0.55f);
            ctx.drawTexture(SKULL[frame], x + off, y, size, size, 0, 0, 256, 256, 256, 256);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawTexture(SKULL[frame], x, y, size, size, 0, 0, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    // ---------------------------------------------------------------- texto
    private static void drawText(DrawContext ctx, int w, int h, float a, long t) {
        if (t < 1200) return;
        float ta = clamp(a * clamp((t - 1200) / 600f));
        int alpha = Math.max(5, (int) (ta * 255));
        MinecraftClient mc = MinecraftClient.getInstance();
        Text text = Text.literal(self ? "HAS MUERTO" : playerName.toUpperCase() + " HA CAÍDO");

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(w / 2f, h * 0.80f, 0);
        ms.scale(2f, 2f, 1f);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, text, 0, 0, (alpha << 24) | 0xAA0000);
        ms.pop();
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
