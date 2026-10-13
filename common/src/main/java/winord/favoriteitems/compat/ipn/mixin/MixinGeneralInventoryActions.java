package winord.favoriteitems.compat.ipn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.compat.ipn.IpnCompat;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.GeneralInventoryActions", remap = false)
public abstract class MixinGeneralInventoryActions {
    @Inject(method = {"scrollUp", "scrollDown", "scrollToPlayer", "scrollToChest"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$skipFavoriteWheelMove(CallbackInfo ci) {
        if (IpnCompat.blocksWheelMove()) ci.cancel();
    }

    @Inject(method = "doMoveMatch(ZZ)V", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$leaveOneOfEachFavorite(boolean toPlayer, boolean unused, CallbackInfo ci) {
        IpnCompat.leaveOneBeforeMove(toPlayer);
    }
}
