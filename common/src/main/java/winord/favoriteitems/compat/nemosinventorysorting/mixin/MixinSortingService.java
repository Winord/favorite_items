package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.nemosinventorysorting.NemoReflect;
import winord.favoriteitems.util.FavoriteSlots;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "com.nemonotfound.nemos.inventory.sorting.service.SortingService")
public abstract class MixinSortingService {
    @Inject(method = "sortSlotItems", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$leaveOutFavorites(AbstractContainerMenu menu, int startIndex, int endIndex,
                                                 CallbackInfoReturnable<List<Object>> cir) {
        var filtered = cir.getReturnValue().stream()
                .filter(item -> !FavoriteSlots.isFavorite(menu, NemoReflect.slotIndexOf(item)))
                .toList();
        cir.setReturnValue(filtered);
    }

    @Inject(method = "retrieveSlotSwapMap", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$assignAroundFavorites(List<Object> slotItems, int startIndex,
                                                     CallbackInfoReturnable<Map<Integer, Integer>> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        var menu = player.containerMenu;
        var result = new LinkedHashMap<Integer, Integer>();

        int destination = startIndex;
        for (var item : slotItems) {
            while (FavoriteSlots.isFavorite(menu, destination)) destination++;

            int current = NemoReflect.slotIndexOf(item);
            if (current != destination) result.put(current, destination);
            destination++;
        }

        cir.setReturnValue(result);
    }
}
