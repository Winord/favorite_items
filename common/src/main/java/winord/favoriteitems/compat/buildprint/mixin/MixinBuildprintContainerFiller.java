package winord.favoriteitems.compat.buildprint.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.buildprint.BuildprintCompat;

@Pseudo
@Mixin(targets = "com.buildprint.client.assist.ContainerFiller", remap = false)
public abstract class MixinBuildprintContainerFiller {
    @Inject(
            method = "findSource(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void favoriteitems$keepFavoritesOutOfContainers(AbstractContainerMenu menu, ItemStack want, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(BuildprintCompat.findSourceOutsideFavorites(menu, want));
    }
}
