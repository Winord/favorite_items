package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.nemosinventorysorting.NemoReflect;
import winord.favoriteitems.util.FavoriteSlots;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.client.gui.components.AbstractSortAlphabeticallyButton")
public abstract class MixinAbstractSortAlphabeticallyButton {
    @Shadow(remap = false)
    protected abstract List<Map.Entry<Integer, ItemStack>> getSortedSlotItems(AbstractContainerMenu handler);

    @Inject(method = "getSortedSlotItems", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$leaveOutFavorites(AbstractContainerMenu handler,
                                             CallbackInfoReturnable<List<Map.Entry<Integer, ItemStack>>> cir) {
        var filtered = cir.getReturnValue().stream()
                .filter(entry -> !FavoriteSlots.isFavorite(handler, entry.getKey()))
                .toList();
        cir.setReturnValue(filtered);
    }

    @Inject(method = "createSortedItemMap", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$assignAroundFavorites(AbstractContainerMenu handler,
                                                       CallbackInfoReturnable<Map<Integer, Integer>> cir) {
        var sorted = getSortedSlotItems(handler);
        var result = new LinkedHashMap<Integer, Integer>();

        int destination = NemoReflect.startIndexOf(this);
        for (var entry : sorted) {
            while (FavoriteSlots.isFavorite(handler, destination)) destination++;

            if (!entry.getKey().equals(destination)) result.put(entry.getKey(), destination);
            destination++;
        }

        cir.setReturnValue(result);
    }
}
