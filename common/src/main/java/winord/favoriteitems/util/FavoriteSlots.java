package winord.favoriteitems.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.compat.clientsort.ClientSortCompat;
import winord.favoriteitems.compat.ipn.IpnCompat;
import winord.favoriteitems.compat.litematica.LitematicaCompat;
import winord.favoriteitems.config.FavoriteItemsConfig;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class FavoriteSlots {
    public static final int HOTBAR_SIZE = 9;
    public static final int ARMOR_START_INDEX = 36;
    public static final int OFFHAND_INDEX = 40;
    public static final int INVENTORY_SIZE = OFFHAND_INDEX + 1;
    public static final int ENDER_CHEST_OFFSET = 1000;

    private static final Gson GSON = new Gson();
    private static final String LAST_DEATH_KEY = "last_death";

    private static final Path WORLDS_DIR = FavoriteItems.CONFIG_DIR.resolve("worlds");
    private static final Path SERVERS_DIR = FavoriteItems.CONFIG_DIR.resolve("servers");
    private static final int MAX_FILE_NAME_BYTES = 100;

    private static final Item UNLEARNED = Items.AIR;

    private static final Map<Integer, Item> favorites = new TreeMap<>();
    private static Path currentFile = null;

    private static Item[] lastItems = null;
    private static boolean inventoryReplaced = false;
    private static int awaitingContentTicks = 0;
    private static final int CONTENT_WAIT_TICKS = 40;
    private static final int MASS_CHANGE_SLOTS = 5;

    private static final HandSwapTracker<ItemStack> handSwap = new HandSwapTracker<>(ItemStack::matches, OFFHAND_INDEX);
    private static boolean handSwapChangesFavorites = false;
    private static final int HAND_SWAP_GUARD_TICKS = 40;

    private static boolean leftCreative = false;

    private static final int PICK_TIMEOUT_TICKS = 100;
    private record PendingPick(int from, int to, ItemStack expectedFrom, ItemStack expectedTo, boolean fromFavorite, boolean toFavorite) { }
    private static PendingPick pendingPick = null;
    private static int pickAge = 0;

    private static final int RETURN_TIMEOUT_TICKS = 60;
    private record PendingReturn(ItemStack[] before, ItemStack stack) { }
    private static PendingReturn pendingReturn = null;
    private static int returnTicksLeft = 0;

    private static int screenClickDepth = 0;

    private static final ThreadLocal<Boolean> pickupAllActive = ThreadLocal.withInitial(() -> false);

    private static final long ENDER_CHEST_SYNC_GRACE_MS = 750;
    private static long enderChestOpenedAt = 0L;

    private FavoriteSlots() { }

    public static int enderChestIndex(int enderChestSlot) {
        return ENDER_CHEST_OFFSET + enderChestSlot;
    }

    public static boolean isEnderChestIndex(int index) {
        return index >= ENDER_CHEST_OFFSET;
    }

    public static boolean hasEnderChestFavorites() {
        if (isSuspended()) return false;

        for (var index : favorites.keySet()) {
            if (isEnderChestIndex(index)) return true;
        }
        return false;
    }

    public static boolean isEnderChestTitle(Component title) {
        return title.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("container.enderchest");
    }

    public static Path worldScopeFile(String saveFolderName) {
        return scopeFile(WORLDS_DIR, saveFolderName);
    }

    public static Path serverScopeFile(String address) {
        return scopeFile(SERVERS_DIR, address);
    }

    private static Path scopeFile(Path dir, String name) {
        var fileName = FileNames.truncateUtf8(FileNames.sanitize(name, "_"), MAX_FILE_NAME_BYTES);
        return dir.resolve(fileName + ".json");
    }

    public static void enterScope(Path file) {
        resetState();
        currentFile = file;

        if (Files.exists(file)) load(file);
        awaitingContentTicks = CONTENT_WAIT_TICKS;
    }

    public static void leaveScope() {
        resetState();
        currentFile = null;
    }

    private static void resetState() {
        favorites.clear();
        lastItems = null;
        inventoryReplaced = false;
        awaitingContentTicks = 0;
        DeathFavorites.reset();
        handSwap.clear();
        handSwapChangesFavorites = false;
        leftCreative = false;
        clearPendingMoves();
    }

    private static void load(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            var root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return;

            for (var entry : root.entrySet()) {
                if (entry.getKey().equals(LAST_DEATH_KEY)) {
                    DeathFavorites.load(entry.getValue().getAsJsonObject());
                } else {
                    favorites.put(Integer.parseInt(entry.getKey()), itemOf(entry.getValue().getAsString()));
                }
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException | NumberFormatException e) {
            FavoriteItems.LOGGER.warn("Failed to load favorite slots from {}, starting with an empty set.", file, e);
            favorites.clear();
            DeathFavorites.reset();
        }
    }

    static void save() {
        if (currentFile == null) return;

        var data = new JsonObject();
        favorites.forEach((index, item) ->
                data.addProperty(index.toString(), item == UNLEARNED ? "" : BuiltInRegistries.ITEM.getKey(item).toString()));
        var lastDeath = DeathFavorites.toJson();
        if (lastDeath != null) data.add(LAST_DEATH_KEY, lastDeath);

        try {
            JsonFiles.writeAtomically(GSON, currentFile, data);
        } catch (IOException e) {
            FavoriteItems.LOGGER.warn("Failed to save favorite slots to {}.", currentFile, e);
        }
    }

    private static Item itemOf(String itemId) {
        var id = itemId == null ? null : Identifier.tryParse(itemId);
        return id == null ? UNLEARNED : BuiltInRegistries.ITEM.getValue(id);
    }

    public static boolean isSuspended() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getAbilities().instabuild;
    }

    public static Set<Integer> currentFavorites() {
        var result = new HashSet<Integer>();
        for (var index : favorites.keySet()) {
            if (isFavorite(index)) result.add(index);
        }
        return result;
    }

    public static int playerInventoryIndex(Slot slot) {
        return slot.container instanceof Inventory ? slot.getContainerSlot() : -1;
    }

    public static int resolveIndex(Container container, int slotInContainer) {
        if (container instanceof Inventory) return slotInContainer;

        return Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                && isEnderChestTitle(screen.getTitle())
                ? enderChestIndex(slotInContainer)
                : -1;
    }

    public static int menuSlotOf(AbstractContainerMenu menu, int inventoryIndex) {
        for (var slot : menu.slots) {
            if (slot.container instanceof Inventory && slot.getContainerSlot() == inventoryIndex) return slot.index;
        }
        return -1;
    }

    public static void enterScreenClick() {
        screenClickDepth++;
    }

    public static void exitScreenClick() {
        if (screenClickDepth > 0) screenClickDepth--;
    }

    public static boolean isScreenClickActive() {
        return screenClickDepth > 0;
    }

    public static Set<Integer> occupiedInventoryFavorites(Inventory inventory) {
        var result = new HashSet<Integer>();
        if (isSuspended()) return result;

        for (var index : favorites.keySet()) {
            if (index < INVENTORY_SIZE && isFavorite(index, inventory.getItem(index))) result.add(index);
        }
        return result;
    }

    public static boolean isFavorite(int index) {
        if (index < 0 || isSuspended() || !favorites.containsKey(index)) return false;
        if (isEnderChestIndex(index)) return true;

        var player = Minecraft.getInstance().player;
        return player != null && isFavorite(index, player.getInventory().getItem(index));
    }

    public static boolean isFavorite(int index, ItemStack stack) {
        if (index < 0 || isSuspended()) return false;

        var expected = favorites.get(index);
        if (expected == null) return false;
        if (isEnderChestIndex(index)) return true;
        return !stack.isEmpty()
                && (expected == UNLEARNED || !FavoriteItemsConfig.lobbyFavorites || stack.getItem() == expected);
    }

    public static boolean isFavorite(AbstractContainerMenu menu, int slotId) {
        if (slotId < 0 || slotId >= menu.slots.size()) return false;

        var slot = menu.slots.get(slotId);
        return isFavorite(resolveIndex(slot.container, slot.getContainerSlot()), slot.getItem());
    }

    public static void favorite(int index) {
        if (index < 0 || isSuspended()) return;
        if (favorites.put(index, UNLEARNED) == null) save();
    }

    public static void unfavorite(int index) {
        if (index < 0 || isSuspended()) return;
        if (favorites.remove(index) != null) save();
    }

    public static boolean toggle(int index) {
        if (isSuspended()) return favorites.containsKey(index);

        var nowFavorite = !isFavorite(index);
        if (nowFavorite) favorites.put(index, UNLEARNED); else favorites.remove(index);
        save();
        return nowFavorite;
    }

    public static void setPickupAllActive(boolean active) {
        pickupAllActive.set(active);
    }

    public static boolean isPickupAllActive() {
        return pickupAllActive.get();
    }

    public static void onEnderChestOpened() {
        enderChestOpenedAt = System.currentTimeMillis();
    }

    public static void clientTick(Inventory inventory) {
        screenClickDepth = 0;
        IpnCompat.resetState();
        LitematicaCompat.resetState();
        ClientSortCompat.resetState();

        var swapResolution = handSwap.tick(inventory::getItem);

        if (isSuspended()) {
            clearPendingMoves();
            DeathFavorites.suspend();
            lastItems = null;
            leftCreative = true;
            return;
        }

        if (DeathFavorites.tick(Minecraft.getInstance(), inventory)) {
            clearPendingMoves();
            lastItems = null;
            return;
        }

        if (awaitingContentTicks > 0) {
            awaitingContentTicks--;
            clearPendingMoves();
            lastItems = null;
            DeathFavorites.pause();
            return;
        }

        swapResolution.filter(r -> handSwapChangesFavorites).ifPresent(FavoriteSlots::applyHandSwap);
        tickPendingPick(inventory);
        tickPendingReturn(inventory);

        updateFavorites(inventory);
        pruneEmptiedEnderChest();
        DeathFavorites.collectReturns(Minecraft.getInstance(), inventory);
    }

    static void pruneEmptiedInventory(Inventory inventory) {
        var emptied = favorites.keySet().stream()
                .filter(index -> index < INVENTORY_SIZE && inventory.getItem(index).isEmpty())
                .toList();
        if (emptied.isEmpty()) return;

        emptied.forEach(favorites::remove);
        save();
    }

    static void clearInventoryFavorites() {
        var stale = favorites.keySet().stream().filter(index -> index < INVENTORY_SIZE).toList();
        if (stale.isEmpty()) return;

        stale.forEach(favorites::remove);
        save();
    }

    public static void onInventoryContent() {
        awaitingContentTicks = 0;
        inventoryReplaced = true;
    }

    public static void onPlayerKilled(LocalPlayer player) {
        if (isSuspended()) return;

        DeathFavorites.onKilled(player);
    }

    public static void onRespawned() {
        awaitingContentTicks = CONTENT_WAIT_TICKS;
        inventoryReplaced = true;
    }

    public static boolean isHandSwapPending() {
        return handSwap.hasPending();
    }

    public static boolean isHandSwapInFlight() {
        return handSwap.isInFlight(HAND_SWAP_GUARD_TICKS);
    }

    public static void onCarriedFavoriteReturned(Inventory inventory, ItemStack carried) {
        if (isSuspended() || carried.isEmpty()) return;

        var before = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) before[i] = inventory.getItem(i).copy();
        pendingReturn = new PendingReturn(before, carried.copy());
        returnTicksLeft = RETURN_TIMEOUT_TICKS;
    }

    private static void tickPendingReturn(Inventory inventory) {
        if (pendingReturn == null) return;

        var returned = pendingReturn.stack();
        int best = -1;
        int bestGain = 0;
        int total = 0;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (i >= ARMOR_START_INDEX && i != OFFHAND_INDEX) continue;

            var now = inventory.getItem(i);
            if (now.isEmpty() || !ItemStack.isSameItemSameComponents(now, returned)) continue;

            var before = pendingReturn.before()[i];
            int gain = now.getCount() - (ItemStack.isSameItemSameComponents(before, returned) ? before.getCount() : 0);
            if (gain <= 0) continue;

            total += gain;
            if (gain > bestGain) {
                best = i;
                bestGain = gain;
            }
        }

        if (total < returned.getCount() && --returnTicksLeft > 0) return;

        if (best >= 0 && setFavorite(best, true)) save();
        clearPendingReturn();
    }

    private static void clearPendingReturn() {
        pendingReturn = null;
        returnTicksLeft = 0;
    }

    private static void clearPendingMoves() {
        clearPendingPick();
        clearPendingReturn();
    }

    public static void onHandSwapRequested(Inventory inventory) {
        int hotbarIndex = inventory.getSelectedSlot();
        var hotbar = inventory.getItem(hotbarIndex);
        var offhand = inventory.getItem(OFFHAND_INDEX);
        if (ItemStack.matches(hotbar, offhand)) return;

        var hotbarFavorite = isFavorite(hotbarIndex);
        var offhandFavorite = isFavorite(OFFHAND_INDEX);
        if (hotbarFavorite == offhandFavorite && !ToolGuard.anyEnabled() && !ToolGuard.breakingProtectEnabled()) return;

        handSwapChangesFavorites = !isSuspended();
        handSwap.request(hotbarIndex, hotbar.copy(), offhand.copy(), hotbarFavorite, offhandFavorite);
    }

    private static void applyHandSwap(HandSwapTracker.Resolution resolution) {
        var changed = setFavorite(resolution.hotbarIndex(), resolution.hotbarFavorite());
        changed |= setFavorite(OFFHAND_INDEX, resolution.offhandFavorite());
        if (changed) save();
    }

    public static void onPickItemRequested(Inventory inventory, int from) {
        if (isSuspended()) return;

        applyPendingPick();

        int to = inventory.getSuitableHotbarSlot();
        if (from < 0 || to < 0 || from == to) return;

        boolean fromFavorite = isFavorite(from);
        boolean toFavorite = isFavorite(to);
        if (fromFavorite == toFavorite) return;

        pendingPick = new PendingPick(from, to, inventory.getItem(to).copy(), inventory.getItem(from).copy(), fromFavorite, toFavorite);
        pickAge = 0;
    }

    private static boolean isPickPending(int index) {
        return pendingPick != null && (index == pendingPick.from() || index == pendingPick.to());
    }

    private static void tickPendingPick(Inventory inventory) {
        if (pendingPick == null) return;

        if (ItemStack.matches(inventory.getItem(pendingPick.to()), pendingPick.expectedTo())
                && ItemStack.matches(inventory.getItem(pendingPick.from()), pendingPick.expectedFrom())) {
            applyPendingPick();
        } else if (++pickAge > PICK_TIMEOUT_TICKS) {
            clearPendingPick();
        }
    }

    private static void applyPendingPick() {
        if (pendingPick == null) return;

        var changed = setFavorite(pendingPick.to(), pendingPick.fromFavorite());
        changed |= setFavorite(pendingPick.from(), pendingPick.toFavorite());
        clearPendingPick();
        if (changed) save();
    }

    private static void clearPendingPick() {
        pendingPick = null;
    }

    public static void swapFavorites(int a, int b) {
        if (a < 0 || b < 0 || a == b || isSuspended()) return;

        var favoriteA = isFavorite(a);
        var favoriteB = isFavorite(b);
        if (!favoriteA && !favoriteB) return;

        setFavorite(a, favoriteB);
        setFavorite(b, favoriteA);
        save();
    }

    private static boolean setFavorite(int index, boolean favorite) {
        return favorite ? favorites.put(index, UNLEARNED) != UNLEARNED : favorites.remove(index) != null;
    }

    private static void updateFavorites(Inventory inventory) {
        var current = new Item[INVENTORY_SIZE];
        int changed = 0;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            current[i] = inventory.getItem(i).getItem();
            if (lastItems != null && current[i] != lastItems[i]) changed++;
        }

        var previous = lastItems;
        boolean lobbyMode = FavoriteItemsConfig.lobbyFavorites;
        boolean replaced = previous == null || inventoryReplaced || changed >= MASS_CHANGE_SLOTS;
        boolean afterCreative = leftCreative;
        lastItems = current;
        inventoryReplaced = false;
        leftCreative = false;

        boolean dirty = false;
        for (var index : new ArrayList<>(favorites.keySet())) {
            if (index >= INVENTORY_SIZE || handSwap.isPending(index) || isPickPending(index)) continue;

            var expected = favorites.get(index);
            var now = current[index];
            var before = previous == null ? Items.AIR : previous[index];

            boolean realChange = !lobbyMode
                    || (!replaced && before != Items.AIR && (expected == UNLEARNED || before == expected));

            if (now == Items.AIR) {
                if (realChange || afterCreative) {
                    favorites.remove(index);
                    dirty = true;
                }
            } else if (expected == UNLEARNED || (now != expected && realChange)) {
                favorites.put(index, now);
                dirty = true;
            }
        }
        if (dirty) save();
    }

    private static void pruneEmptiedEnderChest() {
        if (favorites.isEmpty()) return;
        if (System.currentTimeMillis() - enderChestOpenedAt < ENDER_CHEST_SYNC_GRACE_MS) return;
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return;
        if (!isEnderChestTitle(screen.getTitle())) return;

        var emptied = new ArrayList<Integer>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory) continue;

            var index = enderChestIndex(slot.getContainerSlot());
            if (favorites.containsKey(index) && slot.getItem().isEmpty()) emptied.add(index);
        }
        if (emptied.isEmpty()) return;

        emptied.forEach(favorites::remove);
        save();
    }
}
