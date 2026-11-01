package winord.favoriteitems.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
            if (!isBackpackSlot(slot) || !FavoriteSlots.isFavorite(FavoriteSlots.playerInventoryIndex(slot))) continue;

            var held = slot.getItem();
            int space = slot.getMaxStackSize(held) - held.getCount();
            if (space > 0 && ItemStack.isSameItemSameComponents(held, product)) {
                favorites.add(slot);
                free += space;
            }
        }
        if (free <= 0) return false;

        int craftable = craftableTimes(menu);
        if (craftable <= 0) return false;

        int crafts = Math.min((free + yield - 1) / yield, craftable);
        int overshoot = crafts * yield - free;
        if (overshoot > 0 && overshoot > overflowRoom(menu, product, maxStack)) crafts--;
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

            deposit(menu, favorites);
            if (!menu.getCarried().isEmpty()) deposit(menu, overflowSlots(menu, product));
            remaining -= crafted;
        }
        return true;
    }

    private static boolean isBackpackSlot(Slot slot) {
        int index = FavoriteSlots.playerInventoryIndex(slot);
        return index >= 0 && index < FavoriteSlots.ARMOR_START_INDEX;
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

    private static List<Slot> overflowSlots(AbstractContainerMenu menu, ItemStack product) {
        var partial = new ArrayList<Slot>();
        var empty = new ArrayList<Slot>();
        for (int i = menu.slots.size() - 1; i >= 0; i--) {
            var slot = menu.slots.get(i);
            if (!isBackpackSlot(slot)) continue;

            int index = FavoriteSlots.playerInventoryIndex(slot);
            var held = slot.getItem();
            if (held.isEmpty()) {
                if (!FavoriteSlots.isMarked(index)) empty.add(slot);
            } else if (!FavoriteSlots.isFavorite(index) && held.getCount() < slot.getMaxStackSize(held)
                    && ItemStack.isSameItemSameComponents(held, product)) {
                partial.add(slot);
            }
        }
        partial.addAll(empty);
        return partial;
    }

    private static int overflowRoom(AbstractContainerMenu menu, ItemStack product, int maxStack) {
        int room = 0;
        for (var slot : overflowSlots(menu, product)) {
            room += slot.hasItem() ? slot.getMaxStackSize(slot.getItem()) - slot.getItem().getCount() : maxStack;
        }
        return room;
    }

    private static void deposit(AbstractContainerMenu menu, List<Slot> targets) {
        for (var slot : targets) {
            var carried = menu.getCarried();
            if (carried.isEmpty()) return;

            var held = slot.getItem();
            if (held.isEmpty() ? FavoriteSlots.isMarked(FavoriteSlots.playerInventoryIndex(slot))
                    : held.getCount() >= slot.getMaxStackSize(held) || !ItemStack.isSameItemSameComponents(held, carried)) {
                continue;
            }
            SlotClicks.click(menu, slot.index, 0);
        }
    }
}
