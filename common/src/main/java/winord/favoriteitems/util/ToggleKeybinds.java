package winord.favoriteitems.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import winord.favoriteitems.config.FavoriteItemsConfig;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class ToggleKeybinds {
    public static final String CATEGORY = "key.categories." + MOD_ID;

    private record Toggle(KeyMapping key, String labelKey, BooleanSupplier get, Consumer<Boolean> set) { }

    private static final List<Toggle> TOGGLES = List.of(
            toggle("toggle_protect", () -> FavoriteItemsConfig.protectEnabled, v -> FavoriteItemsConfig.protectEnabled = v),
            toggle("toggle_axe_protect", () -> FavoriteItemsConfig.axeProtect, v -> FavoriteItemsConfig.axeProtect = v),
            toggle("toggle_shovel_protect", () -> FavoriteItemsConfig.shovelProtect, v -> FavoriteItemsConfig.shovelProtect = v),
            toggle("toggle_hoe_protect", () -> FavoriteItemsConfig.hoeProtect, v -> FavoriteItemsConfig.hoeProtect = v),
            toggle("toggle_shears_protect", () -> FavoriteItemsConfig.shearsProtect, v -> FavoriteItemsConfig.shearsProtect = v),
            toggle("toggle_brush_protect", () -> FavoriteItemsConfig.brushProtect, v -> FavoriteItemsConfig.brushProtect = v));

    private ToggleKeybinds() { }

    private static Toggle toggle(String name, BooleanSupplier get, Consumer<Boolean> set) {
        var key = new KeyMapping("key." + MOD_ID + "." + name, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
        return new Toggle(key, "message." + MOD_ID + ".label." + name, get, set);
    }

    public static void register() {
        TOGGLES.forEach(toggle -> KeyMappingRegistry.register(toggle.key()));
    }

    public static void tick() {
        if (FavoriteSlots.isSuspended()) {
            for (var toggle : TOGGLES) {
                while (toggle.key().consumeClick()) { }
            }
            return;
        }

        for (var toggle : TOGGLES) {
            boolean pressed = false;
            while (toggle.key().consumeClick()) {
                toggle.set().accept(!toggle.get().getAsBoolean());
                pressed = true;
            }
            if (!pressed) continue;

            FavoriteItemsConfig.save();
            ActionBarMessages.toggled(toggle.labelKey(), toggle.get().getAsBoolean());
        }
    }
}
