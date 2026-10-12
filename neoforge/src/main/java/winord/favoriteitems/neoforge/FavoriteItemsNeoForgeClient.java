package winord.favoriteitems.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import winord.favoriteitems.FavoriteItemsClient;
import winord.favoriteitems.config.ConfigScreens;

final class FavoriteItemsNeoForgeClient {
    private FavoriteItemsNeoForgeClient() { }

    static void init(ModContainer container) {
        FavoriteItemsClient.initClient();
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> ConfigScreens.create(parent));
    }
}
