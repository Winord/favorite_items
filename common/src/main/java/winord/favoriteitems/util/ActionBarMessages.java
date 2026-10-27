package winord.favoriteitems.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.config.ProtectAction;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class ActionBarMessages {
    private ActionBarMessages() { }

    public static void dropBlocked() {
        show(Component.translatable("message." + MOD_ID + ".drop_blocked"));
    }

    public static void protectBlocked(ProtectAction action) {
        show(Component.translatable("message." + MOD_ID + ".protected", Component.translatable(action.labelKey())));
    }

    public static void toolBlocked(String toggleLabelKey) {
        show(Component.translatable("message." + MOD_ID + ".tool_blocked", Component.translatable(toggleLabelKey)));
    }

    public static void toolBreakBlocked(ItemStack stack) {
        show(Component.translatable("message." + MOD_ID + ".tool_break_blocked", stack.getHoverName()));
    }

    public static void toggled(String labelKey, boolean on) {
        var state = (on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF)
                .copy().withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
        show(Component.translatable("message." + MOD_ID + ".toggled", Component.translatable(labelKey), state));
    }

    private static void show(Component message) {
        if (!FavoriteItemsConfig.actionBarNotifications) return;

        var gui = Minecraft.getInstance().gui;
        if (gui != null) gui.hud.setOverlayMessage(message, false);
    }
}
