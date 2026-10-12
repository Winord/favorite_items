package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.util.FavoriteSlots;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.service.LockedSlotService")
public class MixinLockedSlotService {
    @Inject(method = "isLocked", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$favoritesReplaceLockedSlots(int index, int startIndex, CallbackInfoReturnable<Boolean> cir) {
        boolean favorite = Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                && FavoriteSlots.isFavorite(screen.getMenu(), index);

        cir.setReturnValue(favorite);
    }
}
