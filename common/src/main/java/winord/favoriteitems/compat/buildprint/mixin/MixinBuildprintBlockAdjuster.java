package winord.favoriteitems.compat.buildprint.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import winord.favoriteitems.compat.buildprint.BuildprintCompat;

@Pseudo
@Mixin(targets = "com.buildprint.client.assist.BlockAdjuster", remap = false)
public abstract class MixinBuildprintBlockAdjuster {
    @Redirect(
            method = "canGet(Lnet/minecraft/world/entity/player/Player;Ljava/util/function/Predicate;Z)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;", remap = false),
            remap = false, require = 0)
    private static ItemStack favoriteitems$countOnlyUsableStacks(Inventory inventory, int index) {
        return BuildprintCompat.assistVisibleItem(inventory, index, inventory.getItem(index));
    }

    @Redirect(
            method = "withTool(Lnet/minecraft/client/Minecraft;Ljava/util/function/Predicate;Lnet/minecraft/world/item/Item;Ljava/lang/Runnable;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;", remap = false),
            remap = false, require = 0)
    private static ItemStack favoriteitems$pickOnlyUsableStacks(Inventory inventory, int index) {
        return BuildprintCompat.assistVisibleItem(inventory, index, inventory.getItem(index));
    }
}
