package winord.favoriteitems.compat.buildprint;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import winord.favoriteitems.util.FavoriteSlots;
import winord.favoriteitems.util.ProtectGuard;
import winord.favoriteitems.util.ToolGuard;

public final class BuildprintCompat {
    private BuildprintCompat() { }

    public static ItemStack visibleItem(Inventory inventory, int index, ItemStack stack) {
        return isHidden(inventory, index, stack) ? ItemStack.EMPTY : stack;
    }

    public static ItemStack visibleSelectedItem(Inventory inventory) {
        return visibleItem(inventory, inventory.getSelectedSlot(), inventory.getSelectedItem());
    }

    public static ItemStack assistVisibleItem(Inventory inventory, int index, ItemStack stack) {
        if (stack.isEmpty()) return stack;

        var player = Minecraft.getInstance().player;
        boolean hidden = player != null && inventory == player.getInventory() && ProtectGuard.hidesFromAssist(player, index, stack);
        return hidden ? ItemStack.EMPTY : stack;
    }

    public static int findSourceOutsideFavorites(AbstractContainerMenu menu, ItemStack want) {
        for (var slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) continue;

            var stack = slot.getItem();
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, want)) continue;
            if (FavoriteSlots.isFavorite(menu, slot.index)) continue;

            return slot.index;
        }
        return -1;
    }

    public static boolean toolWouldBreakOn(ItemStack stack, BlockState state) {
        return ToolGuard.wouldBreakMining(stack, state);
    }

    private static boolean isHidden(Inventory inventory, int index, ItemStack stack) {
        if (stack.isEmpty()) return false;

        var player = Minecraft.getInstance().player;
        return player != null && inventory == player.getInventory() && ProtectGuard.hidesFromBuilding(player, index, stack);
    }
}
