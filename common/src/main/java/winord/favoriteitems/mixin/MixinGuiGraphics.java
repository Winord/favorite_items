package winord.favoriteitems.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import winord.favoriteitems.config.StylePreviewOverlay;

@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {
    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int favoriteitems$clipX1(int value, int x1, int y1, int x2, int y2) {
        return StylePreviewOverlay.clipScissor(0, x1, y1, x2, y2);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int favoriteitems$clipY1(int value, int x1, int y1, int x2, int y2) {
        return StylePreviewOverlay.clipScissor(1, x1, y1, x2, y2);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private int favoriteitems$clipX2(int value, int x1, int y1, int x2, int y2) {
        return StylePreviewOverlay.clipScissor(2, x1, y1, x2, y2);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 3, require = 0)
    private int favoriteitems$clipY2(int value, int x1, int y1, int x2, int y2) {
        return StylePreviewOverlay.clipScissor(3, x1, y1, x2, y2);
    }
}
