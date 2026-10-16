package winord.favoriteitems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.util.JsonFiles;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class FavoriteItemsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FavoriteItems.CONFIG_DIR.resolve("settings.json");

    public static FavoriteStyle activeStyle = FavoriteStyle.STAR;
    public static FavoriteSoundStyle soundStyle = FavoriteSoundStyle.DEFAULT;
    public static HotbarIndicatorMode hotbarIndicatorMode = HotbarIndicatorMode.ON_INTERACTION;
    public static DropFavoriteMode dropFavorites = DropFavoriteMode.BLOCK;
    public static boolean actionBarNotifications = true;

    public static boolean protectEnabled = false;
    public static ProtectScope protectScope = ProtectScope.STACKABLE;
    public static ProtectTarget protectTarget = ProtectTarget.FAVORITES;
    public static String protectPreset = ProtectPresets.DEFAULT_NAME;

    public static boolean axeProtect = false;
    public static boolean shovelProtect = false;
    public static boolean hoeProtect = false;
    public static boolean shearsProtect = false;
    public static boolean brushProtect = false;
    public static boolean toolsBreakingProtect = false;

    public static MoveSameFavoritesMode moveSameFavorites = MoveSameFavoritesMode.STAY;

    public static boolean ipnRefillSkipsFavorites = true;
    public static boolean ipnRefillProtectedLastItem = true;

    public static boolean lobbyFavorites = false;
    public static boolean deathFavorites = false;

    public static final Map<FavoriteStyle, StyleSettings> styles = defaultStyles();

    private FavoriteItemsConfig() { }

    private static Map<FavoriteStyle, StyleSettings> defaultStyles() {
        var map = new EnumMap<FavoriteStyle, StyleSettings>(FavoriteStyle.class);
        for (var style : FavoriteStyle.values()) map.put(style, StyleSettings.defaultFor(style));
        return map;
    }

    public static StyleSettings settingsFor(FavoriteStyle style) {
        return styles.get(style);
    }

    public static StyleSettings activeSettings() {
        return styles.get(activeStyle);
    }

    public static void load() {
        if (!Files.exists(FILE)) return;

        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            var loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) loaded.applyTo();
        } catch (IOException | JsonSyntaxException e) {
            FavoriteItems.LOGGER.warn("Failed to load config from {}, using defaults.", FILE, e);
        }
    }

    public static void save() {
        try {
            JsonFiles.writeAtomically(GSON, FILE, Data.capture());
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to save config to {}.", FILE, e);
        }
    }

    private static final class Data {
        FavoriteStyle activeStyle = FavoriteItemsConfig.activeStyle;
        FavoriteSoundStyle soundStyle = FavoriteItemsConfig.soundStyle;
        HotbarIndicatorMode hotbarIndicatorMode = FavoriteItemsConfig.hotbarIndicatorMode;
        DropFavoriteMode dropFavorites = FavoriteItemsConfig.dropFavorites;
        MoveSameFavoritesMode moveSameFavorites = FavoriteItemsConfig.moveSameFavorites;
        boolean actionBarNotifications = FavoriteItemsConfig.actionBarNotifications;
        boolean protectEnabled = FavoriteItemsConfig.protectEnabled;
        ProtectScope protectScope = FavoriteItemsConfig.protectScope;
        ProtectTarget protectTarget = FavoriteItemsConfig.protectTarget;
        String protectPreset = FavoriteItemsConfig.protectPreset;
        boolean axeProtect = FavoriteItemsConfig.axeProtect;
        boolean shovelProtect = FavoriteItemsConfig.shovelProtect;
        boolean hoeProtect = FavoriteItemsConfig.hoeProtect;
        boolean shearsProtect = FavoriteItemsConfig.shearsProtect;
        boolean brushProtect = FavoriteItemsConfig.brushProtect;
        boolean toolsBreakingProtect = FavoriteItemsConfig.toolsBreakingProtect;
        boolean ipnRefillSkipsFavorites = FavoriteItemsConfig.ipnRefillSkipsFavorites;
        boolean ipnRefillProtectedLastItem = FavoriteItemsConfig.ipnRefillProtectedLastItem;
        boolean lobbyFavorites = FavoriteItemsConfig.lobbyFavorites;
        boolean deathFavorites = FavoriteItemsConfig.deathFavorites;
        Map<FavoriteStyle, StyleSettings> styles = new EnumMap<>(FavoriteStyle.class);

        static Data capture() {
            var data = new Data();
            FavoriteItemsConfig.styles.forEach((style, settings) -> data.styles.put(style, settings.copy()));
            return data;
        }

        void applyTo() {
            if (activeStyle != null) FavoriteItemsConfig.activeStyle = activeStyle;
            if (soundStyle != null) FavoriteItemsConfig.soundStyle = soundStyle;
            if (hotbarIndicatorMode != null) FavoriteItemsConfig.hotbarIndicatorMode = hotbarIndicatorMode;
            if (dropFavorites != null) FavoriteItemsConfig.dropFavorites = dropFavorites;
            if (moveSameFavorites != null) FavoriteItemsConfig.moveSameFavorites = moveSameFavorites;
            FavoriteItemsConfig.actionBarNotifications = actionBarNotifications;
            FavoriteItemsConfig.protectEnabled = protectEnabled;
            if (protectScope != null) FavoriteItemsConfig.protectScope = protectScope;
            if (protectTarget != null) FavoriteItemsConfig.protectTarget = protectTarget;
            if (protectPreset != null && !protectPreset.isBlank()) FavoriteItemsConfig.protectPreset = protectPreset;
            FavoriteItemsConfig.axeProtect = axeProtect;
            FavoriteItemsConfig.shovelProtect = shovelProtect;
            FavoriteItemsConfig.hoeProtect = hoeProtect;
            FavoriteItemsConfig.shearsProtect = shearsProtect;
            FavoriteItemsConfig.brushProtect = brushProtect;
            FavoriteItemsConfig.toolsBreakingProtect = toolsBreakingProtect;
            FavoriteItemsConfig.ipnRefillSkipsFavorites = ipnRefillSkipsFavorites;
            FavoriteItemsConfig.ipnRefillProtectedLastItem = ipnRefillProtectedLastItem;
            FavoriteItemsConfig.lobbyFavorites = lobbyFavorites;
            FavoriteItemsConfig.deathFavorites = deathFavorites;

            if (styles != null) {
                styles.forEach((style, settings) -> {
                    if (style != null && settings != null) FavoriteItemsConfig.styles.put(style, settings);
                });
            }
            FavoriteItemsConfig.styles.get(FavoriteStyle.MINIMAL).backgroundEnabled = true;
            FavoriteItemsConfig.styles.values().forEach(StyleSettings::normalize);
        }
    }
}
