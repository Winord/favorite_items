package winord.favoriteitems.compat.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.compat.litematica.LitematicaCompat;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.util.InventoryUtils", remap = false)
public abstract class MixinLitematicaInventoryUtils {
    @Inject(method = {"schematicWorldPickBlock", "setPickedItemToHand"}, at = @At("HEAD"), remap = false, require = 0)
    private static void favoriteitems$enterPick(CallbackInfo ci) {
        LitematicaCompat.enterPick();
    }

    @Inject(method = {"schematicWorldPickBlock", "setPickedItemToHand"}, at = @At("RETURN"), remap = false, require = 0)
    private static void favoriteitems$exitPick(CallbackInfo ci) {
        LitematicaCompat.exitPick();
    }
}
