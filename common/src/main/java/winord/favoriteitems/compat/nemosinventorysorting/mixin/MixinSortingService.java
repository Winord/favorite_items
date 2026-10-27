package winord.favoriteitems.compat.nemosinventorysorting.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.nemosinventorysorting.NemoReflect;
import winord.favoriteitems.compat.nemosinventorysorting.NemoSortPlan;
import winord.favoriteitems.util.FavoriteSlots;

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

    @Inject(method = "retrieveSlotSwaps", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void favoriteitems$assignAroundFavorites(List<Object> slotItems, int startIndex, int endIndex,
                                                     CallbackInfoReturnable<Map<Integer, Integer>> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        cir.setReturnValue(NemoSortPlan.swapMap(player.containerMenu, slotItems, startIndex, endIndex));
    }
}
