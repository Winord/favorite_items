package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.litematica.LitematicaCompat;
import winord.favoriteitems.util.ProtectGuard;

@Mixin(Inventory.class)
public abstract class MixinInventory {
    @Inject(method = "findSlotMatchingItem", at = @At("RETURN"), cancellable = true)
    private void favoriteitems$skipStackThatCannotBePlaced(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!LitematicaCompat.isPicking()) return;

        LitematicaCompat.notePickedStack(stack);
        int found = cir.getReturnValue();
        var inventory = (Inventory) (Object) this;
        var player = Minecraft.getInstance().player;
        if (found < 0 || player == null || inventory != player.getInventory()) return;
        if (!ProtectGuard.hidesFromBuilding(player, found, inventory.getItem(found))) return;

        cir.setReturnValue(ProtectGuard.findBuildableStack(player, stack));
    }
}
