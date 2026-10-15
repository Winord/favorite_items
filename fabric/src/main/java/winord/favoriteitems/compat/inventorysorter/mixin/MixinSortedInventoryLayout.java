package winord.favoriteitems.compat.inventorysorter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.inventorysorter.InventorySorterCompat;

import java.util.List;

@Pseudo
@Mixin(targets = "net.kyrptonaught.inventorysorter.sort.SortedInventoryLayout", remap = false)
public abstract class MixinSortedInventoryLayout {
    @Inject(method = "splitIgnoredStacks", at = @At("HEAD"), remap = false, require = 0)
    private static void favoriteitems$beginSplit(List<?> input, @Coerce Object priorityRules, CallbackInfoReturnable<Object> cir) {
        InventorySorterCompat.beginSplit();
    }

    @Inject(method = "splitIgnoredStacks", at = @At("RETURN"), remap = false, require = 0)
    private static void favoriteitems$endSplit(List<?> input, @Coerce Object priorityRules, CallbackInfoReturnable<Object> cir) {
        InventorySorterCompat.endSplit();
    }
}
