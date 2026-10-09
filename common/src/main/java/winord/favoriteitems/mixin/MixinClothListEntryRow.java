package winord.favoriteitems.mixin;

import me.shedaniel.clothconfig2.api.ScissorsHandler;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.config.StylePreviewOverlay;

@Pseudo
@Mixin(targets = "me.shedaniel.clothconfig2.api.AbstractConfigListEntry", remap = false)
public abstract class MixinClothListEntryRow {
    @Inject(method = "render", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$trackRow(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                                        int mouseX, int mouseY, boolean isHovered, float delta, CallbackInfo ci) {
        StylePreviewOverlay.beginRow(this, graphics, x, y, mouseX, mouseY);
        if (!ScissorsHandler.INSTANCE.getScissorsAreas().isEmpty()) ScissorsHandler.INSTANCE.applyScissors();
    }
}
