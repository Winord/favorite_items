package winord.favoriteitems.compat.sorted.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.compat.sorted.SortedCompat;

@Pseudo
@Mixin(targets = "com.sorted.sort.InventorySorter")
public abstract class MixinSortedDeathRestore {
    @Inject(method = "restoreFromDeathSnapshot", at = @At("HEAD"), require = 0, remap = false)
    private static void favoriteitems$beginRestore(AbstractContainerMenu handler, ItemStack[] snapshot, CallbackInfo ci) {
        SortedCompat.setRestoring(true);
    }

    @Inject(method = "restoreFromDeathSnapshot", at = @At("RETURN"), require = 0, remap = false)
    private static void favoriteitems$endRestore(AbstractContainerMenu handler, ItemStack[] snapshot, CallbackInfo ci) {
        SortedCompat.setRestoring(false);
    }
}
