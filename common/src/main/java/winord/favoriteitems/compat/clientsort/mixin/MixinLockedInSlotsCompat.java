package winord.favoriteitems.compat.clientsort.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.clientsort.ClientSortCompat;
import winord.favoriteitems.util.FavoriteSlots;

@Pseudo
@Mixin(targets = {
        "dev.terminalmc.clientsort.client.compat.lockedinslots.LockedInSlotsCompat",
        "dev.terminalmc.clientsort.client.compat.itemlocks.ItemLocksCompat"
})
public class MixinLockedInSlotsCompat {
    @Inject(method = "isLocked", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void favoriteitems$countFavoritesAsLocked(Slot slot, CallbackInfoReturnable<Boolean> cir) {
        if (slot.container instanceof Inventory && ClientSortCompat.topsUpFavorites()) return;

        if (FavoriteSlots.isFavorite(FavoriteSlots.resolveIndex(slot.container, slot.getContainerSlot()))) {
            cir.setReturnValue(true);
        }
    }
}
