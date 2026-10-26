package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import winord.favoriteitems.compat.nemosinventorysorting.NemoCompat;

@Mixin(value = AbstractContainerScreen.class, priority = 500)
public abstract class MixinNemoAltInput {
    @ModifyVariable(method = "mouseClicked", at = @At("HEAD"), argsOnly = true, require = 0)
    private MouseButtonEvent favoriteitems$hideAltFromClick(MouseButtonEvent event) {
        return NemoCompat.withoutAlt(event);
    }

    @ModifyVariable(method = "mouseDragged", at = @At("HEAD"), argsOnly = true, require = 0)
    private MouseButtonEvent favoriteitems$hideAltFromDrag(MouseButtonEvent event) {
        return NemoCompat.withoutAlt(event);
    }
}
