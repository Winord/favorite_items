package winord.favoriteitems.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.FavoriteItemsClient;
import winord.favoriteitems.config.ConfigScreens;

@Mod(FavoriteItems.MOD_ID)
public final class FavoriteItemsForge {
    public FavoriteItemsForge() {
        EventBuses.registerModEventBus(FavoriteItems.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        FavoriteItems.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> FavoriteItemsClient::initClient);

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> ConfigScreens.create(parent)));
    }
}
