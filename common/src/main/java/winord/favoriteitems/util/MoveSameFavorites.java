package winord.favoriteitems.util;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import winord.favoriteitems.compat.ipn.IpnCompat;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.config.MoveSameFavoritesMode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class MoveSameFavorites {
    public enum Move {
        MATCHING(true, true),
        ALL(false, true),
        REFILL(true, false);

        private final boolean needsMatch;
        private final boolean intoEmptySlots;

        Move(boolean needsMatch, boolean intoEmptySlots) {
            this.needsMatch = needsMatch;
            this.intoEmptySlots = intoEmptySlots;
        }
    }

    private MoveSameFavorites() { }

    public static boolean isAnySortingModLoaded() {
        return Platform.isModLoaded("nemos_inventory_sorting") || Platform.isModLoaded("clientsort")
                || Platform.isModLoaded(IpnCompat.MOD_ID);
    }

    public static boolean leavesOne() {
        return FavoriteItemsConfig.moveSameFavorites == MoveSameFavoritesMode.LEAVE_ONE;
    }

    public static void leaveOneBehind(AbstractContainerMenu menu, Predicate<Slot> inSource) {
        leaveOneBehind(menu, inSource, Move.MATCHING, ItemStack.EMPTY);
    }

    public static void leaveOneBehind(AbstractContainerMenu menu, Predicate<Slot> inSource, Move move, ItemStack focused) {
        if (!leavesOne() || Minecraft.getInstance().player == null) return;

        List<Slot> storage = new ArrayList<>();
        for (var slot : menu.slots) {
            if (!inSource.test(slot) && isStorage(menu, slot)) storage.add(slot);
        }
        if (storage.isEmpty()) return;

        for (var slot : menu.slots) {
            int index = FavoriteSlots.playerInventoryIndex(slot);
            if (index < 0 || index >= FavoriteSlots.ARMOR_START_INDEX || !inSource.test(slot)) continue;

            ItemStack stack = slot.getItem();
            if (stack.getCount() <= 1 || stack.getMaxStackSize() <= 1 || !FavoriteSlots.isFavorite(menu, slot.index)) continue;
            if (!focused.isEmpty() && !ItemStack.isSameItemSameTags(focused, stack)) continue;
            if (move.needsMatch && !hasRoomFor(storage, stack, move.intoEmptySlots)) continue;

            SlotClicks.moveAllButOne(menu, slot.index, storage, move.intoEmptySlots);
        }
    }

    public static void topUpFavorites(AbstractContainerMenu menu, Predicate<Slot> inTarget, ItemStack focused) {
        var player = Minecraft.getInstance().player;
        if (player == null || !menu.getCarried().isEmpty()) return;

        List<Slot> sources = new ArrayList<>();
        for (var slot : menu.slots) {
            if (isStorage(menu, slot) && slot.hasItem() && slot.mayPickup(player)) sources.add(slot);
        }
        if (sources.isEmpty()) return;

        for (var slot : menu.slots) {
            int index = FavoriteSlots.playerInventoryIndex(slot);
            if (index < 0 || index >= FavoriteSlots.ARMOR_START_INDEX || !inTarget.test(slot)) continue;

            ItemStack stack = slot.getItem();
            if (stack.getMaxStackSize() <= 1 || !FavoriteSlots.isFavorite(menu, slot.index)) continue;
            if (!focused.isEmpty() && !ItemStack.isSameItemSameTags(focused, stack)) continue;

            for (var source : sources) {
                if (slot.getItem().getCount() >= slot.getMaxStackSize(slot.getItem())) break;
                if (ItemStack.isSameItemSameTags(source.getItem(), slot.getItem())) {
                    SlotClicks.mergeInto(menu, source.index, slot.index);
                }
            }
        }
    }

    private static boolean hasRoomFor(List<Slot> storage, ItemStack stack, boolean orEmptySlot) {
        boolean holdsItem = false;
        for (var target : storage) {
            ItemStack held = target.getItem();
            if (held.isEmpty() || !ItemStack.isSameItemSameTags(held, stack)) continue;

            holdsItem = true;
            if (held.getCount() < target.getMaxStackSize(held)) return true;
        }
        return holdsItem && orEmptySlot && storage.stream().anyMatch(target -> !target.hasItem());
    }

    private static boolean isStorage(AbstractContainerMenu menu, Slot slot) {
        Container container = slot.container;
        return !(container instanceof Inventory) && !(container instanceof CraftingContainer)
                && (slot.getItem().isEmpty() || slot.mayPlace(slot.getItem()))
                && !FavoriteSlots.isFavorite(menu, slot.index);
    }
}
