package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.service.LockedSlotService")
public abstract class MixinLockedSlotService {
    @Inject(method = "isLocked", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$neverLocked(int index, int startIndex, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
