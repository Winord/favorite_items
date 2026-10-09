package winord.favoriteitems.util;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;

public final class HandSwapTracker<T> {
    private static final int TIMEOUT_TICKS = 100;

    public record Resolution(int hotbarIndex, boolean hotbarFavorite, boolean offhandFavorite) { }

    private final BiPredicate<T, T> sameStack;
    private final int offhandIndex;

    private boolean pending = false;
    private int hotbarIndex;
    private T expectedHotbar;
    private T expectedOffhand;
    private boolean targetHotbarFavorite;
    private boolean targetOffhandFavorite;
    private int age;

    public HandSwapTracker(BiPredicate<T, T> sameStack, int offhandIndex) {
        this.sameStack = sameStack;
        this.offhandIndex = offhandIndex;
    }

    public boolean hasPending() {
        return pending;
    }

    public boolean isPending(int slotIndex) {
        return pending && (slotIndex == hotbarIndex || slotIndex == offhandIndex);
    }

    public boolean isInFlight(int maxTicks) {
        return pending && age < maxTicks;
    }

    public void clear() {
        pending = false;
    }

    public void request(int hotbarIndex, T hotbar, T offhand, boolean hotbarFavorite, boolean offhandFavorite) {
        this.hotbarIndex = hotbarIndex;
        this.expectedHotbar = offhand;
        this.expectedOffhand = hotbar;
        this.targetHotbarFavorite = offhandFavorite;
        this.targetOffhandFavorite = hotbarFavorite;
        this.age = 0;
        this.pending = true;
    }

    public Optional<Resolution> tick(IntFunction<T> slotContents) {
        if (!pending) return Optional.empty();

        if (sameStack.test(slotContents.apply(hotbarIndex), expectedHotbar)
                && sameStack.test(slotContents.apply(offhandIndex), expectedOffhand)) {
            pending = false;
            return Optional.of(new Resolution(hotbarIndex, targetHotbarFavorite, targetOffhandFavorite));
        }

        age++;
        if (age > TIMEOUT_TICKS) pending = false;
        return Optional.empty();
    }
}
