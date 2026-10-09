package winord.favoriteitems.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.util.FavoriteSlots;

@Mixin(Slot.class)
public class MixinSlot {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$protectFavoriteFromGather(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!FavoriteSlots.isPickupAllActive()) return;

        var self = (Slot) (Object) this;
        if (FavoriteSlots.isFavorite(FavoriteSlots.resolveIndex(self.container, self.getContainerSlot()), self.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
