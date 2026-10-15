package winord.favoriteitems.compat.inventorysorter.mixin;

import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.compat.inventorysorter.InventorySorterCompat;

import java.util.List;

@Pseudo
@Mixin(targets = "net.kyrptonaught.inventorysorter.inventory.ContainerInventorySorter", remap = false)
public abstract class MixinContainerInventorySorter {
    @Inject(method = "sort", at = @At("HEAD"), remap = false, require = 0)
    private static void favoriteitems$beginSort(Container container, int startSlot, int size, @Coerce Object sortType,
                                                String languageCode, List<?> sortPriorityRules, boolean sortIntoBundles,
                                                CallbackInfo ci) {
        InventorySorterCompat.beginContainer(container, startSlot, size);
    }

    @Inject(method = "sort", at = @At("RETURN"), remap = false, require = 0)
    private static void favoriteitems$endSort(Container container, int startSlot, int size, @Coerce Object sortType,
                                              String languageCode, List<?> sortPriorityRules, boolean sortIntoBundles,
                                              CallbackInfo ci) {
        InventorySorterCompat.end();
    }
}
