package winord.favoriteitems.compat.ipn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.ipn.IpnCompat;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.event.autorefill.AutoRefillHandler$ItemSlotMonitor", remap = false)
public abstract class MixinAutoRefillMonitor {
    @Inject(method = "shouldHandleItem", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$refillProtectedLastItem(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && IpnCompat.protectedLastItemNeedsRefill(this)) cir.setReturnValue(true);
    }

    @Inject(method = "handle", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$beginTopUp(CallbackInfo ci) {
        IpnCompat.beginTopUp(this);
    }

    @Inject(method = "handle", at = @At("RETURN"), remap = false, require = 0)
    private void favoriteitems$endTopUp(CallbackInfo ci) {
        IpnCompat.endTopUp();
    }
}
