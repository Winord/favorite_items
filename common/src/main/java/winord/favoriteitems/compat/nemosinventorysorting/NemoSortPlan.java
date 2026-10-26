package winord.favoriteitems.compat.nemosinventorysorting;

import net.minecraft.world.inventory.AbstractContainerMenu;
import winord.favoriteitems.util.FavoriteSlots;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NemoSortPlan {
    private NemoSortPlan() { }

    public static Map<Integer, Integer> swapMap(AbstractContainerMenu menu, List<?> slotItems, int startIndex, int endIndex) {
        var result = new LinkedHashMap<Integer, Integer>();

        int destination = startIndex;
        for (var item : slotItems) {
            while (destination < endIndex && FavoriteSlots.isFavorite(menu, destination)) destination++;
            if (destination >= endIndex) break;

            int current = NemoReflect.slotIndexOf(item);
            if (current != destination) result.put(current, destination);
            destination++;
        }
        return result;
    }
}
