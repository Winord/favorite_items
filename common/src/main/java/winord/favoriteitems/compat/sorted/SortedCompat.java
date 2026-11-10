package winord.favoriteitems.compat.sorted;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.platform.Platform;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.FavoriteItemsClient;
import winord.favoriteitems.mixin.KeyMappingAccessor;
import winord.favoriteitems.util.FavoriteSlots;

import java.lang.reflect.Method;
import java.util.function.IntFunction;

public final class SortedCompat {
    public static final String MOD_ID = "sorted";

    private static final String PROTECTION_CLASS = "com.sorted.api.SortedProtection";
    private static final String INPUT_CLASS = "com.sorted.api.SortedInput";
    private static final String TOP_UP = "TOP_UP";
    private static final int ENDER_CHEST_SIZE = 27;

    private static Boolean loaded = null;
    private static Method yieldAlt;
    private static Method releaseAlt;
    private static boolean inputFailed;
    private static boolean yielding;

    private SortedCompat() { }

    public static boolean isLoaded() {
        if (loaded == null) loaded = Platform.isModLoaded(MOD_ID);
        return loaded;
    }

    public static void register() {
        if (!isLoaded()) return;

        try {
            var protection = Class.forName(PROTECTION_CLASS);
            protection.getMethod("register", String.class, IntFunction.class)
                    .invoke(null, FavoriteItems.MOD_ID, (IntFunction<String>) SortedCompat::inventoryLevel);
            protection.getMethod("registerEnderChest", String.class, IntFunction.class)
                    .invoke(null, FavoriteItems.MOD_ID, (IntFunction<String>) SortedCompat::enderChestLevel);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FavoriteItems.LOGGER.warn("Sorted is installed, but its protection API (0.1.42 or newer) is missing; "
                    + "favorites are not protected from its sorts and transfers.", e);
        }
    }

    public static void tick() {
        if (inputFailed || !isLoaded()) return;

        boolean wantsAlt = isFavoriteKeyAlt();
        if (wantsAlt == yielding) return;

        try {
            if (yieldAlt == null) {
                var input = Class.forName(INPUT_CLASS);
                yieldAlt = input.getMethod("yieldAlt", String.class);
                releaseAlt = input.getMethod("releaseAlt", String.class);
            }
            (wantsAlt ? yieldAlt : releaseAlt).invoke(null, FavoriteItems.MOD_ID);
            yielding = wantsAlt;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FavoriteItems.LOGGER.warn("Sorted is installed, but its input API (0.1.42 or newer) is missing; "
                    + "its Alt gestures stay on and may clash with the favorite key.", e);
            inputFailed = true;
        }
    }

    private static boolean isFavoriteKeyAlt() {
        var key = ((KeyMappingAccessor) (Object) FavoriteItemsClient.FAVORITE_KEY).favoriteitems$getKey();
        return key.getType() == InputConstants.Type.KEYBOARD
                && (key.getValue() == InputConstants.KEY_LALT || key.getValue() == InputConstants.KEY_RALT);
    }

    private static String inventoryLevel(int slot) {
        return slot >= 0 && slot < FavoriteSlots.INVENTORY_SIZE && FavoriteSlots.isFavorite(slot) ? TOP_UP : null;
    }

    private static String enderChestLevel(int slot) {
        return slot >= 0 && slot < ENDER_CHEST_SIZE && FavoriteSlots.isFavorite(FavoriteSlots.enderChestIndex(slot)) ? TOP_UP : null;
    }
}
