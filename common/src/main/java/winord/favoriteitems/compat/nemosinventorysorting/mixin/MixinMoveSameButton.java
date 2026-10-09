package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.nemosinventorysorting.NemoReflect;
import winord.favoriteitems.util.MoveSameFavorites;

import java.util.List;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.client.gui.components.MoveSameButton")
public abstract class MixinMoveSameButton {
    @Inject(method = "getItemSlotsToInteractWith", at = @At("HEAD"), require = 0, remap = false)
    private void favoriteitems$leaveOneOfEachFavorite(AbstractContainerMenu menu, CallbackInfoReturnable<List<Integer>> cir) {
        int start = NemoReflect.startIndexOf(this);
        int end = NemoReflect.endIndexOf(this, menu);
        MoveSameFavorites.leaveOneBehind(menu, slot -> slot.index >= start && slot.index < end);
    }
}
