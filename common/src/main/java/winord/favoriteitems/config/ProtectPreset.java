package winord.favoriteitems.config;

import java.util.EnumMap;

public final class ProtectPreset {
    private final String id;
    private final String name;
    private final boolean builtin;
    private final EnumMap<ProtectAction, ProtectRule> rules = new EnumMap<>(ProtectAction.class);

    public ProtectPreset(String id, String name, boolean builtin) {
        this.id = id;
        this.name = name;
        this.builtin = builtin;
        for (var action : ProtectAction.values()) {
            rules.put(action, new ProtectRule(action));
        }
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public ProtectRule rule(ProtectAction action) {
        return rules.get(action);
    }

    public ProtectLevel level(ProtectAction action) {
        return rules.get(action).level();
    }

    public void setLevel(ProtectAction action, ProtectLevel level) {
        rules.get(action).setLevel(level);
    }

    public ProtectPreset copyAs(String newId, String newName) {
        var copy = new ProtectPreset(newId, newName, false);
        rules.forEach((action, rule) -> copy.rules.put(action, rule.copy()));
        return copy;
    }
}
