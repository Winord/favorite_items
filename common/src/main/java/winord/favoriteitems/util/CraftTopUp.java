package winord.favoriteitems.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public final class CraftTopUp {
    private CraftTopUp() { }

    public static boolean run(AbstractContainerMenu menu, Slot result) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || minecraft.player == null || !menu.getCarried().isEmpty() || !result.hasItem()) return false;

        var product = result.getItem().copy();
        int yield = product.getCount();
        int maxStack = product.getMaxStackSize();
        if (maxStack <= 1) return false;

        var favorites = new ArrayList<Slot>();
        int free = 0;
        for (var slot : menu.slots) {
            int index = FavoriteSlots.playerInventoryIndex(slot);
            if (index < 0 || index >= FavoriteSlots.ARMOR_START_INDEX || !FavoriteSlots.isFavorite(index)) continue;

            var held = slot.getItem();
            int space = slot.getMaxStackSize(held) - held.getCount();
            if (space > 0 && ItemStack.isSameItemSameComponents(held, product)) {
                favorites.add(slot);
                free += space;
            }
        }

        int crafts = Math.min(free / yield, craftableTimes(menu));
        if (crafts <= 0) return false;

        for (int remaining = crafts; remaining > 0; ) {
            int batch = Math.min(remaining, Math.max(1, maxStack / yield));
            int crafted = 0;
            for (int i = 0; i < batch; i++) {
                if (!result.hasItem()) result.set(product.copy());

                int before = menu.getCarried().getCount();
                SlotClicks.click(menu, result.index, 0);
                if (menu.getCarried().getCount() <= before) break;
                crafted++;
            }
            if (crafted == 0) break;

            for (var slot : favorites) {
                if (menu.getCarried().isEmpty()) break;

                var held = slot.getItem();
                if (held.getCount() < slot.getMaxStackSize(held) && ItemStack.isSameItemSameComponents(held, menu.getCarried())) {
                    SlotClicks.click(menu, slot.index, 0);
                }
            }
            remaining -= crafted;
        }
        return true;
    }

    private static int craftableTimes(AbstractContainerMenu menu) {
        int times = Integer.MAX_VALUE;
        boolean hasGrid = false;
        for (var slot : menu.slots) {
            if (slot.container instanceof CraftingContainer && slot.hasItem()) {
                times = Math.min(times, slot.getItem().getCount());
                hasGrid = true;
            }
        }
        return hasGrid ? times : 0;
    }
}
