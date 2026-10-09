package winord.favoriteitems.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import winord.favoriteitems.util.RenderUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class StylePreviewRenderer {
    public static final int SLOT_FRAME_SIZE = 18;

    private static final float UNDER_ICON_Z = 50f;
    private static final float OVER_ICON_Z = 200f;
    private static final ResourceLocation SLOT_BACKGROUND =
            new ResourceLocation(MOD_ID, "textures/gui/slot_background.png");

    public record Data(FavoriteStyle style, Supplier<StyleSettings> settings) { }

    private static final Map<FavoriteStyle, Data> LIVE = new EnumMap<>(FavoriteStyle.class);

    private static ItemStack sampleItem;

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

    public static void drawSlot(GuiGraphics graphics, Data data, int x, int y) {
        if (sampleItem == null) sampleItem = new ItemStack(Items.DIAMOND_SWORD);

        var settings = data.settings().get();
        int ix = x + 1, iy = y + 1;

        graphics.blit(SLOT_BACKGROUND, x, y, 0, 0, SLOT_FRAME_SIZE, SLOT_FRAME_SIZE, SLOT_FRAME_SIZE, SLOT_FRAME_SIZE);

        if (settings.backgroundEnabled) {
            graphics.fill(ix, iy, ix + 16, iy + 16, settings.backgroundArgb());
        }

        drawIcon(graphics, data.style(), settings, false, ix, iy, UNDER_ICON_Z);
        graphics.renderItem(sampleItem, ix, iy);
        drawIcon(graphics, data.style(), settings, true, ix, iy, OVER_ICON_Z);
    }

    private static void drawIcon(GuiGraphics graphics, FavoriteStyle style, StyleSettings settings, boolean above,
                                 int x, int y, float z) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0f, 0f, z);
        RenderUtil.renderIcon(graphics, style, settings, above, x, y, 1f, true);
        pose.popPose();
    }
}
