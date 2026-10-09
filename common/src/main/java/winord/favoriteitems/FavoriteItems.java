package winord.favoriteitems;

import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import winord.favoriteitems.sound.FavoriteItemsSounds;

import java.nio.file.Path;

public final class FavoriteItems {
    public static final String MOD_ID = "favoriteitems";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Path CONFIG_DIR = Platform.getConfigFolder().resolve(MOD_ID);

    private FavoriteItems() { }

    public static void init() {
        FavoriteItemsSounds.register();
        LOGGER.info("Initialized FavoriteItems!");
    }
}
