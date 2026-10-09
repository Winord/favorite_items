package winord.favoriteitems.config;

import java.util.Locale;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public enum FavoriteStyle {
    STAR,
    TERRARIA,
    LOCK,
    BOOKMARK,
    LEAF,
    GEM,
    HEART,
    CIRCLE,
    MINIMAL,
    CUSTOM;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String langKey() {
        return "config." + MOD_ID + ".style." + id();
    }
}
