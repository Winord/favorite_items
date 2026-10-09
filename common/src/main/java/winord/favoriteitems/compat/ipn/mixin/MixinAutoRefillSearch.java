package winord.favoriteitems.compat.ipn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.ipn.IpnCompat;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.event.AutoRefillHandler$ItemSlotMonitor$Companion", remap = false)
public abstract class MixinAutoRefillSearch {
    @Inject(method = "findCorrespondingSlot", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$enterRefillSearch(CallbackInfoReturnable<Integer> cir) {
        IpnCompat.enterRefillSearch();
    }

    @Inject(method = "findCorrespondingSlot", at = @At("RETURN"), remap = false, require = 0)
    private void favoriteitems$exitRefillSearch(CallbackInfoReturnable<Integer> cir) {
        IpnCompat.exitRefillSearch();
    }
}
