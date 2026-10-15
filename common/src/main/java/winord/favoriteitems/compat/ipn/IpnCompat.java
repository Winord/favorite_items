package winord.favoriteitems.compat.ipn;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.extension.ExtensionContainerScreen;
import winord.favoriteitems.util.FavoriteSlots;
import winord.favoriteitems.util.MoveSameFavorites;
import winord.favoriteitems.util.ProtectGuard;
import winord.favoriteitems.util.SlotClicks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

public final class IpnCompat {
    public static final String MOD_ID = "inventoryprofilesnext";

    private static final String LOCK_HANDLER = "org.anti_ad.mc.ipnext.event.LockSlotsHandler";
    private static final String LOCK_SETTINGS = "org.anti_ad.mc.ipnext.config.LockedSlotsSettings";
    private static final String MOD_SETTINGS = "org.anti_ad.mc.ipnext.config.ModSettings";
    private static final String ADVANCED_CONTAINER = "org.anti_ad.mc.ipnext.inventory.AdvancedContainer";
    private static final String GENERAL_ACTIONS = "org.anti_ad.mc.ipnext.inventory.GeneralInventoryActions";
    private static final String REFILL_MONITOR = "org.anti_ad.mc.ipnext.event.autorefill.AutoRefillHandler$ItemSlotMonitor$Companion";
    private static final String ITEM_AREA = "org.anti_ad.mc.ipnext.inventory.ItemArea";
    private static final String SLOT_MONITOR = "org.anti_ad.mc.ipnext.event.autorefill.AutoRefillHandler$ItemSlotMonitor";
    private static final String REFILL_HANDLER = "org.anti_ad.mc.ipnext.event.autorefill.AutoRefillHandler";
    private static final String CONTAINER_CLICKER = "org.anti_ad.mc.ipnext.inventory.ContainerClicker";
    private static final String IPN_ITEM_STACK = "org.anti_ad.mc.ipnext.item.ItemStack";
    private static final String IPN_ITEM_TYPE = "org.anti_ad.mc.ipnext.item.ItemType";

    private static final int HOTBAR_MENU_START = 36;

    private static int predicateDepth = 0;

    private static int refillSearchDepth = 0;

    private static int inputDepth = 0;

    private static int topUpMenuSlot = -1;

    private static Set<Integer> swipeProtected = Set.of();
    private static Set<Integer> swipeLockedBefore = Set.of();

    private static boolean statusLogged = false;

    private static final int UNKNOWN = 0, READY = 1, FAILED = 2;
    private static int handlerState = UNKNOWN;
    private static int settingsState = UNKNOWN;
    private static Object lockHandler;
    private static Method isMappedSlotLocked;
    private static Method isSlotLocked;
    private static Method storedLocks;
    private static Object disableInteractionConfig;
    private static Method configValue;

    private static int moveState = UNKNOWN;
    private static Object moveAllModifier;
    private static Object alwaysMoveAll;
    private static Object focusedOnlyModifier;
    private static Object justRefillModifier;
    private static Object includeHotbarModifier;
    private static Object alwaysIncludeHotbar;
    private static Method hotkeyPressing;
    private static Method booleanValue;

    private static int refillState = UNKNOWN;
    private static Object refillHandler;
    private static Method monitorCurrentSlot;
    private static Method monitorStoredSlot;
    private static Method monitorStoredItem;
    private static Method stackCount;
    private static Method stackType;
    private static Method typeItem;
    private static Method isBlackListed;

    private IpnCompat() { }

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static void enterPredicate() {
        predicateDepth++;
    }

    public static void exitPredicate() {
        if (predicateDepth > 0) predicateDepth--;
    }

    public static boolean insidePredicate() {
        return predicateDepth > 0;
    }

    public static void resetState() {
        predicateDepth = 0;
        inputDepth = 0;
        refillSearchDepth = 0;
        topUpMenuSlot = -1;
    }

    public static void enterRefillSearch() {
        refillSearchDepth++;
    }

    public static void exitRefillSearch() {
        if (refillSearchDepth > 0) refillSearchDepth--;
    }

    public static boolean hidesFavoritesFromRefill() {
        return refillSearchDepth > 0 && (FavoriteItemsConfig.ipnRefillSkipsFavorites || topUpMenuSlot >= 0);
    }

    public static boolean refillMayUseFavorites() {
        return refillSearchDepth > 0 && !FavoriteItemsConfig.ipnRefillSkipsFavorites && topUpMenuSlot < 0;
    }

    public static boolean protectedLastItemNeedsRefill(Object monitor) {
        if (!FavoriteItemsConfig.ipnRefillProtectedLastItem || !FavoriteItemsConfig.protectEnabled) return false;

        var player = Minecraft.getInstance().player;
        if (player == null || !isLoaded() || !prepareRefill()) return false;

        try {
            int menuSlot = (int) monitorCurrentSlot.invoke(monitor);
            if (protectedLastIndex(menuSlot) < 0 || menuSlot != (int) monitorStoredSlot.invoke(monitor)) return false;

            Object stored = monitorStoredItem.invoke(monitor);
            if (stored == null || (int) stackCount.invoke(stored) <= 0) return false;

            Object storedItem = typeItem.invoke(stackType.invoke(stored));
            if (storedItem != player.inventoryMenu.getSlot(menuSlot).getItem().getItem()) return false;

            return !Boolean.TRUE.equals(isBlackListed.invoke(null, refillHandler, stored));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public static void beginTopUp(Object monitor) {
        topUpMenuSlot = -1;
        if (!FavoriteItemsConfig.ipnRefillProtectedLastItem || !FavoriteItemsConfig.protectEnabled) return;
        if (!isLoaded() || !prepareRefill()) return;

        try {
            int menuSlot = (int) monitorCurrentSlot.invoke(monitor);
            if (protectedLastIndex(menuSlot) >= 0) topUpMenuSlot = menuSlot;
        } catch (ReflectiveOperationException | RuntimeException e) {
            topUpMenuSlot = -1;
        }
    }

    public static void endTopUp() {
        topUpMenuSlot = -1;
    }

    public static boolean cancelsTopUpShiftClick(int menuSlot) {
        return topUpMenuSlot >= 0 && menuSlot == topUpMenuSlot;
    }

    public static boolean replaceSwapWithMerge(int sourceMenuSlot, int hotbarIndex) {
        if (topUpMenuSlot < 0 || topUpMenuSlot != HOTBAR_MENU_START + hotbarIndex) return false;

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.screen != null) return true;

        var menu = player.inventoryMenu;
        if (sourceMenuSlot < 0 || sourceMenuSlot >= menu.slots.size() || sourceMenuSlot == topUpMenuSlot) return true;
        if (!menu.getCarried().isEmpty()) return true;

        ItemStack source = menu.getSlot(sourceMenuSlot).getItem();
        ItemStack target = menu.getSlot(topUpMenuSlot).getItem();
        if (source.isEmpty() || FavoriteSlots.isFavorite(menu, sourceMenuSlot) || !ItemStack.isSameItemSameComponents(source, target)) {
            return true;
        }

        SlotClicks.mergeInto(menu, sourceMenuSlot, topUpMenuSlot);
        return true;
    }

    private static int protectedLastIndex(int menuSlot) {
        var player = Minecraft.getInstance().player;
        if (player == null) return -1;

        var menu = player.inventoryMenu;
        if (menuSlot < 0 || menuSlot >= menu.slots.size()) return -1;

        var inventory = player.getInventory();
        int index = FavoriteSlots.playerInventoryIndex(menu.getSlot(menuSlot));
        if (index != inventory.getSelectedSlot() && index != FavoriteSlots.OFFHAND_INDEX) return -1;

        return ProtectGuard.protectsLastItem(player, index, inventory.getItem(index)) ? index : -1;
    }

    public static void leaveOneBeforeMove(boolean toPlayer) {
        if (!MoveSameFavorites.leavesOne() || !isLoaded() || !prepareMoveSettings()) return;
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return;

        var menu = screen.getMenu();
        try {
            boolean moveAll = pressing(moveAllModifier) != enabled(alwaysMoveAll);
            boolean focusedOnly = !moveAll && (pressing(focusedOnlyModifier) || !menu.getCarried().isEmpty());
            boolean justRefill = !moveAll && !focusedOnly && pressing(justRefillModifier);

            var focused = ItemStack.EMPTY;
            if (focusedOnly) {
                if (!menu.getCarried().isEmpty()) return;

                var hovered = screen instanceof ExtensionContainerScreen extension ? extension.favoriteitems$hoveredSlot() : null;
                if (hovered == null || hovered.getItem().isEmpty()) return;
                focused = hovered.getItem();
            }

            boolean includeHotbar = pressing(includeHotbarModifier) != enabled(alwaysIncludeHotbar) || (toPlayer && justRefill);
            int firstIndex = includeHotbar ? 0 : FavoriteSlots.HOTBAR_SIZE;
            Predicate<Slot> playerSide = slot -> FavoriteSlots.playerInventoryIndex(slot) >= firstIndex;

            if (toPlayer) {
                MoveSameFavorites.topUpFavorites(menu, playerSide, focused);
            } else {
                var move = moveAll || focusedOnly ? MoveSameFavorites.Move.ALL
                        : justRefill ? MoveSameFavorites.Move.REFILL : MoveSameFavorites.Move.MATCHING;
                MoveSameFavorites.leaveOneBehind(menu, playerSide, move, focused);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
        }
    }

    private static boolean pressing(Object hotkey) throws ReflectiveOperationException {
        return (boolean) hotkeyPressing.invoke(hotkey);
    }

    private static boolean enabled(Object option) throws ReflectiveOperationException {
        return (boolean) booleanValue.invoke(option);
    }

    public static List<Integer> withoutFavorites(List<Integer> slotNumbers) {
        var player = Minecraft.getInstance().player;
        if (player == null) return slotNumbers;

        var menu = player.inventoryMenu;
        return removeIf(slotNumbers, number -> FavoriteSlots.isFavorite(menu, number));
    }

    public static List<Integer> withoutEnderChestFavorites(List<Integer> slotNumbers) {
        if (refillSearchDepth > 0 || slotNumbers.isEmpty() || !FavoriteSlots.hasEnderChestFavorites()) return slotNumbers;

        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
                || !FavoriteSlots.isEnderChestTitle(screen.getTitle())) return slotNumbers;

        var menu = screen.getMenu();
        return removeIf(slotNumbers, number -> number >= 0 && number < menu.slots.size()
                && !(menu.getSlot(number).container instanceof Inventory)
                && FavoriteSlots.isFavorite(menu, number));
    }

    private static List<Integer> removeIf(List<Integer> numbers, IntPredicate remove) {
        List<Integer> kept = null;
        for (int i = 0; i < numbers.size(); i++) {
            int number = numbers.get(i);
            boolean removed = remove.test(number);
            if (removed && kept == null) kept = new ArrayList<>(numbers.subList(0, i));
            if (!removed && kept != null) kept.add(number);
        }
        return kept == null ? numbers : kept;
    }

    public static void enterInput() {
        inputDepth++;
    }

    public static void exitInput() {
        if (inputDepth > 0) inputDepth--;
    }

    public static Set<Integer> favoriteSlotsForPlanning() {
        var player = Minecraft.getInstance().player;
        return player == null ? Set.of() : FavoriteSlots.occupiedInventoryFavorites(player.getInventory());
    }

    public static <V> Map<Integer, V> lockableSlots(Map<Integer, V> all) {
        var context = Context.current();
        if (context == null) return all;

        Map<Integer, V> result = null;
        for (var index : all.keySet()) {
            if (!context.hidesFromLocking(index, inputDepth > 0)) continue;

            if (result == null) result = new LinkedHashMap<>(all);
            result.remove(index);
        }
        return result == null ? all : result;
    }

    public static void beginSwipe() {
        swipeProtected = Set.of();
        swipeLockedBefore = Set.of();

        var context = Context.current();
        if (context == null) return;

        var protectedSlots = new HashSet<Integer>();
        var lockedBefore = new HashSet<Integer>();
        for (int index = 0; index < FavoriteSlots.INVENTORY_SIZE; index++) {
            if (!context.hidesFromLocking(index, true)) continue;

            protectedSlots.add(index);
            if (context.stored.contains(index)) lockedBefore.add(index);
        }
        swipeProtected = protectedSlots;
        swipeLockedBefore = lockedBefore;
    }

    public static void endSwipe() {
        if (swipeProtected.isEmpty()) return;

        var context = Context.current();
        if (context == null) return;

        try {
            for (var index : swipeProtected) {
                if (swipeLockedBefore.contains(index)) context.stored.add(index);
                else context.stored.remove(index);
            }
        } catch (UnsupportedOperationException ignored) {
        } finally {
            swipeProtected = Set.of();
            swipeLockedBefore = Set.of();
        }
    }

    public static boolean cancelsScreenClick(Slot slot, int slotId, int button, ContainerInput clickType) {
        if (!isLoaded() || !prepareHandler() || !prepareSettings()) return false;

        try {
            if (!Boolean.TRUE.equals(configValue.invoke(disableInteractionConfig))) return false;
            if (slot != null && Boolean.TRUE.equals(isMappedSlotLocked.invoke(lockHandler, slot))) return true;

            return clickType == ContainerInput.SWAP && Boolean.TRUE.equals(isSlotLocked.invoke(lockHandler, button));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public static boolean blocksWheelMove() {
        return Minecraft.getInstance().screen instanceof ExtensionContainerScreen screen
                && screen.favoriteitems$blocksWheelMove();
    }

    public static void trackCursorFavoriteReturn() {
        if (Minecraft.getInstance().screen instanceof ExtensionContainerScreen screen) {
            screen.favoriteitems$trackCursorFavoriteReturn();
        }
    }

    private static boolean prepareHandler() {
        if (handlerState != UNKNOWN) return handlerState == READY;

        try {
            var handlerClass = Class.forName(LOCK_HANDLER);
            lockHandler = handlerClass.getField("INSTANCE").get(null);
            isMappedSlotLocked = handlerClass.getMethod("isMappedSlotLocked", Slot.class);
            isSlotLocked = handlerClass.getMethod("isSlotLocked", int.class);
            storedLocks = handlerClass.getMethod("getLockedInvSlotsStoredValue");

            handlerState = READY;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FavoriteItems.LOGGER.warn("Inventory Profiles Next is installed, but its slot locks could not be read; "
                    + "its lock overlay will not hide favorites.", e);
            handlerState = FAILED;
        }
        return handlerState == READY;
    }

    private static boolean prepareRefill() {
        if (refillState != UNKNOWN) return refillState == READY;

        try {
            var monitorClass = Class.forName(SLOT_MONITOR);
            var handlerClass = Class.forName(REFILL_HANDLER);
            var stackClass = Class.forName(IPN_ITEM_STACK);
            var typeClass = Class.forName(IPN_ITEM_TYPE);

            refillHandler = handlerClass.getField("INSTANCE").get(null);
            monitorCurrentSlot = monitorClass.getMethod("getCurrentSlotId");
            monitorStoredSlot = monitorClass.getMethod("getStoredSlotId");
            monitorStoredItem = monitorClass.getMethod("getStoredItem");
            stackCount = stackClass.getMethod("getCount");
            stackType = stackClass.getMethod("getItemType");
            typeItem = typeClass.getMethod("getItem");
            isBlackListed = handlerClass.getMethod("access$isBlackListed", handlerClass, stackClass);

            refillState = READY;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FavoriteItems.LOGGER.warn("Inventory Profiles Next is installed, but its auto refill could not be read; "
                    + "a protected last item will not be refilled.", e);
            refillState = FAILED;
        }
        return refillState == READY;
    }

    private static boolean prepareMoveSettings() {
        if (moveState != UNKNOWN) return moveState == READY;

        try {
            var settingsClass = Class.forName(MOD_SETTINGS);
            var settings = settingsClass.getField("INSTANCE").get(null);
            moveAllModifier = settingsClass.getMethod("getMOVE_ALL_MODIFIER").invoke(settings);
            alwaysMoveAll = settingsClass.getMethod("getALWAYS_MOVE_ALL").invoke(settings);
            focusedOnlyModifier = settingsClass.getMethod("getMOVE_FOCUS_MACH_MODIFIER").invoke(settings);
            justRefillModifier = settingsClass.getMethod("getMOVE_JUST_REFILL_MODIFIER").invoke(settings);
            includeHotbarModifier = settingsClass.getMethod("getINCLUDE_HOTBAR_MODIFIER").invoke(settings);
            alwaysIncludeHotbar = settingsClass.getMethod("getALWAYS_INCLUDE_HOTBAR").invoke(settings);
            hotkeyPressing = moveAllModifier.getClass().getMethod("isPressing");
            booleanValue = alwaysMoveAll.getClass().getMethod("getBooleanValue");

            moveState = READY;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FavoriteItems.LOGGER.warn("Inventory Profiles Next is installed, but its move settings could not be read; "
                    + "\"Move Same Items: Favorites\" will not apply to it.", e);
            moveState = FAILED;
        }
        return moveState == READY;
    }

    private static boolean prepareSettings() {
        if (settingsState != UNKNOWN) return settingsState == READY;

        try {
            var settingsClass = Class.forName(LOCK_SETTINGS);
            var settings = settingsClass.getField("INSTANCE").get(null);
            disableInteractionConfig = settingsClass.getMethod("getLOCK_SLOTS_DISABLE_USER_INTERACTION").invoke(settings);
            configValue = findMethod(disableInteractionConfig.getClass(), "getBooleanValue", "getValue");

            settingsState = READY;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FavoriteItems.LOGGER.warn("Inventory Profiles Next is installed, but its lock settings could not be read; "
                    + "a locked slot's cancelled click will not be recognised.", e);
            settingsState = FAILED;
        }
        return settingsState == READY;
    }

    private record Context(Set<Integer> stored, ExtensionContainerScreen screen, Inventory inventory) {
        @SuppressWarnings("unchecked")
        static Context current() {
            if (!isLoaded() || !prepareHandler()) return null;

            var minecraft = Minecraft.getInstance();
            if (minecraft.player == null || !(minecraft.screen instanceof ExtensionContainerScreen screen)) return null;

            try {
                var stored = (Set<Integer>) storedLocks.invoke(lockHandler);
                return stored == null ? null : new Context(stored, screen, minecraft.player.getInventory());
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }

        boolean hidesFromLocking(int index, boolean clickingOrSwiping) {
            boolean locked = stored.contains(index);

            if (FavoriteSlots.isFavorite(index) && !inventory.getItem(index).isEmpty()) return !locked || clickingOrSwiping;
            return !locked && screen.favoriteitems$wouldToggleSlot(index);
        }
    }

    private static Method findMethod(Class<?> type, String... names) throws NoSuchMethodException {
        for (var name : names) {
            try {
                return type.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + names[0]);
    }

    public static void logStatus() {
        if (statusLogged || !isLoaded()) return;
        statusLogged = true;

        var loader = IpnCompat.class.getClassLoader();
        var missing = new ArrayList<String>();
        for (var name : List.of(LOCK_HANDLER, LOCK_SETTINGS, MOD_SETTINGS, ADVANCED_CONTAINER, GENERAL_ACTIONS, REFILL_MONITOR, ITEM_AREA,
                SLOT_MONITOR, CONTAINER_CLICKER)) {
            try {
                Class.forName(name, false, loader);
            } catch (ClassNotFoundException | LinkageError e) {
                missing.add(name);
            }
        }

        if (missing.isEmpty()) {
            FavoriteItems.LOGGER.info("Inventory Profiles Next found: favorites are treated as locked slots by it.");
        } else {
            FavoriteItems.LOGGER.warn("Inventory Profiles Next found, but these classes are missing, so part of the "
                    + "compat is inactive: {}", missing);
        }
    }
}
