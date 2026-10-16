package winord.favoriteitems.mixin;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.shedaniel.clothconfig2.gui.widget.DynamicElementListWidget$ElementEntry", remap = false)
public abstract class MixinClothElementEntry {
    @Inject(method = "setFocused(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$keepFocusedChild(GuiEventListener child, CallbackInfo ci) {
        if (((ContainerEventHandler) (Object) this).getFocused() == child) ci.cancel();
    }
}
