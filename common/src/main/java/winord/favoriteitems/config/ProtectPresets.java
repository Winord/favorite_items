package winord.favoriteitems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.chat.Component;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.util.FileNames;
import winord.favoriteitems.util.JsonFiles;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class ProtectPresets {
    public static final String BUILD_ID = "build_protect";
    public static final String SOFT_ID = "soft_protect";
    public static final String FULL_ID = "full_protect";
    public static final String CUSTOM_ID = "custom";

    public static final String DEFAULT_NAME = "Build Protect";

    private static final List<String> BUILTIN_IDS = List.of(BUILD_ID, SOFT_ID, FULL_ID, CUSTOM_ID);
    private static final int MAX_NAME_LENGTH = 32;
    private static final int FORMAT_VERSION = 2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FavoriteItems.CONFIG_DIR.resolve("presets");

    private static final List<ProtectPreset> presets = new ArrayList<>();

    private static ProtectPreset active = defaultsFor(BUILD_ID);

    private ProtectPresets() { }

    public static ProtectPreset active() {
        return active;
    }

    public static ProtectLevel levelOf(ProtectAction... actions) {
        var best = ProtectLevel.NO;
        for (var action : actions) {
            var level = active.level(action);
            if (level.ordinal() > best.ordinal()) best = level;
        }
        return best;
    }

    public static List<ProtectPreset> all() {
        return Collections.unmodifiableList(presets);
    }

    private static ProtectPreset find(String name) {
        if (name == null) return null;

        for (var preset : presets) {
            if (preset.name().equalsIgnoreCase(name.trim())) return preset;
        }
        return null;
    }

    private static void setActive(ProtectPreset preset) {
        active = preset != null ? preset : fallback();
        FavoriteItemsConfig.protectPreset = active.name();
    }

    private static String builtinName(String id) {
        return switch (id) {
            case BUILD_ID -> DEFAULT_NAME;
            case SOFT_ID -> "Soft Protect";
            case FULL_ID -> "Full Protect";
            default -> "Custom";
        };
    }

    public static ProtectPreset defaultsFor(String id) {
        var preset = new ProtectPreset(id, builtinName(id), true);
        switch (id) {
            case BUILD_ID -> preset.setLevel(ProtectAction.PLACE_BLOCK, ProtectLevel.LAST_ITEM);
            case SOFT_ID -> {
                preset.setLevel(ProtectAction.FEED_ANIMAL, ProtectLevel.FULL);
                preset.setLevel(ProtectAction.EAT, ProtectLevel.LAST_ITEM);
                preset.setLevel(ProtectAction.THROW, ProtectLevel.LAST_ITEM);
                preset.setLevel(ProtectAction.FIREWORK, ProtectLevel.LAST_ITEM);
                preset.setLevel(ProtectAction.SHOOT_ARROW, ProtectLevel.LAST_ITEM);
            }
            case FULL_ID -> {
                for (var action : ProtectAction.values()) preset.setLevel(action, ProtectLevel.FULL);
            }
            default -> { }
        }
        return preset;
    }

    public static void load() {
        presets.clear();
        ProtectGroups.load();

        try {
            Files.createDirectories(DIR);
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to create preset folder {}.", DIR, e);
        }

        for (var id : BUILTIN_IDS) {
            if (!Files.exists(fileFor(id))) save(defaultsFor(id));
        }

        var loaded = new ArrayList<ProtectPreset>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIR, "*.json")) {
            for (var file : stream) {
                var preset = readFile(file);
                if (preset != null) loaded.add(preset);
            }
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to list presets in {}.", DIR, e);
        }

        for (var id : BUILTIN_IDS) {
            presets.add(loaded.stream().filter(p -> p.id().equals(id)).findFirst().orElseGet(() -> defaultsFor(id)));
        }
        loaded.stream()
                .filter(p -> !p.isBuiltin())
                .sorted(Comparator.comparing(p -> p.name().toLowerCase(Locale.ROOT)))
                .forEach(p -> {
                    if (find(p.name()) != null) {
                        FavoriteItems.LOGGER.warn("Ignoring preset file '{}': the name '{}' is already used.", p.id(), p.name());
                    } else {
                        presets.add(p);
                    }
                });

        var wanted = find(FavoriteItemsConfig.protectPreset);
        active = wanted != null ? wanted : fallback();
    }

    private static ProtectPreset fallback() {
        var build = find(DEFAULT_NAME);
        return build != null ? build : defaultsFor(BUILD_ID);
    }

    private static Path fileFor(String id) {
        return DIR.resolve(id + ".json");
    }

    private static ProtectPreset readFile(Path file) {
        var fileName = file.getFileName().toString();
        var id = fileName.substring(0, fileName.length() - ".json".length());
        boolean builtin = BUILTIN_IDS.contains(id);

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            var data = GSON.fromJson(reader, FileData.class);
            if (data == null) throw new JsonSyntaxException("empty file");

            var name = data.name != null && !data.name.isBlank() ? data.name.trim() : (builtin ? builtinName(id) : id);
            var preset = new ProtectPreset(id, name, builtin);
            if (data.actions != null) {
                data.actions.forEach((key, ruleData) -> {
                    var action = ProtectAction.byId(key);
                    if (action != null && ruleData != null) readRule(preset.rule(action), ruleData);
                });
            }
            return preset;
        } catch (IOException | JsonParseException e) {
            FavoriteItems.LOGGER.warn("Failed to read preset {}; {}.", file,
                    builtin ? "using the built-in defaults for it this session" : "skipping it", e);
            return null;
        }
    }

    private static void readRule(ProtectRule rule, RuleData data) {
        var level = ProtectLevel.byId(data.level);
        if (level != null) rule.setLevel(level);

        rule.setLists(
                data.include != null ? data.include : rule.include(),
                data.exclude != null ? data.exclude : rule.exclude());
    }

    public static void save(ProtectPreset preset) {
        var data = new FileData();
        data.version = FORMAT_VERSION;
        data.name = preset.name();
        for (var action : ProtectAction.values()) {
            var rule = preset.rule(action);

            var ruleData = new RuleData();
            ruleData.level = rule.level().id();
            ruleData.include = rule.include();
            ruleData.exclude = rule.exclude();
            data.actions.put(action.id(), ruleData);
        }

        var target = fileFor(preset.id());
        try {
            JsonFiles.writeAtomically(GSON, target, data);
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to save preset {}.", target, e);
        }
    }

    private static final class FileData {
        int version;
        String name;
        Map<String, RuleData> actions = new LinkedHashMap<>();
    }

    private static final class RuleData {
        String level;
        List<String> include;
        List<String> exclude;
    }

    public static Optional<Component> validateNewName(String raw) {
        var name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) return Optional.empty();

        if (name.length() > MAX_NAME_LENGTH) {
            return Optional.of(Component.translatable("config." + MOD_ID + ".preset_name_too_long", MAX_NAME_LENGTH));
        }
        if (find(name) != null || idInUse(idFor(name))) {
            return Optional.of(Component.translatable("config." + MOD_ID + ".preset_name_taken"));
        }
        return Optional.empty();
    }

    private static boolean idInUse(String id) {
        for (var preset : presets) {
            if (preset.id().equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    private static String idFor(String name) {
        return FileNames.sanitize(name, "preset");
    }

    private static ProtectPreset create(String name, ProtectPreset template) {
        if (validateNewName(name).isPresent()) return null;

        var trimmed = name.trim();
        var copy = template.copyAs(idFor(trimmed), trimmed);
        save(copy);
        presets.add(copy);
        return copy;
    }

    private static void delete(ProtectPreset preset) {
        if (preset == null || preset.isBuiltin()) return;

        try {
            Files.deleteIfExists(fileFor(preset.id()));
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to delete preset {}.", preset.id(), e);
            return;
        }
        presets.remove(preset);
    }

    private static void rewriteBuiltins() {
        for (var id : BUILTIN_IDS) {
            save(defaultsFor(id));
        }
        FavoriteItemsConfig.protectPreset = active.name();
        load();
    }

    public static void applyScreenChanges(String selectedName, String newName, String deleteName, boolean resetBuiltins) {
        if (resetBuiltins) rewriteBuiltins();

        if (deleteName != null) delete(find(deleteName));

        var target = find(selectedName);

        if (newName != null && !newName.isBlank() && target != null) {
            var created = create(newName, target);
            if (created != null) target = created;
        }

        setActive(target);
    }
}
