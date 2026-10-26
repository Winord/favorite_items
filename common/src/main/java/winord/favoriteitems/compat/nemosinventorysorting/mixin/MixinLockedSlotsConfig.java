package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.models.config.LockedSlotsConfig")
public abstract class MixinLockedSlotsConfig {
    @Inject(method = "getLockedSlots", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$noLockedSlots(CallbackInfoReturnable<List<Object>> cir) {
        cir.setReturnValue(List.of());
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$refuseLock(@Coerce Object lockedSlot, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$treatAsUnlocked(@Coerce Object lockedSlot, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
