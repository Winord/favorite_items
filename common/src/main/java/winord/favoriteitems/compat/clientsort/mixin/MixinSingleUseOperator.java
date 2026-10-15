package winord.favoriteitems.compat.clientsort.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.clientsort.ClientSortCompat;
import winord.favoriteitems.util.FavoriteSlots;
import winord.favoriteitems.util.MoveSameFavorites;

@Pseudo
@Mixin(targets = "dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator")
public abstract class MixinSingleUseOperator {
    @Inject(method = "transferMatching", at = @At("HEAD"), require = 0, remap = false)
    private static void favoriteitems$leaveOneOfEachFavorite(AbstractContainerScreen<?> screen, Slot originSlot,
                                                             boolean onlyClient, CallbackInfoReturnable<Boolean> cir) {
        int origin = FavoriteSlots.playerInventoryIndex(originSlot);
        if (origin < 0) return;

        var hotbarScope = ClientSortCompat.hotbarScope();
        boolean originInHotbar = origin < FavoriteSlots.HOTBAR_SIZE;
        if (originInHotbar && hotbarScope == ClientSortCompat.HotbarScope.NONE) return;

        boolean mergedHotbar = hotbarScope == ClientSortCompat.HotbarScope.INVENTORY;
        MoveSameFavorites.leaveOneBehind(screen.getMenu(), slot -> {
            int index = FavoriteSlots.playerInventoryIndex(slot);
            return index >= 0 && (mergedHotbar || (index < FavoriteSlots.HOTBAR_SIZE) == originInHotbar);
        });
    }

    @Inject(method = "collectSlots", at = @At("HEAD"), require = 0, remap = false)
    private void favoriteitems$beginCollecting(Slot refSlot, @Coerce Object scope, CallbackInfoReturnable<Slot[]> cir) {
        ClientSortCompat.beginCollecting(this);
    }

    @Inject(method = "collectSlots", at = @At("RETURN"), require = 0, remap = false)
    private void favoriteitems$endCollecting(Slot refSlot, @Coerce Object scope, CallbackInfoReturnable<Slot[]> cir) {
        ClientSortCompat.endCollecting();
    }
}
