package winord.favoriteitems.config;

import dev.architectury.platform.Platform;
import net.minecraft.client.gui.screens.Screen;

public final class ConfigScreens {
    private ConfigScreens() { }

    public static Screen create(Screen parent) {
        if (Platform.isModLoaded("cloth-config") || Platform.isModLoaded("cloth_config")) {
            return ClothConfigScreenBuilder.build(parent);
        }
        return new MissingClothScreen(parent);
    }
}
