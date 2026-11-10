package winord.favoriteitems.compat.buildprint.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import winord.favoriteitems.compat.buildprint.BuildprintCompat;

@Pseudo
@Mixin(targets = "com.buildprint.client.assist.PlaceExecutor", remap = false)
public abstract class MixinBuildprintPlaceExecutor {
    @Redirect(
            method = "equip(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/item/Item;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;", remap = false),
            remap = false, require = 0)
    private static ItemStack favoriteitems$skipStackThatCannotBePlaced(Inventory inventory, int index) {
        return BuildprintCompat.visibleItem(inventory, index, inventory.getItem(index));
    }

    @Redirect(
            method = "equip(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/item/Item;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedItem()Lnet/minecraft/world/item/ItemStack;", remap = false),
            remap = false, require = 0)
    private static ItemStack favoriteitems$skipHeldStackThatCannotBePlaced(Inventory inventory) {
        return BuildprintCompat.visibleSelectedItem(inventory);
    }
}
