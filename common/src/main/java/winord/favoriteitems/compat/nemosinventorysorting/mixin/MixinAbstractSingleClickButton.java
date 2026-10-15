package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import winord.favoriteitems.util.FavoriteSlots;

import java.util.List;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.gui.components.buttons.AbstractSingleClickButton")
public abstract class MixinAbstractSingleClickButton {
    @ModifyVariable(method = "triggerClickForAllItems", at = @At("HEAD"), argsOnly = true, require = 0, remap = false)
    private List<Integer> favoriteitems$skipFavoriteSlots(List<Integer> slotIndices) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return slotIndices;

        var menu = screen.getMenu();

        return slotIndices.stream()
                .filter(index -> !FavoriteSlots.isFavorite(menu, index))
                .toList();
    }
}
