package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.config.model.SettingsConfig")
public class MixinSettingsConfig {
    @Inject(method = "isSlotLockingEnabled", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$disableNemoSlotLocking(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
