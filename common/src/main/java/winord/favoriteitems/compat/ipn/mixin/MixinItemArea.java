package winord.favoriteitems.compat.ipn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.ipn.IpnCompat;

import java.util.List;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.ItemArea", remap = false)
public abstract class MixinItemArea {
    @Inject(method = "getSlotIndices", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$skipFavoriteSlots(CallbackInfoReturnable<List<Integer>> cir) {
        var slots = cir.getReturnValue();
        if (!IpnCompat.hidesFavoritesFromRefill()) return;

        var visible = IpnCompat.withoutFavorites(slots);
        if (visible != slots) cir.setReturnValue(visible);
    }
}
