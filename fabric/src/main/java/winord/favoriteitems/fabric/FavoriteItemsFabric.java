package winord.favoriteitems.fabric;

import net.fabricmc.api.ModInitializer;
import winord.favoriteitems.FavoriteItems;

public final class FavoriteItemsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FavoriteItems.init();
    }
}
