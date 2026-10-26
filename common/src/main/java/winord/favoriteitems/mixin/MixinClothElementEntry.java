package winord.favoriteitems.mixin;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.config.ClothSelectionBack;

@Pseudo
@Mixin(targets = "me.shedaniel.clothconfig2.gui.widget.DynamicElementListWidget$ElementEntry", remap = false)
public abstract class MixinClothElementEntry {
    @Inject(method = "setFocused(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$keepFocusedChild(GuiEventListener child, CallbackInfo ci) {
        if (((ContainerEventHandler) (Object) this).getFocused() == child) ci.cancel();
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$stepSelectionBack(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ClothSelectionBack.stepBackOnRightClick(this, event)) cir.setReturnValue(true);
    }
}
