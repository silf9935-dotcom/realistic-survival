package ru.realisticsurvival.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ru.realisticsurvival.network.SurvivalHudPayload;

public final class RealisticSurvivalClient implements ClientModInitializer {
    private static SurvivalHudPayload latest = new SurvivalHudPayload(100, 100, 100, 100, 100, 370, 0, 0);

    @Override
    public void onInitializeClient() {
        // Replace vanilla hearts and hunger icons with the survival meters; keep armor and hotbar.
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> (graphics, delta) -> {});
        HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, original -> (graphics, delta) -> {});
        ClientPlayNetworking.registerGlobalReceiver(SurvivalHudPayload.TYPE,
                (payload, context) -> latest = payload);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("realistic_survival", "survival_hud"),
                RealisticSurvivalClient::render);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int w = g.guiWidth();
        int h = g.guiHeight();
        SurvivalHudPayload s = latest;

        int left = 10;
        int bottom = h - 12;
        drawMeter(g, mc, left, bottom - 60, 174, "♥", s.health(), 100, 0xFFE51D2A);
        drawMeter(g, mc, left, bottom - 30, 174, "🍖", s.hunger(), 100, 0xFFB97945);
        drawMeter(g, mc, left, bottom, 174, "◆", s.thirst(), 100, 0xFF354BD2);

        int tempY = bottom - 18;
        g.fill(left + 180, tempY - 8, left + 184, tempY + 8, 0xFF171717);
        g.fill(left + 181, tempY - 6, left + 183, tempY + 5, 0xFFE53B35);
        g.drawString(mc.font, String.format(java.util.Locale.ROOT, "%.1f°C", s.temperatureTenths() / 10.0),
                left + 192, tempY - 4, 0xFFFFFFFF, true);

        // Keep the vanilla nine-slot hotbar untouched; stamina is a compact readout above it.
        String stamina = "⚡ " + s.stamina() + "/100";
        int staminaX = w / 2 - mc.font.width(stamina) / 2;
        int staminaY = h - 58;
        g.fill(staminaX - 5, staminaY - 3, staminaX + mc.font.width(stamina) + 5, staminaY + 13, 0x99090912);
        g.drawString(mc.font, stamina, staminaX, staminaY, 0xFF4D9CFF, true);

        String weight = "▣ " + String.format(java.util.Locale.ROOT, "%.1f", s.carryWeightTenths() / 10.0) + "/30 кг";
        g.drawString(mc.font, weight, w - mc.font.width(weight) - 12, h - 42, 0xFFFFD29A, true);
        String sanity = "Мозг " + s.sanity() + "%";
        g.drawString(mc.font, sanity, w - mc.font.width(sanity) - 12, h - 25, 0xFFFF91C8, true);

        // Anonymous body map: red points show affected areas, never injury names.
        int panelW = 104, panelH = 142, px = w - panelW - 10, py = 10;
        g.fill(px, py, px + panelW, py + panelH, 0xB9000000);
        g.fill(px, py, px + panelW, py + 1, 0xFF777777);
        int cx = px + 52;
        g.fill(cx - 9, py + 10, cx + 9, py + 32, 0xFF777777);
        g.fill(cx - 15, py + 35, cx + 15, py + 73, 0xFF777777);
        g.fill(cx - 13, py + 75, cx - 2, py + 119, 0xFF777777);
        g.fill(cx + 2, py + 75, cx + 13, py + 119, 0xFF777777);
        g.fill(cx - 29, py + 37, cx - 17, py + 73, 0xFF777777);
        g.fill(cx + 17, py + 37, cx + 29, py + 73, 0xFF777777);
        int[][] dots = {{cx,py+17},{cx,py+43},{cx-23,py+49},{cx+23,py+49},
                {cx-8,py+65},{cx+8,py+65},{cx-8,py+91},{cx+8,py+91},{cx,py+111}};
        for (int i = 0; i < dots.length; i++) {
            if ((s.injuryMask() & (1 << i)) != 0) {
                g.fill(dots[i][0]-2, dots[i][1]-2, dots[i][0]+3, dots[i][1]+3, 0xFFFF2020);
            }
        }
        g.drawString(mc.font, "ТРАВМЫ", px + 7, py + 126, 0xFFFFFFFF, true);
    }

    private static void drawMeter(GuiGraphics g, Minecraft mc, int x, int y, int width,
                                  String icon, int value, int max, int color) {
        int height = 22;
        g.fill(x, y, x + width, y + height, 0xCC080808);
        g.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFF252525);
        int fill = Math.max(0, Math.min(width - 6, (width - 6) * value / Math.max(1, max)));
        g.fill(x + 3, y + 3, x + 3 + fill, y + height - 3, color);
        g.drawString(mc.font, icon + " " + value + "%", x + 7, y + 7, 0xFFFFFFFF, true);
    }
}
