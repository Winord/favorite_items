package winord.favoriteitems.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import winord.favoriteitems.config.FavoriteItemsConfig;

public final class ExternalClickGuard {
    private ExternalClickGuard() { }

    public static boolean vetoes(Player player, int containerId, int slotId, int button, ClickType clickType) {
        if (clickType != ClickType.QUICK_MOVE && clickType != ClickType.THROW) return false;
        if (FavoriteSlots.isScreenClickActive() || FavoriteSlots.isSuspended()) return false;

        var menu = menuOf(player, containerId);
        if (menu == null || !FavoriteSlots.isFavorite(menu, slotId)) return false;

        if (clickType == ClickType.QUICK_MOVE) return true;

        var stack = menu.getSlot(slotId).getItem();
        return FavoriteItemsConfig.dropFavorites.blocks(stack, button == 0 ? 1 : stack.getCount());
    }

    public static AbstractContainerMenu menuOf(Player player, int containerId) {
        if (player.containerMenu.containerId == containerId) return player.containerMenu;
        if (player.inventoryMenu.containerId == containerId) return player.inventoryMenu;
        return null;
    }
}
