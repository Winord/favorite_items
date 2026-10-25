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
@Mixin(targets = "net.kyrptonaught.inventorysorter.client.sort.plan.ClientFallbackSortPlanBuilder", remap = false)
public abstract class MixinClientFallbackSortPlanBuilder {
    @Inject(method = "build", at = @At("HEAD"), remap = false, require = 0)
    private void favoriteitems$beginBuild(@Coerce Object sortScope, @Coerce Object sortType, String languageCode,
                                          List<?> rules, boolean sortIntoBundles, boolean sortIntoHotbarBundles,
                                          CallbackInfoReturnable<Object> cir) {
        InventorySorterCompat.beginClientBuild();
    }

    @Inject(method = "build", at = @At("RETURN"), remap = false, require = 0)
    private void favoriteitems$endBuild(@Coerce Object sortScope, @Coerce Object sortType, String languageCode,
                                        List<?> rules, boolean sortIntoBundles, boolean sortIntoHotbarBundles,
                                        CallbackInfoReturnable<Object> cir) {
        InventorySorterCompat.end();
    }

    @Inject(method = "stacks", at = @At("HEAD"), remap = false, require = 0)
    private static void favoriteitems$noteScope(List<?> slots, CallbackInfoReturnable<List<?>> cir) {
        InventorySorterCompat.beginClientSlots(slots);
    }
}
