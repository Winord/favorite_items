package winord.favoriteitems.fabric;

import net.fabricmc.api.ClientModInitializer;
import winord.favoriteitems.FavoriteItemsClient;

public final class FavoriteItemsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FavoriteItemsClient.initClient();
    }
}
