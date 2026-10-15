package winord.favoriteitems.config;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class StylePreviewOverlay {
    public enum Tab { GENERAL, STYLES, OTHER }

    public interface ListAccess {
        int top();
        int bottom();
        void setBounds(int left, int width);
        Object sectionAt(int mouseY);
        void closeDropdownsOutside(double mouseX, double mouseY);
        int labelLimit(Object entry);
        void paintFrame(GuiGraphicsExtractor graphics, Object widget);
        void syncVisibility();
    }

    private record Section(FavoriteStyle style, Object entry, BooleanSupplier expanded) { }

    private static final int SIDE_MARGIN = 6;
    private static final int LARGE_PREVIEW_MIN_WIDTH = 700;
    private static final int PANEL_TOP_GAP = 12;
    private static final int CLOTH_SCROLLBAR_MARGIN = 30;
    private static final int SCROLLBAR_GAP = 3;
    private static final int PADDING = 4;
    private static final int PANEL_BACKGROUND = 0xFF0A0805;
    private static final int DIVIDER = 0x40FFFFFF;
    private static final int BACKGROUND = 0xF0100010;
    private static final int BORDER_TOP = 0x505000FF;
    private static final int BORDER_BOTTOM = 0x5028007F;
    private static final String ELLIPSIS = "...";

    private static final float MARQUEE_SPEED = 24f;
    private static final long MARQUEE_PAUSE_MS = 800;
    private static final int ROW_HEIGHT = 20;
    private static final long ROW_STALE_MS = 100;
    private static final long MARQUEE_GAP_MS = 250;

    private static WeakReference<Screen> screen = new WeakReference<>(null);
    private static Supplier<Tab> tab = () -> Tab.OTHER;
    private static Supplier<FavoriteStyle> activeStyle = () -> FavoriteItemsConfig.activeStyle;
    private static Supplier<ListAccess> listAccess = () -> null;
    private static final List<Section> sections = new ArrayList<>();
    private static FavoriteStyle current;
    private static Tab lastTab;

    private static Object rowEntry;
    private static GuiGraphicsExtractor rowGraphics;
    private static int rowX, rowY, rowMouseX, rowMouseY;
    private static long rowStamp;
    private static Object marqueeEntry;
    private static long marqueeStart, marqueeSeen;

    private static boolean panelActive;
    private static int panelLeft, panelTop, panelRight, panelBottom;
    private static int boxWidth, previewScale;

    private StylePreviewOverlay() { }

    public static void clearSections() {
        sections.clear();
    }

    public static void registerSection(FavoriteStyle style, Object entry, BooleanSupplier expanded) {
        sections.add(new Section(style, entry, expanded));
    }

    public static void attach(Screen configScreen, Supplier<Tab> tabSupplier,
                              Supplier<FavoriteStyle> activeStyleSupplier, Supplier<ListAccess> listSupplier) {
        screen = new WeakReference<>(configScreen);
        tab = tabSupplier;
        activeStyle = activeStyleSupplier;
        listAccess = listSupplier;
        current = null;
        lastTab = null;
        panelActive = false;
        rowEntry = null;
        rowGraphics = null;
        marqueeEntry = null;
    }

    public static void beginRow(Object entry, GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        rowEntry = entry;
        rowGraphics = graphics;
        rowX = x;
        rowY = y;
        rowMouseX = mouseX;
        rowMouseY = mouseY;
        rowStamp = Util.getMillis();
    }

    public static void onInit(Screen target) {
        if (target != screen.get()) return;

        var list = listAccess.get();
        if (list == null) return;

        list.syncVisibility();
        panelActive = false;
        list.setBounds(0, target.width);
        if (tab.get() == Tab.OTHER) return;

        var font = Minecraft.getInstance().font;
        previewScale = target.width >= LARGE_PREVIEW_MIN_WIDTH ? 4 : 3;
        int slotPx = StylePreviewRenderer.SLOT_FRAME_SIZE * previewScale;
        int widestTitle = 0;
        for (var style : FavoriteStyle.values()) {
            widestTitle = Math.max(widestTitle, font.width(titleOf(style)));
        }
        boxWidth = Math.max(widestTitle, slotPx) + PADDING * 2;
        int panelWidth = boxWidth + SIDE_MARGIN * 2;

        panelTop = list.top();
        panelBottom = list.bottom();
        panelLeft = target.width - panelWidth;
        panelRight = target.width;
        list.setBounds(0, panelLeft + CLOTH_SCROLLBAR_MARGIN - SCROLLBAR_GAP);
        panelActive = true;
    }

    public static void onMouseClicked(Screen target, double mouseX, double mouseY) {
        if (target != screen.get()) return;

        var list = listAccess.get();
        if (list != null) list.closeDropdownsOutside(mouseX, mouseY);
    }

    public static void render(Screen target, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (target != screen.get()) return;

        var currentTab = tab.get();
        if (currentTab != lastTab) {
            lastTab = currentTab;
            current = null;
        }

        var style = switch (currentTab) {
            case GENERAL -> activeStyle.get();
            case STYLES -> stylesTabStyle(mouseY);
            case OTHER -> null;
        };
        if (style == null || !panelActive) return;

        draw(graphics, style);
    }

    private static FavoriteStyle stylesTabStyle(int mouseY) {
        var list = listAccess.get();
        if (list != null) {
            var hovered = list.sectionAt(mouseY);
            if (hovered != null) {
                for (var section : sections) {
                    if (section.entry() != hovered) continue;
                    if (section.expanded().getAsBoolean()) current = section.style();
                    break;
                }
            }
        }
        if (current == null) current = activeStyle.get();
        return current;
    }

    public static void paintListFrame(Object widget, GuiGraphicsExtractor graphics) {
        var ours = screen.get();
        if (ours == null || Minecraft.getInstance().screen != ours || !panelActive) return;

        var list = listAccess.get();
        if (list != null) list.paintFrame(graphics, widget);
    }

    public static Component fitLabel(Object entry, Component label) {
        var ours = screen.get();
        if (ours == null || Minecraft.getInstance().screen != ours) return label;

        var list = listAccess.get();
        if (list == null) return label;

        int limit = list.labelLimit(entry);
        if (limit <= 0) return label;

        var font = Minecraft.getInstance().font;
        int textWidth = font.width(label);
        if (textWidth <= limit) return label;

        if (labelHovered(entry, list, limit)) {
            drawMarquee(entry, list, label, textWidth, limit);
            return Component.empty();
        }
        if (marqueeEntry == entry) marqueeEntry = null;

        int textLimit = limit - font.width(ELLIPSIS);
        if (textLimit <= 0) return label;

        var cut = font.plainSubstrByWidth(label.getString(), textLimit);
        return Component.literal(cut + ELLIPSIS).setStyle(label.getStyle());
    }

    private static boolean labelHovered(Object entry, ListAccess list, int limit) {
        if (rowEntry != entry || rowGraphics == null) return false;
        if (Util.getMillis() - rowStamp > ROW_STALE_MS) return false;

        return rowMouseX >= rowX && rowMouseX < rowX + limit
                && rowMouseY >= rowY && rowMouseY < rowY + ROW_HEIGHT
                && rowMouseY >= list.top() && rowMouseY < list.bottom();
    }

    private static void drawMarquee(Object entry, ListAccess list, Component label, int textWidth, int limit) {
        var font = Minecraft.getInstance().font;
        long now = Util.getMillis();
        if (marqueeEntry != entry || now - marqueeSeen > MARQUEE_GAP_MS) {
            marqueeEntry = entry;
            marqueeStart = now;
        }
        marqueeSeen = now;

        float offset = marqueeOffset(now - marqueeStart, textWidth - limit);

        rowGraphics.enableScissor(rowX, rowY + 3, rowX + limit, rowY + 3 + font.lineHeight + 5);
        var pose = rowGraphics.pose();
        pose.pushMatrix();
        pose.translate(-offset, 0f);
        rowGraphics.text(font, label, rowX, rowY + 6, 0xFFFFFFFF, true);
        pose.popMatrix();
        rowGraphics.disableScissor();
    }

    private static float marqueeOffset(long elapsedMs, float overflow) {
        float travelMs = overflow * 1000f / MARQUEE_SPEED;
        float cycle = 2f * (MARQUEE_PAUSE_MS + travelMs);
        float phase = elapsedMs % cycle;

        if (phase < MARQUEE_PAUSE_MS) return 0f;
        phase -= MARQUEE_PAUSE_MS;
        if (phase < travelMs) return overflow * phase / travelMs;
        phase -= travelMs;
        if (phase < MARQUEE_PAUSE_MS) return overflow;
        phase -= MARQUEE_PAUSE_MS;
        return overflow * (1f - phase / travelMs);
    }

    private static Component titleOf(FavoriteStyle style) {
        return Component.translatable(style.langKey());
    }

    private static void draw(GuiGraphicsExtractor graphics, FavoriteStyle style) {
        var font = Minecraft.getInstance().font;
        var title = titleOf(style);

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, PANEL_BACKGROUND);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, DIVIDER);

        int panelWidth = panelRight - panelLeft;
        int slotPx = StylePreviewRenderer.SLOT_FRAME_SIZE * previewScale;
        int width = boxWidth;
        int height = PADDING + font.lineHeight + PADDING + slotPx + PADDING;

        int left = panelLeft + (panelWidth - width) / 2;
        int top = panelTop + PANEL_TOP_GAP;

        graphics.fill(left + 1, top, left + width - 1, top + height, BACKGROUND);
        graphics.fill(left, top + 1, left + width, top + height - 1, BACKGROUND);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 2, BORDER_TOP);
        graphics.fill(left + 1, top + 2, left + 2, top + height - 2, BORDER_TOP);
        graphics.fill(left + 1, top + height - 2, left + width - 1, top + height - 1, BORDER_BOTTOM);
        graphics.fill(left + width - 2, top + 2, left + width - 1, top + height - 2, BORDER_BOTTOM);

        graphics.text(font, title, left + (width - font.width(title)) / 2, top + PADDING, 0xFFFFFFFF, false);

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(left + (width - slotPx) / 2f, top + PADDING + font.lineHeight + PADDING);
        pose.scale(previewScale, previewScale);
        StylePreviewRenderer.drawSlot(graphics, StylePreviewRenderer.dataFor(style), 0, 0);
        pose.popMatrix();
    }
}