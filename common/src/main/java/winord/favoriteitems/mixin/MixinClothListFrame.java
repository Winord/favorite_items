package winord.favoriteitems.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.config.StylePreviewOverlay;

@Pseudo
@Mixin(targets = "me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget", remap = false)
public abstract class MixinClothListFrame {
    @Inject(method = "renderDecorations", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$paintFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        StylePreviewOverlay.paintListFrame(this, graphics, mouseY);
    }

    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private int favoriteitems$hideMouseUnderPanel(int mouseX) {
        return StylePreviewOverlay.listMouseX(this, mouseX);
    }

    @Inject(method = "renderBackBackground", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$beginBackgroundClip(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        StylePreviewOverlay.beginRowsClip(this, graphics);
    }

    @Inject(method = "renderBackBackground", at = @At("RETURN"), remap = false, require = 0)
    private void favoriteitems$endBackgroundClip(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        StylePreviewOverlay.endRowsClip(graphics);
    }
}
