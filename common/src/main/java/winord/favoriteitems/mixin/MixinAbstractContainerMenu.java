package winord.favoriteitems.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.util.FavoriteSlots;

@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu {
    @Inject(method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
    private void favoriteitems$beginPickupAll(int slotIndex, int button, ContainerInput clickType, Player player, CallbackInfo ci) {
        if (clickType == ContainerInput.PICKUP_ALL) FavoriteSlots.setPickupAllActive(true);
    }

    @Inject(method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V", at = @At("TAIL"))
    private void favoriteitems$endPickupAll(int slotIndex, int button, ContainerInput clickType, Player player, CallbackInfo ci) {
        if (clickType == ContainerInput.PICKUP_ALL) FavoriteSlots.setPickupAllActive(false);
    }
}
