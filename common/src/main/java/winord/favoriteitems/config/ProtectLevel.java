package winord.favoriteitems.config;

import java.util.Locale;

public enum ProtectLevel {
    NO,
    LAST_ITEM,
    FULL;

    public boolean covers(int count) {
        return switch (this) {
            case NO -> false;
            case LAST_ITEM -> count == 1;
            case FULL -> true;
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ProtectLevel byId(String id) {
        if (id == null) return null;

        for (var level : values()) {
            if (level.id().equalsIgnoreCase(id.trim())) return level;
        }
        return null;
    }
}
