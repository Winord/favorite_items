package winord.favoriteitems.compat.ipn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.compat.ipn.IpnCompat;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.ContainerClicker", remap = false)
public abstract class MixinContainerClicker {
    @Inject(method = "swap", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$mergeInsteadOfSwap(int sourceMenuSlot, int hotbarIndex, CallbackInfo ci) {
        if (IpnCompat.replaceSwapWithMerge(sourceMenuSlot, hotbarIndex)) ci.cancel();
    }

    @Inject(method = "shiftClick", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$keepFavoriteInPlace(int menuSlot, CallbackInfo ci) {
        if (IpnCompat.cancelsTopUpShiftClick(menuSlot)) ci.cancel();
    }
}
