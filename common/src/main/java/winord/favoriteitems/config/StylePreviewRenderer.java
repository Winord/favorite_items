package winord.favoriteitems.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import winord.favoriteitems.util.RenderUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class StylePreviewRenderer {
    public static final int SLOT_FRAME_SIZE = 18;

    private static final Identifier SLOT_BACKGROUND =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/slot_background.png");

    private static final Identifier SAMPLE_ITEM = Identifier.withDefaultNamespace("textures/item/diamond_sword.png");

    public record Data(FavoriteStyle style, Supplier<StyleSettings> settings) { }

    private static final Map<FavoriteStyle, Data> LIVE = new EnumMap<>(FavoriteStyle.class);

    private StylePreviewRenderer() { }

    public static void clearLive() {
        LIVE.clear();
    }

    public static void registerLive(Data data) {
        LIVE.put(data.style(), data);
    }

    public static Data dataFor(FavoriteStyle style) {
        var live = LIVE.get(style);
        return live != null ? live : new Data(style, () -> FavoriteItemsConfig.settingsFor(style));
    }

    public static void drawSlot(GuiGraphicsExtractor graphics, Data data, int x, int y) {
        var settings = data.settings().get();
        int ix = x + 1, iy = y + 1;

        graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND, x, y, 0f, 0f, SLOT_FRAME_SIZE, SLOT_FRAME_SIZE, SLOT_FRAME_SIZE, SLOT_FRAME_SIZE, -1);

        if (settings.backgroundEnabled) {
            graphics.fill(ix, iy, ix + 16, iy + 16, settings.backgroundArgb());
        }

        RenderUtil.renderIcon(graphics, data.style(), settings, false, ix, iy, 1f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SAMPLE_ITEM, ix, iy, 0f, 0f, 16, 16, 16, 16, -1);
        RenderUtil.renderIcon(graphics, data.style(), settings, true, ix, iy, 1f);
    }
}
