package winord.favoriteitems.compat.inventorysorter;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import winord.favoriteitems.util.FavoriteSlots;

import java.lang.reflect.Method;
import java.util.List;

public final class InventorySorterCompat {
    private static boolean[] favoritePositions;
    private static int depth;
    private static boolean splitting;
    private static int splitIndex;

    private static Method scopedSlotAccessor;
    private static boolean accessorFailed;

    private InventorySorterCompat() { }

    public static void beginContainer(Container container, int startSlot, int size) {
        var positions = new boolean[Math.max(size, 0)];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = FavoriteSlots.isFavorite(FavoriteSlots.resolveIndex(container, startSlot + i));
        }
        begin(positions);
    }

    public static void beginClientSlots(List<?> scopedSlots) {
        if (favoritePositions != null) return;

        var positions = new boolean[scopedSlots.size()];
        for (int i = 0; i < positions.length; i++) {
            var slot = slotOf(scopedSlots.get(i));
            positions[i] = slot != null
                    && FavoriteSlots.isFavorite(FavoriteSlots.resolveIndex(slot.container, slot.getContainerSlot()));
        }
        favoritePositions = positions;
    }

    public static void beginClientBuild() {
        favoritePositions = null;
        depth++;
    }

    public static void end() {
        if (depth > 0 && --depth == 0) clear();
    }

    private static void begin(boolean[] positions) {
        depth++;
        favoritePositions = positions;
    }

    public static void beginSplit() {
        splitting = true;
        splitIndex = 0;
    }

    public static void endSplit() {
        splitting = false;
    }

    public static boolean ignoresNextStack() {
        if (!splitting) return false;

        int index = splitIndex++;
        return favoritePositions != null && index < favoritePositions.length && favoritePositions[index];
    }

    public static void resetState() {
        clear();
        depth = 0;
    }

    private static void clear() {
        favoritePositions = null;
        splitting = false;
        splitIndex = 0;
    }

    private static Slot slotOf(Object scopedSlot) {
        if (accessorFailed) return null;

        try {
            if (scopedSlotAccessor == null) {
                scopedSlotAccessor = scopedSlot.getClass().getMethod("slot");
                scopedSlotAccessor.setAccessible(true);
            }
            return (Slot) scopedSlotAccessor.invoke(scopedSlot);
        } catch (ReflectiveOperationException | RuntimeException e) {
            accessorFailed = true;
            winord.favoriteitems.FavoriteItems.LOGGER.warn(
                    "Inventory Sorter is installed, but its client sort scope could not be read; favorites are not kept in place.", e);
            return null;
        }
    }
}
