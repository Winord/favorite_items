package winord.favoriteitems.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.config.FavoriteStyle;
import winord.favoriteitems.config.StyleSettings;

import java.util.EnumMap;
import java.util.Map;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class RenderUtil {
    private static final int ICON_SIZE = 18;
    private static final int ICON_INSET = 1;
    private static final float ABOVE_ITEM_Z = 300f;

    private static final Map<FavoriteStyle, ResourceLocation> MASKS = new EnumMap<>(FavoriteStyle.class);
    private static final Map<FavoriteStyle, ResourceLocation> OVERLAYS = new EnumMap<>(FavoriteStyle.class);

    static {
        for (var style : FavoriteStyle.values()) {
            MASKS.put(style, ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/" + style.id() + "_mask.png"));
            OVERLAYS.put(style, ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/" + style.id() + "_overlay.png"));
        }
    }

    private RenderUtil() { }

    public static void renderActiveIcon(GuiGraphics graphics, boolean above, int x, int y, float alpha) {
        renderIcon(graphics, FavoriteItemsConfig.activeStyle, FavoriteItemsConfig.activeSettings(), above, x, y, alpha, false);
    }

    public static void renderIcon(GuiGraphics graphics, FavoriteStyle style, StyleSettings settings, boolean above,
                                  int x, int y, float alpha, boolean depthTested) {
        if (!settings.drawsLayers(above)) return;

        boolean dyeable = settings.dyeableEnabled && settings.dyeableAboveItem == above;
        boolean fixed = settings.staticEnabled && settings.staticAboveItem == above;
        float dyeableAlpha = alpha * settings.dyeableAlpha();
        float staticAlpha = alpha * settings.staticAlpha();

        var mask = MASKS.get(style);
        var overlay = OVERLAYS.get(style);
        int px = x - ICON_INSET, py = y - ICON_INSET;

        int tint = settings.dyeableColor & 0xFFFFFF;
        float zShift = above && !depthTested ? ABOVE_ITEM_Z : 0f;

        var pose = graphics.pose();
        pose.pushPose();
        if (zShift != 0f) pose.translate(0f, 0f, zShift);

        if (settings.shadowEnabled) {
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0f);
            if (dyeable) blitLayer(graphics, mask, px, py, 0, dyeableAlpha * 0.25f);
            if (fixed) blitLayer(graphics, overlay, px, py, 0, staticAlpha * 0.25f);
            pose.popPose();
        }

        if (dyeable && fixed && !settings.staticAboveDyeable) {
            blitLayer(graphics, overlay, px, py, 0xFFFFFF, staticAlpha);
            blitLayer(graphics, mask, px, py, tint, dyeableAlpha);
        } else {
            if (dyeable) blitLayer(graphics, mask, px, py, tint, dyeableAlpha);
            if (fixed) blitLayer(graphics, overlay, px, py, 0xFFFFFF, staticAlpha);
        }

        pose.popPose();
    }

    private static void blitLayer(GuiGraphics graphics, ResourceLocation texture, int px, int py, int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        graphics.blit(RenderType::guiTextured, texture, px, py, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, (a << 24) | rgb);
    }
}
