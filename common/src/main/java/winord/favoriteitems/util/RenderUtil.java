package winord.favoriteitems.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
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

    private static final Map<FavoriteStyle, ResourceLocation> MASKS = new EnumMap<>(FavoriteStyle.class);
    private static final Map<FavoriteStyle, ResourceLocation> OVERLAYS = new EnumMap<>(FavoriteStyle.class);

    static {
        for (var style : FavoriteStyle.values()) {
            MASKS.put(style, new ResourceLocation(MOD_ID, "textures/gui/" + style.id() + "_mask.png"));
            OVERLAYS.put(style, new ResourceLocation(MOD_ID, "textures/gui/" + style.id() + "_overlay.png"));
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

        int tint = settings.dyeableColor;
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;

        if (!depthTested) RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (settings.shadowEnabled) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0f);
            if (dyeable) blitLayer(graphics, mask, px, py, 0f, 0f, 0f, dyeableAlpha * 0.25f);
            if (fixed) blitLayer(graphics, overlay, px, py, 0f, 0f, 0f, staticAlpha * 0.25f);
            pose.popPose();
        }

        if (dyeable && fixed && !settings.staticAboveDyeable) {
            blitLayer(graphics, overlay, px, py, 1f, 1f, 1f, staticAlpha);
            blitLayer(graphics, mask, px, py, r, g, b, dyeableAlpha);
        } else {
            if (dyeable) blitLayer(graphics, mask, px, py, r, g, b, dyeableAlpha);
            if (fixed) blitLayer(graphics, overlay, px, py, 1f, 1f, 1f, staticAlpha);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void blitLayer(GuiGraphics graphics, ResourceLocation texture, int px, int py,
                                  float r, float g, float b, float alpha) {
        RenderSystem.setShaderColor(r, g, b, alpha);
        graphics.blit(texture, px, py, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }
}
