package winord.favoriteitems.compat.inventorysorter.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.inventorysorter.InventorySorterCompat;

@Pseudo
@Mixin(targets = "net.kyrptonaught.inventorysorter.sort.SortPriorityRules", remap = false)
public abstract class MixinSortPriorityRules {
    @Inject(method = "shouldIgnore", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void favoriteitems$keepFavoriteInPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (InventorySorterCompat.ignoresNextStack()) cir.setReturnValue(true);
    }
}
