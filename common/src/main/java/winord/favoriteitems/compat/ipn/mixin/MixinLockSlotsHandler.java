package winord.favoriteitems.compat.ipn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.ipn.IpnCompat;

import java.util.LinkedHashSet;
import java.util.Map;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.event.LockSlotsHandler", remap = false)
public abstract class MixinLockSlotsHandler {
    @Inject(method = "getLockedInvSlots", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$addFavorites(CallbackInfoReturnable<Iterable<Integer>> cir) {
        if (IpnCompat.insidePredicate() || IpnCompat.refillMayUseFavorites()) return;

        var favorites = IpnCompat.favoriteSlotsForPlanning();
        if (favorites.isEmpty()) return;

        var merged = new LinkedHashSet<Integer>();
        cir.getReturnValue().forEach(merged::add);
        merged.addAll(favorites);
        cir.setReturnValue(merged);
    }

    @Inject(method = {"isSlotLocked", "isMappedSlotLocked", "isQMoveActionAllowed", "isHotbarQMoveActionAllowed"},
            at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$enterPredicate(CallbackInfoReturnable<Boolean> cir) {
        IpnCompat.enterPredicate();
    }

    @Inject(method = {"isSlotLocked", "isMappedSlotLocked", "isQMoveActionAllowed", "isHotbarQMoveActionAllowed"},
            at = @At("RETURN"), remap = false, require = 0)
    private void favoriteitems$exitPredicate(CallbackInfoReturnable<Boolean> cir) {
        IpnCompat.exitPredicate();
    }

    @Inject(method = "getSlotLocations", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$hideFavorites(CallbackInfoReturnable<Map<Integer, Object>> cir) {
        cir.setReturnValue(IpnCompat.lockableSlots(cir.getReturnValue()));
    }

    @Inject(method = "onCancellableInput", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$enterInput(CallbackInfoReturnable<Boolean> cir) {
        IpnCompat.enterInput();
    }

    @Inject(method = "onCancellableInput", at = @At("RETURN"), remap = false, require = 0)
    private void favoriteitems$exitInput(CallbackInfoReturnable<Boolean> cir) {
        IpnCompat.exitInput();
    }

    @Inject(method = "onTickInGame", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$beginSwipe(CallbackInfo ci) {
        IpnCompat.beginSwipe();
    }

    @Inject(method = "onTickInGame", at = @At("RETURN"), remap = false, require = 0)
    private void favoriteitems$endSwipe(CallbackInfo ci) {
        IpnCompat.endSwipe();
    }
}
