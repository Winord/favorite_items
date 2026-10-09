package winord.favoriteitems.config;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.List;

public final class ProtectRule {
    private final ProtectAction action;
    private ProtectLevel level = ProtectLevel.NO;
    private List<String> include;
    private List<String> exclude;

    private ProtectEntryMatcher includeMatcher;
    private ProtectEntryMatcher excludeMatcher;
    private int matcherRevision = -1;

    public ProtectRule(ProtectAction action) {
        this.action = action;
        this.include = ProtectDefaults.include(action);
        this.exclude = List.of();
    }

    public ProtectLevel level() {
        return level;
    }

    public void setLevel(ProtectLevel level) {
        this.level = level != null ? level : ProtectLevel.NO;
    }

    public List<String> include() {
        return include;
    }

    public List<String> exclude() {
        return exclude;
    }

    public void setLists(List<String> include, List<String> exclude) {
        this.include = clean(include);
        this.exclude = clean(exclude);
        this.includeMatcher = null;
        this.excludeMatcher = null;
    }

    private static List<String> clean(List<String> entries) {
        var result = new LinkedHashSet<String>();
        if (entries != null) {
            for (var raw : entries) {
                var normalized = ProtectEntryMatcher.normalize(raw);
                if (normalized != null) result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    public ProtectRule copy() {
        var copy = new ProtectRule(action);
        copy.level = level;
        copy.include = include;
        copy.exclude = exclude;
        return copy;
    }

    private ProtectEntryMatcher includes() {
        refreshMatchers();
        if (includeMatcher == null) includeMatcher = ProtectEntryMatcher.parse(include);
        return includeMatcher;
    }

    private ProtectEntryMatcher excludes() {
        refreshMatchers();
        if (excludeMatcher == null) excludeMatcher = ProtectEntryMatcher.parse(exclude);
        return excludeMatcher;
    }

    private void refreshMatchers() {
        int current = ProtectGroups.revision();
        if (matcherRevision == current) return;

        includeMatcher = null;
        excludeMatcher = null;
        matcherRevision = current;
    }

    public boolean matchesItem(ItemStack stack) {
        return includes().matchesItem(stack) && !excludes().matchesItem(stack);
    }

    public boolean matchesBlock(BlockState state) {
        return includes().matchesBlock(state) && !excludes().matchesBlock(state);
    }

    public boolean matchesEntity(Entity entity) {
        return includes().matchesEntity(entity) && !excludes().matchesEntity(entity);
    }
}
