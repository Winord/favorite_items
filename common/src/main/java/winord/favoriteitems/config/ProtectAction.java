package winord.favoriteitems.config;

import java.util.Locale;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public enum ProtectAction {
    PLACE_BLOCK(ProtectSubject.ITEM),
    BONE_MEAL(ProtectSubject.ITEM),
    DYE(ProtectSubject.ITEM),
    WAX(ProtectSubject.ITEM),
    THROW(ProtectSubject.ITEM),
    EAT(ProtectSubject.ITEM),
    FIREWORK(ProtectSubject.ITEM),
    SHOOT_ARROW(ProtectSubject.ITEM),
    SPAWN_EGG(ProtectSubject.ITEM),
    FEED_ANIMAL(ProtectSubject.ENTITY),
    USE_ON_VILLAGER(ProtectSubject.ENTITY),
    USE_ON_ENTITY(ProtectSubject.ENTITY),
    USE_ON_GUI_BLOCK(ProtectSubject.BLOCK),
    USE_ON_REDSTONE(ProtectSubject.BLOCK),
    USE_ON_BLOCK(ProtectSubject.BLOCK),
    OTHER_USE(ProtectSubject.ITEM);

    private final ProtectSubject subject;

    ProtectAction(ProtectSubject subject) {
        this.subject = subject;
    }

    public ProtectSubject subject() {
        return subject;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String labelKey() {
        return "config." + MOD_ID + ".protect_action." + id();
    }

    public String tooltipKey() {
        return labelKey() + ".tooltip";
    }

    public static ProtectAction byId(String id) {
        for (var action : values()) {
            if (action.id().equals(id)) return action;
        }
        return null;
    }
}
