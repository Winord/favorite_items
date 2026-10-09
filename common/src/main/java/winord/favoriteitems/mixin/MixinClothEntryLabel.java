package winord.favoriteitems.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.config.StylePreviewOverlay;

@Pseudo
@Mixin(targets = "me.shedaniel.clothconfig2.api.AbstractConfigEntry", remap = false)
public abstract class MixinClothEntryLabel {
    @Inject(method = "getDisplayedFieldName", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$fitLabel(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(StylePreviewOverlay.fitLabel(this, cir.getReturnValue()));
    }
}
