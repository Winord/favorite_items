package winord.favoriteitems.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.config.StylePreviewOverlay;

@Pseudo
@Mixin(targets = "me.shedaniel.clothconfig2.gui.ClothConfigScreen$ListWidget", remap = false)
public abstract class MixinClothListRows {
    @Inject(method = "renderList", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$beginRowsClip(GuiGraphicsExtractor graphics, int startX, int startY, int mouseX, int mouseY,
                                             float delta, CallbackInfo ci) {
        StylePreviewOverlay.beginRowsClip(this, graphics);
    }

    @Inject(method = "renderList", at = @At("RETURN"), remap = false, require = 0)
    private void favoriteitems$endRowsClip(GuiGraphicsExtractor graphics, int startX, int startY, int mouseX, int mouseY,
                                           float delta, CallbackInfo ci) {
        StylePreviewOverlay.endRowsClip(graphics);
    }
}
