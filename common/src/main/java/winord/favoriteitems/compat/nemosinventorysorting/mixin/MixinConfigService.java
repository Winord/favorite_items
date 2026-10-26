package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.compat.nemosinventorysorting.NemoCompat;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.service.config.ConfigService")
public abstract class MixinConfigService {
    @Inject(method = "loadLockedSlotsConfig", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void favoriteitems$skipLockedSlotsLoad(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "writeConfig", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$skipLockedSlotsWrite(boolean update, String filePath, Object config, CallbackInfo ci) {
        if (NemoCompat.isLockedSlotsFile(filePath)) ci.cancel();
    }
}
