package winord.favoriteitems.compat.litematica.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.litematica.LitematicaCompat;
import winord.favoriteitems.util.ProtectGuard;

@Pseudo
@Mixin(targets = "fi.dy.masa.malilib.util.InventoryUtils", remap = false)
public abstract class MixinMalilibInventoryUtils {
    @Inject(method = "swapItemToMainHand", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void favoriteitems$fetchUsableStack(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (!LitematicaCompat.isPicking() || player == null) return;

        if (ProtectGuard.swapUsableStackIntoHand(player, LitematicaCompat.pickedStack())) cir.setReturnValue(true);
    }
}
