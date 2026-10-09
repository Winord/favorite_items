package winord.favoriteitems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.util.JsonFiles;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ProtectGroups {
    public static final String ALL = "all";
    public static final String PREFIX = "@";

    private static final int FORMAT_VERSION = 1;
    private static final Pattern NAME = Pattern.compile("[a-z0-9_.-]+");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FavoriteItems.CONFIG_DIR.resolve("presets").resolve("groups");

    private static final Map<String, Builtin> BUILTINS = new LinkedHashMap<>();

    static {
        BUILTINS.put("placeables", new Builtin(
                "Everything a right-click places in the world: every block item, plus painting, item frames, armor stand and end crystal.",
                ProtectVanilla.PLACEABLES));
        BUILTINS.put("edible", new Builtin("Every vanilla item with food properties.", ProtectVanilla.FOODS));
        BUILTINS.put("dyes", new Builtin("The 16 dyes, ink sac and glow ink sac.", ProtectVanilla.DYES));
        BUILTINS.put("spawn_eggs", new Builtin("Every vanilla spawn egg.", ProtectVanilla.SPAWN_EGGS));
        BUILTINS.put("animals", new Builtin("Every vanilla mob that extends Animal (breedable / tameable).", ProtectVanilla.ANIMALS));
        BUILTINS.put("gui_blocks", new Builtin("Vanilla blocks that open a screen when right-clicked.", ProtectVanilla.GUI_BLOCKS));
        BUILTINS.put(ALL, new Builtin("Everything in the game, vanilla and modded.", List.of("*")));
    }

    private static volatile Map<String, List<String>> groups = Map.of();
    private static volatile int revision;

    private ProtectGroups() { }

    public static String ref(String name) {
        return PREFIX + name;
    }

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    public static boolean exists(String name) {
        return groups.containsKey(name);
    }

    public static List<String> entries(String name) {
        return groups.get(name);
    }

    public static int revision() {
        return revision;
    }

    public static void load() {
        try {
            Files.createDirectories(DIR);
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to create group folder {}.", DIR, e);
        }

        BUILTINS.forEach((name, builtin) -> {
            if (!Files.exists(fileFor(name))) writeFile(name, builtin.description, builtin.entries);
        });

        var loaded = new LinkedHashMap<String, List<String>>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIR, "*.json")) {
            for (var file : stream) {
                var fileName = file.getFileName().toString();
                var name = fileName.substring(0, fileName.length() - ".json".length());
                if (!NAME.matcher(name).matches()) {
                    FavoriteItems.LOGGER.warn("Ignoring group file '{}': the name may only use a-z, 0-9, _ . and -.", fileName);
                    continue;
                }
                var entries = readFile(file);
                if (entries != null) loaded.put(name, entries);
            }
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to list groups in {}.", DIR, e);
        }

        BUILTINS.forEach((name, builtin) -> loaded.computeIfAbsent(name, n -> clean(builtin.entries)));

        groups = Map.copyOf(loaded);
        revision++;
    }

    private static Path fileFor(String name) {
        return DIR.resolve(name + ".json");
    }

    private static List<String> readFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            var data = GSON.fromJson(reader, FileData.class);
            if (data == null || data.entries == null) throw new JsonSyntaxException("no \"entries\" list");
            return clean(data.entries);
        } catch (IOException | JsonParseException e) {
            FavoriteItems.LOGGER.warn("Failed to read group {}; skipping it.", file, e);
            return null;
        }
    }

    private static List<String> clean(List<String> entries) {
        var result = new LinkedHashSet<String>();
        for (var raw : entries) {
            var normalized = ProtectEntryMatcher.normalize(raw);
            if (normalized != null) {
                result.add(normalized);
            } else if (raw != null && !raw.isBlank()) {
                FavoriteItems.LOGGER.warn("Ignoring invalid group entry '{}'.", raw);
            }
        }
        return List.copyOf(result);
    }

    private static void writeFile(String name, String description, List<String> entries) {
        var data = new FileData();
        data.version = FORMAT_VERSION;
        data.description = description;
        data.entries = new ArrayList<>(entries);

        var target = fileFor(name);
        try {
            JsonFiles.writeAtomically(GSON, target, data);
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to write group {}.", target, e);
        }
    }

    private record Builtin(String description, List<String> entries) { }

    private static final class FileData {
        int version;
        String description;
        List<String> entries;
    }
}
