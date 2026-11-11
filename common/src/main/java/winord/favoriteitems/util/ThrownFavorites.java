package winord.favoriteitems.util;

import com.google.gson.JsonArray;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import winord.favoriteitems.FavoriteItems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class ThrownFavorites {
    private static final int MAX_FLIGHTS = 32;
    private static final int THROW_TIMEOUT_TICKS = 200;
    private static final int PICKUP_TIMEOUT_TICKS = 40;
    private static final int PAIR_WINDOW_TICKS = 10;
    private static final double LAUNCH_RADIUS_SQR = 36.0;

    private static final class Throw {
        private final int slot;
        private final int tick;
        private boolean departed;
        private boolean favorite;

        private Throw(int slot, int tick) {
            this.slot = slot;
            this.tick = tick;
        }
    }

    private record Launch(UUID id, int tick) { }

    private record Token(UUID id, boolean favorite, int tick) { }

    private record Arrival(int slot, int tick) { }

    private static final Set<UUID> flights = new LinkedHashSet<>();
    private static final List<Throw> pendingThrows = new ArrayList<>();
    private static final List<Launch> launches = new ArrayList<>();
    private static final Set<UUID> knownTridents = new HashSet<>();
    private static final List<Token> tokens = new ArrayList<>();
    private static final List<Arrival> arrivals = new ArrayList<>();
    private static int usedSlot = -1;
    private static int now = 0;

    private ThrownFavorites() { }

    public static void reset() {
        flights.clear();
        clearTransient();
    }

    private static void clearTransient() {
        pendingThrows.clear();
        launches.clear();
        knownTridents.clear();
        tokens.clear();
        arrivals.clear();
        usedSlot = -1;
    }

    public static void load(JsonArray array) {
        for (var element : array) {
            try {
                flights.add(UUID.fromString(element.getAsString()));
            } catch (IllegalArgumentException | UnsupportedOperationException | IllegalStateException e) {
                FavoriteItems.LOGGER.warn("Ignoring invalid thrown favorite entry {}.", element);
            }
        }
        trimFlights();
    }

    static JsonArray toJson() {
        if (flights.isEmpty()) return null;

        var array = new JsonArray();
        flights.forEach(id -> array.add(id.toString()));
        return array;
    }

    public static void onItemTaken(int itemId, int collectorId) {
        var minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) return;

        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || collectorId != player.getId() || FavoriteSlots.isSuspended()) return;

        if (level.getEntity(itemId) instanceof ThrownTrident trident) {
            var id = trident.getUUID();
            tokens.add(new Token(id, flights.contains(id), now));
        }
    }

    static void tick(Inventory inventory, Item[] previous, Item[] current, boolean replaced) {
        now++;
        trackUse(inventory);
        if (!replaced && previous != null) detectMoves(previous, current);
        scanLaunches();
        pairThrows();
        pairPickups(current);
        expire();
    }

    private static boolean tracked(Item item) {
        return item instanceof TridentItem;
    }

    private static void trackUse(Inventory inventory) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        if (player.isUsingItem() && tracked(player.getUseItem().getItem())) {
            usedSlot = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? inventory.getSelectedSlot()
                    : FavoriteSlots.OFFHAND_INDEX;
        } else if (usedSlot >= 0) {
            if (pendingThrows.isEmpty()) snapshotKnown();
            pendingThrows.add(new Throw(usedSlot, now));
            usedSlot = -1;
        }
    }

    private static void snapshotKnown() {
        knownTridents.clear();
        launches.clear();
        forEachOwnedTrident(trident -> knownTridents.add(trident.getUUID()));
    }

    private static void forEachOwnedTrident(Consumer<ThrownTrident> action) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) return;

        for (var entity : level.entitiesForRendering()) {
            if (entity instanceof ThrownTrident trident && trident.getOwner() == player) action.accept(trident);
        }
    }

    private static void scanLaunches() {
        if (pendingThrows.isEmpty()) return;

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        forEachOwnedTrident(trident -> {
            if (knownTridents.add(trident.getUUID()) && trident.distanceToSqr(player) <= LAUNCH_RADIUS_SQR) {
                launches.add(new Launch(trident.getUUID(), now));
            }
        });
    }

    private static void detectMoves(Item[] previous, Item[] current) {
        for (int i = 0; i < FavoriteSlots.INVENTORY_SIZE; i++) {
            if (tracked(previous[i]) && current[i] == Items.AIR) {
                onDeparted(i);
            } else if (i < FavoriteSlots.ARMOR_START_INDEX && previous[i] == Items.AIR && tracked(current[i])) {
                arrivals.add(new Arrival(i, now));
            }
        }
    }

    private static void onDeparted(int slot) {
        for (var thrown : pendingThrows) {
            if (!thrown.departed && thrown.slot == slot) {
                thrown.departed = true;
                thrown.favorite = FavoriteSlots.isMarked(slot);
                return;
            }
        }
    }

    private static void pairThrows() {
        boolean changed = false;
        while (!launches.isEmpty()) {
            var departed = firstDeparted();
            if (departed == null) break;

            var launch = launches.remove(0);
            pendingThrows.remove(departed);
            if (departed.favorite) {
                flights.add(launch.id());
                changed = true;
            }
        }
        if (trimFlights()) changed = true;
        if (changed) FavoriteSlots.save();

        if (pendingThrows.isEmpty()) {
            launches.clear();
            knownTridents.clear();
        }
    }

    private static Throw firstDeparted() {
        for (var thrown : pendingThrows) {
            if (thrown.departed) return thrown;
        }
        return null;
    }

    private static boolean trimFlights() {
        boolean trimmed = false;
        while (flights.size() > MAX_FLIGHTS) {
            var eldest = flights.iterator();
            eldest.next();
            eldest.remove();
            trimmed = true;
        }
        return trimmed;
    }

    private static void pairPickups(Item[] current) {
        boolean changed = false;
        for (var token : new ArrayList<>(tokens)) {
            var arrival = nearestArrival(token, current);
            if (arrival == null) continue;

            tokens.remove(token);
            arrivals.remove(arrival);
            if (token.favorite()) {
                flights.remove(token.id());
                FavoriteSlots.setFavorite(arrival.slot(), true);
                changed = true;
            }
        }
        if (changed) FavoriteSlots.save();
    }

    private static Arrival nearestArrival(Token token, Item[] current) {
        Arrival best = null;
        for (var arrival : arrivals) {
            if (Math.abs(arrival.tick() - token.tick()) > PAIR_WINDOW_TICKS) continue;
            if (!tracked(current[arrival.slot()])) continue;
            if (best == null || arrival.slot() < best.slot()) best = arrival;
        }
        return best;
    }

    private static void expire() {
        pendingThrows.removeIf(thrown -> now - thrown.tick > THROW_TIMEOUT_TICKS);
        launches.removeIf(launch -> now - launch.tick() > THROW_TIMEOUT_TICKS);
        arrivals.removeIf(arrival -> now - arrival.tick() > PICKUP_TIMEOUT_TICKS);

        boolean changed = false;
        Iterator<Token> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            var token = iterator.next();
            if (now - token.tick() <= PICKUP_TIMEOUT_TICKS) continue;

            iterator.remove();
            if (token.favorite()) {
                flights.remove(token.id());
                changed = true;
            }
        }
        if (changed) FavoriteSlots.save();
    }
}
