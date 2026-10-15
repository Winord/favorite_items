package winord.favoriteitems.neoforge;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import winord.favoriteitems.FavoriteItems;

@Mod(FavoriteItems.MOD_ID)
public final class FavoriteItemsNeoForge {
    public FavoriteItemsNeoForge(ModContainer container) {
        FavoriteItems.init();
        if (Platform.getEnvironment() == Env.CLIENT) FavoriteItemsNeoForgeClient.init(container);
    }
}
