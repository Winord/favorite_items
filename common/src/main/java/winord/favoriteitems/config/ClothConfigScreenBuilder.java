package winord.favoriteitems.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.KeyCodeEntry;
import me.shedaniel.clothconfig2.gui.entries.LongSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.compat.ipn.IpnCompat;
import winord.favoriteitems.util.MoveSameFavorites;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class ClothConfigScreenBuilder {
    private static final int LABEL_GAP = 6;
    private static final int NESTED_INDENT = 14;

    private ClothConfigScreenBuilder() { }

    public static Screen build(Screen parent) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(key("title")));

        var entryBuilder = builder.entryBuilder();

        var general = builder.getOrCreateCategory(Component.translatable(key("category.general")));
        StylePreviewRenderer.clearLive();
        StylePreviewOverlay.clearSections();
        var activeStyleEntry = entryBuilder
                .startEnumSelector(Component.translatable(key("active_style")), FavoriteStyle.class, FavoriteItemsConfig.activeStyle)
                .setDefaultValue(FavoriteStyle.STAR)
                .setEnumNameProvider(value -> Component.translatable(((FavoriteStyle) value).langKey()))
                .setSaveConsumer(value -> FavoriteItemsConfig.activeStyle = value)
                .build();
        var soundStyleEntry = entryBuilder
                .startEnumSelector(Component.translatable(key("sound_style")), FavoriteSoundStyle.class, FavoriteItemsConfig.soundStyle)
                .setDefaultValue(FavoriteSoundStyle.DEFAULT)
                .setEnumNameProvider(value -> Component.translatable(key("sound_style." + lower(value))))
                .setSaveConsumer(value -> FavoriteItemsConfig.soundStyle = value)
                .build();

        general.addEntry(entryBuilder
                .startTextDescription(Component.translatable(key("custom_note")))
                .setDisplayRequirement(() -> soundStyleEntry.getValue() == FavoriteSoundStyle.CUSTOM || activeStyleEntry.getValue() == FavoriteStyle.CUSTOM)
                .build());

        general.addEntry(activeStyleEntry);
        general.addEntry(soundStyleEntry);

        var hotbarIndicatorEntry = entryBuilder
                .startEnumSelector(Component.translatable(key("hotbar_indicator")), HotbarIndicatorMode.class, FavoriteItemsConfig.hotbarIndicatorMode)
                .setDefaultValue(HotbarIndicatorMode.ON_INTERACTION)
                .setEnumNameProvider(value -> Component.translatable(key("hotbar_indicator." + lower(value))))
                .setTooltip(Component.translatable(key("hotbar_indicator.tooltip")))
                .setSaveConsumer(value -> FavoriteItemsConfig.hotbarIndicatorMode = value)
                .build();
        general.addEntry(hotbarIndicatorEntry);

        general.addEntry(entryBuilder
                .startEnumSelector(Component.translatable(key("drop_favorites")), DropFavoriteMode.class, FavoriteItemsConfig.dropFavorites)
                .setDefaultValue(DropFavoriteMode.BLOCK)
                .setEnumNameProvider(value -> Component.translatable(key("drop_favorites." + lower(value))))
                .setTooltip(Component.translatable(key("drop_favorites.tooltip")))
                .setSaveConsumer(value -> FavoriteItemsConfig.dropFavorites = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable(key("action_bar_notifications")), FavoriteItemsConfig.actionBarNotifications)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(key("action_bar_notifications.tooltip")))
                .setSaveConsumer(value -> FavoriteItemsConfig.actionBarNotifications = value)
                .build());

        var styles = builder.getOrCreateCategory(Component.translatable(key("category.styles")));
        for (var style : FavoriteStyle.values()) {
            styles.addEntry(style == FavoriteStyle.MINIMAL
                    ? buildMinimalStyleSubCategory(entryBuilder)
                    : buildStyleSubCategory(entryBuilder, style));
        }

        var saveProtection = buildAdditionalProtectionCategory(builder, entryBuilder);
        buildExtraCategory(builder, entryBuilder);
        builder.setSavingRunnable(() -> {
            saveProtection.run();
            FavoriteItemsConfig.save();
        });

        var screen = builder.build();
        var generalName = Component.translatable(key("category.general")).getString();
        var stylesName = Component.translatable(key("category.styles")).getString();
        StylePreviewOverlay.attach(screen, () -> {
            if (!(screen instanceof ClothConfigScreen cloth)) return StylePreviewOverlay.Tab.OTHER;
            var selected = cloth.getSelectedCategory().getString();
            if (selected.equals(generalName)) return StylePreviewOverlay.Tab.GENERAL;
            if (selected.equals(stylesName)) return StylePreviewOverlay.Tab.STYLES;
            return StylePreviewOverlay.Tab.OTHER;
        }, activeStyleEntry::getValue, () -> listAccessOf(screen));
        return screen;
    }

    private static Method holeMethod;
    private static boolean holeFailed;

    private static void paintHole(GuiGraphics graphics, DynamicEntryListWidget<?> list, int from, int to) {
        if (holeFailed) return;

        try {
            if (holeMethod == null) {
                holeMethod = DynamicEntryListWidget.class.getDeclaredMethod("renderHoleBackground",
                        GuiGraphics.class, int.class, int.class, int.class, int.class);
                holeMethod.setAccessible(true);
            }
            holeMethod.invoke(list, graphics, from, to, 255, 255);
        } catch (ReflectiveOperationException | RuntimeException e) {
            holeFailed = true;
            FavoriteItems.LOGGER.warn("Cloth Config list background could not be extended under the style preview.", e);
        }
    }

    private static StylePreviewOverlay.ListAccess listAccessOf(Screen screen) {
        if (!(screen instanceof ClothConfigScreen cloth) || cloth.listWidget == null) return null;

        var list = cloth.listWidget;
        return new StylePreviewOverlay.ListAccess() {
            @Override public int top() { return list.top; }
            @Override public int bottom() { return list.bottom; }
            @Override public void syncVisibility() {
                list.tickList();
                list.tickList();
            }
            @Override public void paintFrame(GuiGraphics graphics, Object widget) {
                if (widget != list) return;

                int from = list.left + list.width;
                int to = screen.width;
                if (from >= to) return;

                int savedLeft = list.left;
                int savedWidth = list.width;
                list.left = from;
                list.width = to - from;
                try {
                    paintHole(graphics, list, 0, list.top);
                    paintHole(graphics, list, list.bottom, list.height);
                } finally {
                    list.left = savedLeft;
                    list.width = savedWidth;
                }
                graphics.blit(RenderType::guiTextured, Screen.HEADER_SEPARATOR, from, list.top - 2, 0f, 0f, to - from, 2, 32, 2);
                graphics.blit(RenderType::guiTextured, Screen.FOOTER_SEPARATOR, from, list.bottom, 0f, 0f, to - from, 2, 32, 2);
            }
            @Override public void setBounds(int left, int width) {
                list.left = left;
                list.width = width;
                list.right = left + width;
            }
            @Override public Object sectionAt(int mouseY) {
                if (mouseY < list.top || mouseY >= list.bottom) return null;
                int y = list.getRowTop(0);
                for (var entry : list.visibleChildren()) {
                    int height = entry.getItemHeight();
                    if (mouseY >= y && mouseY < y + height) return entry;
                    y += height;
                }
                return null;
            }
            @Override public void closeDropdownsOutside(double mouseX, double mouseY) {
                var dropdowns = new ArrayList<DropdownBoxEntry<?>>();
                collectDropdowns(list.visibleChildren(), dropdowns);
                for (var dropdown : dropdowns) {
                    if (dropdown.getFocused() == null) continue;
                    if (containsMouse(dropdown.getSelectionElement(), mouseX, mouseY)) continue;
                    dropdown.getSelectionElement().setFocused((GuiEventListener) null);
                    dropdown.setFocused((GuiEventListener) null);
                }
            }
            @Override public int labelLimit(Object entry) {
                boolean labelled = entry instanceof BooleanListEntry
                        || entry instanceof SelectionListEntry<?>
                        || entry instanceof DropdownBoxEntry<?>
                        || entry instanceof TextFieldListEntry<?>
                        || entry instanceof KeyCodeEntry
                        || entry instanceof IntegerSliderEntry
                        || entry instanceof LongSliderEntry;
                if (!labelled) return -1;

                int depth = depthOf(list.visibleChildren(), entry, 0);
                if (depth < 0) return -1;
                return list.getItemWidth() - 150 - LABEL_GAP - NESTED_INDENT * depth;
            }
        };
    }

    private static void collectDropdowns(Iterable<?> entries, List<DropdownBoxEntry<?>> out) {
        for (var entry : entries) {
            if (entry instanceof DropdownBoxEntry<?> dropdown) out.add(dropdown);
            if (entry instanceof SubCategoryListEntry sub) collectDropdowns(sub.getValue(), out);
        }
    }

    private static boolean containsMouse(GuiEventListener listener, double mouseX, double mouseY) {
        if (listener.isMouseOver(mouseX, mouseY)) return true;
        if (!(listener instanceof ContainerEventHandler container)) return false;
        for (var child : container.children()) {
            if (containsMouse(child, mouseX, mouseY)) return true;
        }
        return false;
    }

    private static int depthOf(Iterable<?> entries, Object target, int depth) {
        for (var entry : entries) {
            if (entry == target) return depth;
            if (entry instanceof SubCategoryListEntry sub) {
                int found = depthOf(sub.getValue(), target, depth + 1);
                if (found >= 0) return found;
            }
        }
        return -1;
    }

    private static void buildExtraCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        var category = builder.getOrCreateCategory(Component.translatable(key("category.extra")));
        List<AbstractConfigListEntry> tools = new ArrayList<>();

        tools.add(toolToggle(entryBuilder, "axe_protect", FavoriteItemsConfig.axeProtect, v -> FavoriteItemsConfig.axeProtect = v));
        tools.add(toolToggle(entryBuilder, "shovel_protect", FavoriteItemsConfig.shovelProtect, v -> FavoriteItemsConfig.shovelProtect = v));
        tools.add(toolToggle(entryBuilder, "hoe_protect", FavoriteItemsConfig.hoeProtect, v -> FavoriteItemsConfig.hoeProtect = v));
        tools.add(toolToggle(entryBuilder, "shears_protect", FavoriteItemsConfig.shearsProtect, v -> FavoriteItemsConfig.shearsProtect = v));
        tools.add(toolToggle(entryBuilder, "brush_protect", FavoriteItemsConfig.brushProtect, v -> FavoriteItemsConfig.brushProtect = v));

        category.addEntry(entryBuilder
                .startSubCategory(Component.translatable(key("tools_protect")), tools)
                .setExpanded(false)
                .build());

        if (MoveSameFavorites.isAnySortingModLoaded()) category.addEntry(buildSortingGroup(entryBuilder));
        if (IpnCompat.isLoaded()) category.addEntry(buildIpnGroup(entryBuilder));

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable(key("lobby_favorites")), FavoriteItemsConfig.lobbyFavorites)
                .setDefaultValue(false)
                .setTooltip(Component.translatable(key("lobby_favorites.tooltip")))
                .setSaveConsumer(v -> FavoriteItemsConfig.lobbyFavorites = v)
                .build());
        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable(key("death_favorites")), FavoriteItemsConfig.deathFavorites)
                .setDefaultValue(false)
                .setTooltip(Component.translatable(key("death_favorites.tooltip")))
                .setSaveConsumer(v -> FavoriteItemsConfig.deathFavorites = v)
                .build());
    }

    private static SubCategoryListEntry buildSortingGroup(ConfigEntryBuilder entryBuilder) {
        List<AbstractConfigListEntry> sorting = new ArrayList<>();
        sorting.add(entryBuilder
                .startEnumSelector(Component.translatable(key("move_same_favorites")), MoveSameFavoritesMode.class, FavoriteItemsConfig.moveSameFavorites)
                .setDefaultValue(MoveSameFavoritesMode.STAY)
                .setEnumNameProvider(value -> Component.translatable(key("move_same_favorites." + lower(value))))
                .setTooltip(Component.translatable(key("move_same_favorites.tooltip")))
                .setSaveConsumer(value -> FavoriteItemsConfig.moveSameFavorites = value)
                .build());
        return entryBuilder
                .startSubCategory(Component.translatable(key("sorting_compat")), sorting)
                .setExpanded(true)
                .build();
    }

    private static SubCategoryListEntry buildIpnGroup(ConfigEntryBuilder entryBuilder) {
        List<AbstractConfigListEntry> ipn = new ArrayList<>();
        ipn.add(entryBuilder
                .startBooleanToggle(Component.translatable(key("ipn_refill_favorites")), FavoriteItemsConfig.ipnRefillSkipsFavorites)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(key("ipn_refill_favorites.tooltip")))
                .setSaveConsumer(v -> FavoriteItemsConfig.ipnRefillSkipsFavorites = v)
                .build());
        ipn.add(entryBuilder
                .startBooleanToggle(Component.translatable(key("ipn_refill_last_item")), FavoriteItemsConfig.ipnRefillProtectedLastItem)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(key("ipn_refill_last_item.tooltip")))
                .setSaveConsumer(v -> FavoriteItemsConfig.ipnRefillProtectedLastItem = v)
                .build());
        return entryBuilder
                .startSubCategory(Component.translatable(key("ipn_compat")), ipn)
                .setExpanded(true)
                .build();
    }

    private static AbstractConfigListEntry<?> toolToggle(
            ConfigEntryBuilder entryBuilder, String name, boolean current, Consumer<Boolean> save)
    {
        return entryBuilder
                .startBooleanToggle(Component.translatable(key(name)), current)
                .setDefaultValue(false)
                .setTooltip(Component.translatable(key(name + ".tooltip")), Component.translatable(key("extra.tooltip.independent")))
                .setSaveConsumer(save)
                .build();
    }

    private record ActionRows(EnumListEntry<ProtectLevel> level, StringListListEntry include, StringListListEntry exclude) { }

    private static Runnable buildAdditionalProtectionCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        var category = builder.getOrCreateCategory(Component.translatable(key("category.additional_protection")));

        var protectEnabledEntry = entryBuilder
                .startBooleanToggle(Component.translatable(key("protect_enabled")), FavoriteItemsConfig.protectEnabled)
                .setDefaultValue(false)
                .setTooltip(Component.translatable(key("protect_enabled.tooltip")))
                .setSaveConsumer(value -> FavoriteItemsConfig.protectEnabled = value)
                .build();
        category.addEntry(protectEnabledEntry);

        category.addEntry(entryBuilder
                .startTextDescription(Component.translatable(key("protect_disabled_note")))
                .setDisplayRequirement(() -> !protectEnabledEntry.getValue())
                .build());

        category.addEntry(entryBuilder
                .startEnumSelector(Component.translatable(key("protect_target")), ProtectTarget.class, FavoriteItemsConfig.protectTarget)
                .setDefaultValue(ProtectTarget.FAVORITES)
                .setEnumNameProvider(value -> Component.translatable(key("protect_target." + lower(value))))
                .setTooltip(Component.translatable(key("protect_target.tooltip")))
                .setSaveConsumer(value -> FavoriteItemsConfig.protectTarget = value)
                .build());

        category.addEntry(entryBuilder
                .startEnumSelector(Component.translatable(key("protect_scope")), ProtectScope.class, FavoriteItemsConfig.protectScope)
                .setDefaultValue(ProtectScope.STACKABLE)
                .setEnumNameProvider(value -> Component.translatable(key("protect_scope." + lower(value))))
                .setTooltip(Component.translatable(key("protect_scope.tooltip")))
                .setSaveConsumer(value -> FavoriteItemsConfig.protectScope = value)
                .build());

        var presetNames = ProtectPresets.all().stream().map(ProtectPreset::name).toList();
        var presetEntry = entryBuilder
                .startStringDropdownMenu(Component.translatable(key("preset")), ProtectPresets.active().name())
                .setSelections(presetNames)
                .setSuggestionMode(false)
                .setDefaultValue(ProtectPresets.DEFAULT_NAME)
                .setTooltip(Component.translatable(key("preset.tooltip")))
                .build();
        category.addEntry(presetEntry);

        var groups = new LinkedHashMap<ProtectPreset, EnumMap<ProtectAction, ActionRows>>();
        for (var preset : ProtectPresets.all()) {
            var defaults = ProtectPresets.defaultsFor(preset.isBuiltin() ? preset.id() : ProtectPresets.CUSTOM_ID);
            var rows = new EnumMap<ProtectAction, ActionRows>(ProtectAction.class);
            var levelRows = new ArrayList<AbstractConfigListEntry>();
            var listRows = new ArrayList<AbstractConfigListEntry>();

            for (var action : ProtectAction.values()) {
                var rule = preset.rule(action);
                var subject = lower(action.subject());

                var levelEntry = entryBuilder
                        .startEnumSelector(Component.translatable(action.labelKey()), ProtectLevel.class, rule.level())
                        .setDefaultValue(defaults.level(action))
                        .setEnumNameProvider(value -> Component.translatable(key("protect_level." + ((ProtectLevel) value).id())))
                        .setTooltip(Component.translatable(action.tooltipKey()))
                        .setSaveConsumer(value -> { })
                        .build();
                levelRows.add(levelEntry);

                var includeEntry = listEntry(entryBuilder, "include", subject, rule.include(), ProtectDefaults.include(action));
                var excludeEntry = listEntry(entryBuilder, "exclude", subject, rule.exclude(), List.of());

                var actionLists = entryBuilder
                        .startSubCategory(Component.translatable(action.labelKey()))
                        .setExpanded(false)
                        .setTooltip(Component.translatable(key("protect_list.subject." + subject)));
                actionLists.add(includeEntry);
                actionLists.add(excludeEntry);
                listRows.add(actionLists.build());

                rows.put(action, new ActionRows(levelEntry, includeEntry, excludeEntry));
            }
            groups.put(preset, rows);

            category.addEntry(entryBuilder
                    .startSubCategory(Component.translatable(key("protect_actions"), preset.name()), levelRows)
                    .setExpanded(true)
                    .setTooltip(Component.translatable(key("protect_actions.tooltip")))
                    .setDisplayRequirement(Requirement.isValue(presetEntry, preset.name()))
                    .build());

            category.addEntry(entryBuilder
                    .startSubCategory(Component.translatable(key("protect_lists"), preset.name()), listRows)
                    .setExpanded(false)
                    .setTooltip(Component.translatable(key("protect_lists.tooltip")))
                    .setDisplayRequirement(Requirement.isValue(presetEntry, preset.name()))
                    .build());
        }

        var newNameEntry = entryBuilder
                .startStrField(Component.translatable(key("new_preset")), "")
                .setDefaultValue("")
                .setTooltip(Component.translatable(key("new_preset.tooltip")))
                .setErrorSupplier(ProtectPresets::validateNewName)
                .setSaveConsumer(value -> { })
                .build();

        var noneLabel = Component.translatable(key("delete_preset.none")).getString();
        var deletable = new ArrayList<String>();
        deletable.add(noneLabel);
        ProtectPresets.all().stream().filter(p -> !p.isBuiltin()).forEach(p -> deletable.add(p.name()));

        var deleteEntry = entryBuilder
                .startStringDropdownMenu(Component.translatable(key("delete_preset")), noneLabel)
                .setSelections(deletable)
                .setSuggestionMode(false)
                .setDefaultValue(noneLabel)
                .setTooltip(Component.translatable(key("delete_preset.tooltip")))
                .build();

        var resetEntry = entryBuilder
                .startBooleanToggle(Component.translatable(key("reset_builtins")), false)
                .setDefaultValue(false)
                .setTooltip(Component.translatable(key("reset_builtins.tooltip")))
                .setSaveConsumer(value -> { })
                .build();

        var manageList = new ArrayList<AbstractConfigListEntry>();
        manageList.add(newNameEntry);
        manageList.add(deleteEntry);
        manageList.add(resetEntry);
        category.addEntry(entryBuilder
                .startSubCategory(Component.translatable(key("manage_presets")), manageList)
                .setExpanded(false)
                .build());

        return () -> {
            groups.forEach((preset, rows) -> {
                boolean changed = false;
                for (var row : rows.entrySet()) {
                    var rule = preset.rule(row.getKey());
                    var entries = row.getValue();

                    var levelBefore = rule.level();
                    var includeBefore = rule.include();
                    var excludeBefore = rule.exclude();

                    rule.setLevel(entries.level().getValue());
                    rule.setLists(entries.include().getValue(), entries.exclude().getValue());

                    if (levelBefore != rule.level() || !includeBefore.equals(rule.include()) || !excludeBefore.equals(rule.exclude())) {
                        changed = true;
                    }
                }
                if (changed) ProtectPresets.save(preset);
            });

            var deleteValue = deleteEntry.getValue();
            ProtectPresets.applyScreenChanges(
                    presetEntry.getValue(),
                    newNameEntry.getValue(),
                    noneLabel.equals(deleteValue) ? null : deleteValue,
                    resetEntry.getValue());
        };
    }

    private static StringListListEntry listEntry(
            ConfigEntryBuilder entryBuilder, String kind, String subject, List<String> current, List<String> defaults)
    {
        return entryBuilder
                .startStrList(Component.translatable(key("protect_list." + kind + "." + subject)), new ArrayList<>(current))
                .setDefaultValue(defaults)
                .setExpanded(false)
                .setCellErrorSupplier(ProtectEntryMatcher::validate)
                .setTooltip(
                        Component.translatable(key("protect_list." + kind + ".tooltip")),
                        Component.translatable(key("protect_list.syntax." + subject)),
                        Component.translatable(key("protect_list.syntax.common")),
                        Component.translatable(key("protect_list.groups." + subject)))
                .setSaveConsumer(value -> { })
                .build();
    }

    private static String key(String suffix) {
        return "config." + MOD_ID + "." + suffix;
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static IntegerSliderEntry percentSlider(
            ConfigEntryBuilder entryBuilder, String name, int current, int defaultValue, Requirement requirement, Consumer<Integer> save)
    {
        var slider = entryBuilder
                .startIntSlider(Component.translatable(key(name)), current, 0, 100)
                .setDefaultValue(defaultValue)
                .setTextGetter(value -> Component.literal(value + "%"))
                .setSaveConsumer(save);
        if (requirement != null) slider.setRequirement(requirement);
        return slider.build();
    }

    private static BooleanListEntry aboveUnderToggle(
            ConfigEntryBuilder entryBuilder, String name, boolean current, boolean defaultValue, Requirement requirement, Consumer<Boolean> save)
    {
        return entryBuilder
                .startBooleanToggle(Component.translatable(key(name)), current)
                .setDefaultValue(defaultValue)
                .setTooltip(Component.translatable(key(name + ".tooltip")))
                .setYesNoTextSupplier(value -> Component.translatable(key(name + "." + (value ? "above" : "under"))))
                .setRequirement(requirement)
                .setSaveConsumer(save)
                .build();
    }

    private static SubCategoryListEntry group(ConfigEntryBuilder entryBuilder, String nameKey, AbstractConfigListEntry... entries) {
        List<AbstractConfigListEntry> list = new ArrayList<>(List.of(entries));
        return entryBuilder.startSubCategory(Component.translatable(nameKey), list).setExpanded(false).build();
    }

    private static boolean isOn(BooleanListEntry[] holder) {
        return holder[0] == null || holder[0].getValue();
    }

    private static Optional<Component> visibilityError(boolean icon, boolean dyeable, boolean fixed, boolean background) {
        return StyleSettings.showsAnything(icon, dyeable, fixed, background)
                ? Optional.empty()
                : Optional.of(Component.translatable(key("nothing_visible")));
    }

    private static SubCategoryListEntry buildStyleSubCategory(ConfigEntryBuilder entryBuilder, FavoriteStyle style) {
        StyleSettings settings = FavoriteItemsConfig.settingsFor(style);
        StyleSettings defaults = StyleSettings.defaultFor(style);

        var iconHolder = new BooleanListEntry[1];
        var dyeableHolder = new BooleanListEntry[1];
        var staticHolder = new BooleanListEntry[1];
        var backgroundHolder = new BooleanListEntry[1];

        Function<Boolean, Optional<Component>> iconError =
                v -> visibilityError(v, isOn(dyeableHolder), isOn(staticHolder), isOn(backgroundHolder));
        Function<Boolean, Optional<Component>> backgroundError =
                v -> visibilityError(isOn(iconHolder), isOn(dyeableHolder), isOn(staticHolder), v);

        var iconEnabledEntry = entryBuilder
                .startBooleanToggle(Component.translatable(key("icon_enabled")), settings.iconEnabled)
                .setDefaultValue(defaults.iconEnabled)
                .setErrorSupplier(iconError)
                .setSaveConsumer(value -> settings.iconEnabled = value)
                .build();
        iconHolder[0] = iconEnabledEntry;

        var dyeableEnabledEntry = entryBuilder
                .startBooleanToggle(Component.translatable(key("dyeable_enabled")), settings.dyeableEnabled)
                .setDefaultValue(defaults.dyeableEnabled)
                .setTooltip(Component.translatable(key("dyeable_enabled.tooltip")))
                .setRequirement(Requirement.isTrue(iconEnabledEntry))
                .setSaveConsumer(value -> settings.dyeableEnabled = value)
                .build();
        dyeableHolder[0] = dyeableEnabledEntry;

        var dyeableColorEntry = entryBuilder
                .startColorField(Component.translatable(key("dyeable_color")), settings.dyeableColor)
                .setDefaultValue(defaults.dyeableColor)
                .setTooltip(Component.translatable(key("dyeable_color.tooltip")))
                .setRequirement(Requirement.isTrue(dyeableEnabledEntry))
                .setSaveConsumer(value -> settings.dyeableColor = value)
                .build();

        var dyeableAboveEntry = aboveUnderToggle(entryBuilder, "layer_position", settings.dyeableAboveItem, defaults.dyeableAboveItem,
                Requirement.isTrue(dyeableEnabledEntry), value -> settings.dyeableAboveItem = value);

        var dyeableOpacityEntry = percentSlider(entryBuilder, "layer_opacity", settings.dyeableOpacityPercent, defaults.dyeableOpacityPercent,
                Requirement.isTrue(dyeableEnabledEntry), value -> settings.dyeableOpacityPercent = value);

        var staticEnabledEntry = entryBuilder
                .startBooleanToggle(Component.translatable(key("static_enabled")), settings.staticEnabled)
                .setDefaultValue(defaults.staticEnabled)
                .setTooltip(Component.translatable(key("static_enabled.tooltip")))
                .setRequirement(Requirement.isTrue(iconEnabledEntry))
                .setSaveConsumer(value -> settings.staticEnabled = value)
                .build();
        staticHolder[0] = staticEnabledEntry;

        var staticAboveEntry = aboveUnderToggle(entryBuilder, "layer_position", settings.staticAboveItem, defaults.staticAboveItem,
                Requirement.isTrue(staticEnabledEntry), value -> settings.staticAboveItem = value);

        var staticOrderEntry = aboveUnderToggle(entryBuilder, "static_order", settings.staticAboveDyeable, defaults.staticAboveDyeable,
                Requirement.all(
                        Requirement.isTrue(staticEnabledEntry),
                        Requirement.isTrue(dyeableEnabledEntry),
                        () -> dyeableAboveEntry.getValue().equals(staticAboveEntry.getValue())),
                value -> settings.staticAboveDyeable = value);

        var staticOpacityEntry = percentSlider(entryBuilder, "layer_opacity", settings.staticOpacityPercent, defaults.staticOpacityPercent,
                Requirement.isTrue(staticEnabledEntry), value -> settings.staticOpacityPercent = value);

        var backgroundEnabledEntry = entryBuilder
                .startBooleanToggle(Component.translatable(key("background_enabled")), settings.backgroundEnabled)
                .setDefaultValue(defaults.backgroundEnabled)
                .setErrorSupplier(backgroundError)
                .setSaveConsumer(value -> settings.backgroundEnabled = value)
                .build();
        backgroundHolder[0] = backgroundEnabledEntry;

        var backgroundColorEntry = entryBuilder
                .startColorField(Component.translatable(key("background_color")), settings.backgroundColor)
                .setDefaultValue(defaults.backgroundColor)
                .setRequirement(Requirement.isTrue(backgroundEnabledEntry))
                .setSaveConsumer(value -> settings.backgroundColor = value)
                .build();

        var backgroundOpacityEntry = percentSlider(entryBuilder, "background_opacity", settings.backgroundOpacityPercent, defaults.backgroundOpacityPercent,
                Requirement.isTrue(backgroundEnabledEntry), value -> settings.backgroundOpacityPercent = value);

        var shadowEnabledEntry = entryBuilder
                .startBooleanToggle(Component.translatable(key("shadow_enabled")), settings.shadowEnabled)
                .setDefaultValue(defaults.shadowEnabled)
                .setTooltip(Component.translatable(key("shadow_enabled.tooltip")))
                .setRequirement(Requirement.isTrue(iconEnabledEntry))
                .setSaveConsumer(value -> settings.shadowEnabled = value)
                .build();

        Supplier<StyleSettings> live = () -> {
            var s = new StyleSettings();
            s.iconEnabled = iconEnabledEntry.getValue();
            s.dyeableEnabled = dyeableEnabledEntry.getValue();
            s.dyeableColor = dyeableColorEntry.getValue();
            s.dyeableAboveItem = dyeableAboveEntry.getValue();
            s.dyeableOpacityPercent = dyeableOpacityEntry.getValue();
            s.staticEnabled = staticEnabledEntry.getValue();
            s.staticAboveItem = staticAboveEntry.getValue();
            s.staticAboveDyeable = staticOrderEntry.getValue();
            s.staticOpacityPercent = staticOpacityEntry.getValue();
            s.backgroundEnabled = backgroundEnabledEntry.getValue();
            s.backgroundColor = backgroundColorEntry.getValue();
            s.backgroundOpacityPercent = backgroundOpacityEntry.getValue();
            s.shadowEnabled = shadowEnabledEntry.getValue();
            return s;
        };

        StylePreviewRenderer.registerLive(new StylePreviewRenderer.Data(style, live));

        return styleSection(entryBuilder, style,
                iconEnabledEntry,
                shadowEnabledEntry,
                backgroundEnabledEntry,
                group(entryBuilder, key("group.dyeable"), dyeableEnabledEntry, dyeableColorEntry, dyeableAboveEntry, dyeableOpacityEntry),
                group(entryBuilder, key("group.static"), staticEnabledEntry, staticAboveEntry, staticOrderEntry, staticOpacityEntry),
                group(entryBuilder, key("group.background"), backgroundColorEntry, backgroundOpacityEntry));
    }

    private static SubCategoryListEntry buildMinimalStyleSubCategory(ConfigEntryBuilder entryBuilder) {
        FavoriteStyle style = FavoriteStyle.MINIMAL;
        StyleSettings settings = FavoriteItemsConfig.settingsFor(style);
        StyleSettings defaults = StyleSettings.defaultFor(style);

        var backgroundColorEntry = entryBuilder
                .startColorField(Component.translatable(key("background_color")), settings.backgroundColor)
                .setDefaultValue(defaults.backgroundColor)
                .setSaveConsumer(value -> settings.backgroundColor = value)
                .build();

        var backgroundOpacityEntry = percentSlider(entryBuilder, "background_opacity", settings.backgroundOpacityPercent, defaults.backgroundOpacityPercent,
                null, value -> settings.backgroundOpacityPercent = value);

        Supplier<StyleSettings> live = () -> {
            var s = StyleSettings.defaultFor(style);
            s.backgroundEnabled = true;
            s.backgroundColor = backgroundColorEntry.getValue();
            s.backgroundOpacityPercent = backgroundOpacityEntry.getValue();
            return s;
        };

        StylePreviewRenderer.registerLive(new StylePreviewRenderer.Data(style, live));

        return styleSection(entryBuilder, style, backgroundColorEntry, backgroundOpacityEntry);
    }

    private static SubCategoryListEntry styleSection(ConfigEntryBuilder entryBuilder, FavoriteStyle style, AbstractConfigListEntry... entries) {
        var section = entryBuilder
                .startSubCategory(Component.translatable(style.langKey()), new ArrayList<>(List.of(entries)))
                .setExpanded(style == FavoriteItemsConfig.activeStyle)
                .build();
        StylePreviewOverlay.registerSection(style, section, section::isExpanded);
        return section;
    }
}
