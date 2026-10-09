package winord.favoriteitems.compat.clientsort;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import winord.favoriteitems.FavoriteItems;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ClientSortCompat {
    private static final String CONFIG = "dev.terminalmc.clientsort.client.config.Config";

    public enum HotbarScope { HOTBAR, INVENTORY, NONE }

    private static Method optionsMethod;
    private static Field hotbarScopeField;
    private static boolean configFailed;

    private static Field operationField;
    private static Field originSlotField;
    private static boolean operatorFailed;

    private static boolean topUpFavorites;

    private ClientSortCompat() { }

    public static HotbarScope hotbarScope() {
        if (configFailed) return HotbarScope.HOTBAR;

        try {
            if (hotbarScopeField == null) {
                var config = Class.forName(CONFIG);
                var options = config.getMethod("options");
                var field = options.getReturnType().getField("hotbarScope");
                optionsMethod = options;
                hotbarScopeField = field;
            }
            var value = hotbarScopeField.get(optionsMethod.invoke(null));
            return HotbarScope.valueOf(((Enum<?>) value).name());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FavoriteItems.LOGGER.warn("Client Sort is installed, but its hotbar scope could not be read; "
                    + "its hotbar is treated as a scope of its own.", e);
            configFailed = true;
            return HotbarScope.HOTBAR;
        }
    }

    public static void beginCollecting(Object operator) {
        topUpFavorites = startsInContainerForMatchTransfer(operator);
    }

    public static void endCollecting() {
        topUpFavorites = false;
    }

    public static boolean topsUpFavorites() {
        return topUpFavorites;
    }

    public static void resetState() {
        topUpFavorites = false;
    }

    private static boolean startsInContainerForMatchTransfer(Object operator) {
        if (operatorFailed) return false;

        try {
            if (operationField == null) {
                var field = findField(operator.getClass(), "op");
                var origin = findField(operator.getClass(), "originSlot");
                field.setAccessible(true);
                origin.setAccessible(true);
                operationField = field;
                originSlotField = origin;
            }
            if (!"MATCH_TRANSFER".equals(((Enum<?>) operationField.get(operator)).name())) return false;

            var origin = (Slot) originSlotField.get(operator);
            return origin != null && !(origin.container instanceof Inventory);
        } catch (ReflectiveOperationException | RuntimeException e) {
            FavoriteItems.LOGGER.warn("Client Sort is installed, but its operators could not be read; "
                    + "its Move Matching will not top up favorites.", e);
            operatorFailed = true;
            return false;
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (; type != null; type = type.getSuperclass()) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }
}
