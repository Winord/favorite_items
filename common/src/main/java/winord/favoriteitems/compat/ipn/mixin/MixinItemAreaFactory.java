package winord.favoriteitems.compat.ipn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import winord.favoriteitems.compat.ipn.IpnCompat;

import java.util.List;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.ItemArea$Companion", remap = false)
public abstract class MixinItemAreaFactory {
    @ModifyVariable(method = "invoke", at = @At("HEAD"), argsOnly = true, index = 2, remap = false, require = 0)
    private List<Integer> favoriteitems$skipEnderChestFavorites(List<Integer> slotIndices) {
        return IpnCompat.withoutEnderChestFavorites(slotIndices);
    }
}
