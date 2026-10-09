package winord.favoriteitems.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import winord.favoriteitems.FavoriteItems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class ProtectEntryMatcher {
    public static final ProtectEntryMatcher EMPTY = new ProtectEntryMatcher(Set.of(), Set.of(), List.of(), false);

    private static final int MAX_GROUP_DEPTH = 16;

    private final Set<ResourceLocation> ids;
    private final Set<String> namespaces;
    private final List<ResourceLocation> tags;
    private final boolean everything;

    private ProtectEntryMatcher(
            Set<ResourceLocation> ids, Set<String> namespaces, List<ResourceLocation> tags, boolean everything)
    {
        this.ids = ids;
        this.namespaces = namespaces;
        this.tags = tags;
        this.everything = everything;
    }

    public static ProtectEntryMatcher parse(List<String> entries) {
        if (entries.isEmpty()) return EMPTY;

        var builder = new Builder();
        builder.addAll(entries, new ArrayList<>());
        return builder.build();
    }

    private static final class Builder {
        final Set<ResourceLocation> ids = new HashSet<>();
        final Set<String> namespaces = new HashSet<>();
        final Set<ResourceLocation> tags = new LinkedHashSet<>();
        boolean everything;

        void addAll(List<String> entries, List<String> path) {
            for (var raw : entries) {
                var entry = Entry.parse(raw);
                if (entry == null) continue;

                switch (entry.kind) {
                    case ID -> ids.add(entry.location);
                    case NAMESPACE -> namespaces.add(entry.location.getNamespace());
                    case TAG -> tags.add(entry.location);
                    case EVERYTHING -> everything = true;
                    case GROUP -> addGroup(entry.groupName, path);
                }
            }
        }

        private void addGroup(String name, List<String> path) {
            var entries = ProtectGroups.entries(name);
            if (entries == null) {
                FavoriteItems.LOGGER.warn("Protect list refers to the group '@{}', but there is no groups/{}.json.", name, name);
                return;
            }
            if (path.contains(name) || path.size() >= MAX_GROUP_DEPTH) {
                FavoriteItems.LOGGER.warn("Group '@{}' includes itself ({}); skipping the repeat.", name, String.join(" -> ", path));
                return;
            }
            path.add(name);
            addAll(entries, path);
            path.remove(path.size() - 1);
        }

        ProtectEntryMatcher build() {
            if (ids.isEmpty() && namespaces.isEmpty() && tags.isEmpty() && !everything) return EMPTY;
            return new ProtectEntryMatcher(ids, namespaces, List.copyOf(tags), everything);
        }
    }

    public static Optional<Component> validate(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();

        var entry = Entry.parse(raw);
        if (entry == null) {
            return Optional.of(Component.translatable("config." + MOD_ID + ".protect_entry_invalid", raw.trim()));
        }
        if (entry.kind == Kind.GROUP && !ProtectGroups.exists(entry.groupName)) {
            return Optional.of(Component.translatable("config." + MOD_ID + ".protect_entry_unknown_group", raw.trim()));
        }
        return Optional.empty();
    }

    public static String normalize(String raw) {
        var entry = Entry.parse(raw);
        return entry != null ? entry.text : null;
    }

    public boolean matchesItem(ItemStack stack) {
        return matches(BuiltInRegistries.ITEM.getKey(stack.getItem()),
                tag -> stack.is(TagKey.create(Registries.ITEM, tag)));
    }

    public boolean matchesBlock(BlockState state) {
        return matches(BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                tag -> state.is(TagKey.create(Registries.BLOCK, tag)));
    }

    public boolean matchesEntity(Entity entity) {
        var type = entity.getType();
        return matches(EntityType.getKey(type), tag -> type.is(TagKey.create(Registries.ENTITY_TYPE, tag)));
    }

    private boolean matches(ResourceLocation key, Predicate<ResourceLocation> hasTag) {
        if (everything || ids.contains(key) || namespaces.contains(key.getNamespace())) return true;

        for (var tag : tags) {
            if (hasTag.test(tag)) return true;
        }
        return false;
    }

    private enum Kind { ID, NAMESPACE, TAG, EVERYTHING, GROUP }

    private static final class Entry {
        final Kind kind;
        final ResourceLocation location;
        final String groupName;
        final String text;

        private Entry(Kind kind, ResourceLocation location, String groupName, String text) {
            this.kind = kind;
            this.location = location;
            this.groupName = groupName;
            this.text = text;
        }

        static Entry parse(String raw) {
            if (raw == null) return null;

            var text = raw.trim().toLowerCase(Locale.ROOT);
            if (text.isEmpty()) return null;

            if (text.equals("*")) return new Entry(Kind.EVERYTHING, null, null, text);

            if (text.startsWith(ProtectGroups.PREFIX)) {
                var name = text.substring(ProtectGroups.PREFIX.length());
                return ProtectGroups.isValidName(name) ? new Entry(Kind.GROUP, null, name, text) : null;
            }

            if (text.startsWith("#")) {
                var tag = ResourceLocation.tryParse(text.substring(1));
                return tag != null ? new Entry(Kind.TAG, tag, null, text) : null;
            }

            if (text.endsWith(":*")) {
                var namespace = text.substring(0, text.length() - 2);
                var probe = ResourceLocation.tryParse(namespace + ":_");
                return probe != null && probe.getNamespace().equals(namespace)
                        ? new Entry(Kind.NAMESPACE, probe, null, text)
                        : null;
            }

            var id = ResourceLocation.tryParse(text);
            return id != null ? new Entry(Kind.ID, id, null, text) : null;
        }
    }
}
