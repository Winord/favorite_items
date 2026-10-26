package winord.favoriteitems.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import winord.favoriteitems.FavoriteItems;
import winord.favoriteitems.config.FavoriteItemsConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DeathFavorites {
    private static final int HISTORY_TICKS = 5;
    private static final int SETTLE_TICKS = 10;
    private static final int REPLACEMENT_WAIT_TICKS = 200;
    private static final int RETURN_WINDOW_TICKS = 6000;
    private static final long RETURN_WINDOW_MILLIS = RETURN_WINDOW_TICKS * 50L;
    private static final double RETURN_RADIUS = 16.0;

    private static final class Wanted {
        final ItemStack template;
        int remaining;

        Wanted(ItemStack stack) {
            template = stack.copy();
            template.setCount(1);
            remaining = stack.getCount();
        }
    }

    private record Held(int index, ItemStack stack) { }

    private static final class DeathRecord {
        final String dimension;
        final Vec3 pos;
        final long gameTime;
        final long realTime;
        final List<Wanted> items = new ArrayList<>();

        DeathRecord(String dimension, Vec3 pos, long gameTime, long realTime) {
            this.dimension = dimension;
            this.pos = pos;
            this.gameTime = gameTime;
            this.realTime = realTime;
        }

        void add(ItemStack stack) {
            for (var wanted : items) {
                if (sameKind(wanted.template, stack)) {
                    wanted.remaining += stack.getCount();
                    return;
                }
            }
            items.add(new Wanted(stack));
        }
    }

    private static final ArrayDeque<List<Held>> history = new ArrayDeque<>();
    private static DeathRecord lastDeath = null;
    private static List<Held> lostFavorites = List.of();
    private static ItemStack[] lastStacks = null;

    private static boolean awaitingBaseline = false;
    private static boolean dead = false;
    private static LocalPlayer deadPlayer = null;
    private static int replacementWaitTicks = 0;
    private static int settleTicksLeft = 0;

    private DeathFavorites() { }

    static void reset() {
        pause();
        awaitingBaseline = false;
        lastDeath = null;
    }

    static void suspend() {
        pause();
        awaitingBaseline = false;
    }

    static void pause() {
        history.clear();
        lostFavorites = List.of();
        lastStacks = null;
        dead = false;
        deadPlayer = null;
        replacementWaitTicks = 0;
        settleTicksLeft = 0;
    }

    static void onKilled(LocalPlayer player) {
        if (!dead) startDeath(player);
    }

    static boolean tick(Minecraft minecraft, Inventory inventory) {
        var player = minecraft.player;
        if (player == null) return false;

        if (player.isDeadOrDying()) {
            if (!dead) startDeath(player);
            return true;
        }

        if (dead && player == deadPlayer && ++replacementWaitTicks <= REPLACEMENT_WAIT_TICKS) return true;

        if (dead) {
            dead = false;
            deadPlayer = null;
            settleTicksLeft = SETTLE_TICKS;
        }
        if (settleTicksLeft > 0) {
            if (--settleTicksLeft > 0) return true;

            finishDeath(inventory);
            return false;
        }

        if (FavoriteItemsConfig.deathFavorites) remember(inventory); else history.clear();
        return false;
    }

    private static void remember(Inventory inventory) {
        var stacks = new ArrayList<Held>();
        for (var index : FavoriteSlots.occupiedInventoryFavorites(inventory)) {
            stacks.add(new Held(index, inventory.getItem(index).copy()));
        }

        history.addLast(stacks);
        if (history.size() > HISTORY_TICKS) history.pollFirst();
    }

    private static void startDeath(LocalPlayer player) {
        dead = true;
        deadPlayer = player;
        replacementWaitTicks = 0;
        lastStacks = null;

        var lost = history.peekFirst();
        history.clear();
        if (lost == null || lost.isEmpty()) return;

        lostFavorites = lost;
        lastDeath = new DeathRecord(player.level().dimension().identifier().toString(), player.position(),
                player.level().getGameTime(), System.currentTimeMillis());
        for (var held : lost) lastDeath.add(held.stack());
        FavoriteSlots.save();
    }

    private static void finishDeath(Inventory inventory) {
        var lost = lostFavorites;
        lostFavorites = List.of();

        if (lost.isEmpty()) {
            FavoriteSlots.pruneEmptiedInventory(inventory);
            return;
        }

        boolean restoredInPlace = true;
        for (var held : lost) {
            if (!sameKind(inventory.getItem(held.index()), held.stack())) {
                restoredInPlace = false;
                break;
            }
        }
        if (restoredInPlace) {
            FavoriteSlots.pruneEmptiedInventory(inventory);
            discardRecord();
            return;
        }

        FavoriteSlots.clearInventoryFavorites();
        awaitingBaseline = true;
    }

    private static void discardRecord() {
        lastDeath = null;
        FavoriteSlots.save();
    }

    static void collectReturns(Minecraft minecraft, Inventory inventory) {
        var level = minecraft.level;
        if (lastDeath == null || level == null || !FavoriteItemsConfig.deathFavorites) {
            lastStacks = null;
            return;
        }

        boolean expired;
        boolean timeNotReached = false;
        if (usesWallClock(minecraft)) {
            expired = System.currentTimeMillis() - lastDeath.realTime > RETURN_WINDOW_MILLIS;
        } else {
            long age = level.getGameTime() - lastDeath.gameTime;
            expired = age > RETURN_WINDOW_TICKS;
            timeNotReached = age < 0;
        }
        if (expired) {
            discardRecord();
            return;
        }
        if (timeNotReached) {
            lastStacks = null;
            return;
        }

        var current = new ItemStack[FavoriteSlots.INVENTORY_SIZE];
        for (int i = 0; i < current.length; i++) current[i] = inventory.getItem(i).copy();
        boolean usesBaseline = lastStacks == null && awaitingBaseline;
        var previous = usesBaseline ? emptyStacks() : lastStacks;
        lastStacks = current;
        if (previous == null) return;
        if (!isNearDeathSite(minecraft)) {
            awaitingBaseline = false;
            return;
        }
        if (minecraft.screen instanceof AbstractContainerScreen<?>) {
            if (usesBaseline) lastStacks = null;
            return;
        }
        awaitingBaseline = false;

        boolean changed = false;
        for (var iterator = lastDeath.items.iterator(); iterator.hasNext(); ) {
            var item = iterator.next();
            int gained = total(current, item.template) - total(previous, item.template);
            if (gained <= 0) continue;

            for (int i = 0; i < current.length; i++) {
                if (isPickupSlot(i) && gain(previous[i], current[i], item.template) > 0) FavoriteSlots.favorite(i);
            }

            item.remaining -= gained;
            if (item.remaining <= 0) iterator.remove();
            changed = true;
        }
        if (!changed) return;

        if (lastDeath.items.isEmpty()) lastDeath = null;
        FavoriteSlots.save();
    }

    static JsonObject toJson() {
        if (lastDeath == null) return null;

        var json = new JsonObject();
        json.addProperty("dimension", lastDeath.dimension);
        json.addProperty("x", lastDeath.pos.x);
        json.addProperty("y", lastDeath.pos.y);
        json.addProperty("z", lastDeath.pos.z);
        json.addProperty("time", lastDeath.gameTime);
        json.addProperty("real_time", lastDeath.realTime);

        var items = new JsonArray();
        for (var item : lastDeath.items) {
            var entry = new JsonObject();
            var encoded = ItemStack.CODEC.encodeStart(registries().createSerializationContext(NbtOps.INSTANCE), item.template).result();
            if (encoded.isEmpty()) continue;
            entry.addProperty("item", encoded.get().toString());
            entry.addProperty("count", item.remaining);
            items.add(entry);
        }
        json.add("items", items);
        return json;
    }

    static void load(JsonObject json) {
        lastDeath = null;
        try {
            var loaded = new DeathRecord(
                    json.get("dimension").getAsString(),
                    new Vec3(json.get("x").getAsDouble(), json.get("y").getAsDouble(), json.get("z").getAsDouble()),
                    json.get("time").getAsLong(),
                    json.get("real_time").getAsLong());

            for (var element : json.getAsJsonArray("items")) {
                var entry = element.getAsJsonObject();
                var stack = ItemStack.CODEC.parse(registries().createSerializationContext(NbtOps.INSTANCE), TagParser.parseCompoundFully(entry.get("item").getAsString())).result().orElse(ItemStack.EMPTY);
                int count = entry.get("count").getAsInt();
                if (stack.isEmpty() || count <= 0) continue;

                stack.setCount(count);
                loaded.add(stack);
            }
            if (!loaded.items.isEmpty()) lastDeath = loaded;
        } catch (CommandSyntaxException | RuntimeException e) {
            FavoriteItems.LOGGER.warn("Ignoring an unreadable last_death record.", e);
        }
    }

    private static HolderLookup.Provider registries() {
        var level = Minecraft.getInstance().level;
        if (level == null) throw new IllegalStateException("No level to resolve registries");
        return level.registryAccess();
    }

    private static boolean usesWallClock(Minecraft minecraft) {
        return FavoriteItemsConfig.lobbyFavorites && minecraft.getSingleplayerServer() == null;
    }

    private static boolean isNearDeathSite(Minecraft minecraft) {
        var player = minecraft.player;
        return player != null
                && player.level().dimension().identifier().toString().equals(lastDeath.dimension)
                && player.position().distanceToSqr(lastDeath.pos) <= RETURN_RADIUS * RETURN_RADIUS;
    }

    private static boolean isPickupSlot(int index) {
        return index < FavoriteSlots.ARMOR_START_INDEX || index == FavoriteSlots.OFFHAND_INDEX;
    }

    private static boolean sameKind(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }

    private static ItemStack[] emptyStacks() {
        var stacks = new ItemStack[FavoriteSlots.INVENTORY_SIZE];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }

    private static int total(ItemStack[] stacks, ItemStack kind) {
        int total = 0;
        for (int i = 0; i < stacks.length; i++) {
            if (isPickupSlot(i) && sameKind(stacks[i], kind)) total += stacks[i].getCount();
        }
        return total;
    }

    private static int gain(ItemStack before, ItemStack after, ItemStack kind) {
        if (!sameKind(after, kind)) return 0;
        return after.getCount() - (sameKind(before, after) ? before.getCount() : 0);
    }
}
