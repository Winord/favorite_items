package winord.favoriteitems.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import winord.favoriteitems.FavoriteItems;

@Mod(FavoriteItems.MOD_ID)
public final class FavoriteItemsNeoForge {
    public FavoriteItemsNeoForge(ModContainer container) {
        FavoriteItems.init();
        if (FMLEnvironment.dist == Dist.CLIENT) FavoriteItemsNeoForgeClient.init(container);
    }
}
