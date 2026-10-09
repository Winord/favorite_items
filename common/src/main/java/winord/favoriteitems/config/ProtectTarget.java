package winord.favoriteitems.config;

import winord.favoriteitems.util.FavoriteSlots;

public enum ProtectTarget {
    FAVORITES,
    ALL_ITEMS;

    public boolean covers(int inventoryIndex) {
        return this == ALL_ITEMS || FavoriteSlots.isFavorite(inventoryIndex);
    }
}
