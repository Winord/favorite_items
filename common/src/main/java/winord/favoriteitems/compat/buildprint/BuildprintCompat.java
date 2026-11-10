package winord.favoriteitems.compat.buildprint;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import winord.favoriteitems.util.ProtectGuard;

public final class BuildprintCompat {
    private BuildprintCompat() { }

    public static ItemStack visibleItem(Inventory inventory, int index, ItemStack stack) {
        return isHidden(inventory, index, stack) ? ItemStack.EMPTY : stack;
    }

    public static ItemStack visibleSelectedItem(Inventory inventory) {
        return visibleItem(inventory, inventory.getSelectedSlot(), inventory.getSelectedItem());
    }

    private static boolean isHidden(Inventory inventory, int index, ItemStack stack) {
        if (stack.isEmpty()) return false;

        var player = Minecraft.getInstance().player;
        return player != null && inventory == player.getInventory() && ProtectGuard.hidesFromBuilding(player, index, stack);
    }
}
