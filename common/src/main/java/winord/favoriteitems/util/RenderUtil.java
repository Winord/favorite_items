package winord.favoriteitems.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.config.FavoriteStyle;
import winord.favoriteitems.config.StyleSettings;

import java.util.EnumMap;
import java.util.Map;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class RenderUtil {
    private static final int ICON_SIZE = 18;
    private static final int ICON_INSET = 1;

    private static final Map<FavoriteStyle, Identifier> MASKS = new EnumMap<>(FavoriteStyle.class);
    private static final Map<FavoriteStyle, Identifier> OVERLAYS = new EnumMap<>(FavoriteStyle.class);

    static {
        for (var style : FavoriteStyle.values()) {
            MASKS.put(style, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/" + style.id() + "_mask.png"));
            OVERLAYS.put(style, Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/" + style.id() + "_overlay.png"));
        }
    }

    private RenderUtil() { }

    public static void renderActiveIcon(GuiGraphicsExtractor graphics, boolean above, int x, int y, float alpha) {
        renderIcon(graphics, FavoriteItemsConfig.activeStyle, FavoriteItemsConfig.activeSettings(), above, x, y, alpha);
    }

    public static void renderIcon(GuiGraphicsExtractor graphics, FavoriteStyle style, StyleSettings settings, boolean above,
                                  int x, int y, float alpha) {
        if (!settings.drawsLayers(above)) return;

        boolean dyeable = settings.dyeableEnabled && settings.dyeableAboveItem == above;
        boolean fixed = settings.staticEnabled && settings.staticAboveItem == above;
        float dyeableAlpha = alpha * settings.dyeableAlpha();
        float staticAlpha = alpha * settings.staticAlpha();

        var mask = MASKS.get(style);
        var overlay = OVERLAYS.get(style);
        int px = x - ICON_INSET, py = y - ICON_INSET;

        int tint = settings.dyeableColor & 0xFFFFFF;
        var pose = graphics.pose();

        if (settings.shadowEnabled) {
            pose.pushMatrix();
            pose.translate(0.5f, 0.5f);
            if (dyeable) blitLayer(graphics, mask, px, py, 0, dyeableAlpha * 0.25f);
            if (fixed) blitLayer(graphics, overlay, px, py, 0, staticAlpha * 0.25f);
            pose.popMatrix();
        }

        if (dyeable && fixed && !settings.staticAboveDyeable) {
            blitLayer(graphics, overlay, px, py, 0xFFFFFF, staticAlpha);
            blitLayer(graphics, mask, px, py, tint, dyeableAlpha);
        } else {
            if (dyeable) blitLayer(graphics, mask, px, py, tint, dyeableAlpha);
            if (fixed) blitLayer(graphics, overlay, px, py, 0xFFFFFF, staticAlpha);
        }
    }

    private static void blitLayer(GuiGraphicsExtractor graphics, Identifier texture, int px, int py, int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, px, py, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, (a << 24) | rgb);
    }
}
