package winord.favoriteitems.compat.ipn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.compat.ipn.IpnCompat;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.AdvancedContainer", remap = false)
public abstract class MixinAdvancedContainer {
    @Inject(method = "cleanCursor", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$trackCursorFavoriteReturn(CallbackInfo ci) {
        IpnCompat.trackCursorFavoriteReturn();
    }
}
