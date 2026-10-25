package winord.favoriteitems.compat.inventorysorter.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.compat.inventorysorter.InventorySorterCompat;

@Pseudo
@Mixin(targets = "net.kyrptonaught.inventorysorter.inventory.PlayerInventorySorter", remap = false)
public abstract class MixinPlayerInventorySorter {
    @Inject(method = "sort", at = @At("HEAD"), remap = false, require = 0)
    private static void favoriteitems$beginSort(ServerPlayer player, @Coerce Object settings, String languageCode, CallbackInfo ci) {
        int first = Inventory.getSelectionSize();
        InventorySorterCompat.beginContainer(player.getInventory(), first, player.getInventory().getNonEquipmentItems().size() - first);
    }

    @Inject(method = "sort", at = @At("RETURN"), remap = false, require = 0)
    private static void favoriteitems$endSort(ServerPlayer player, @Coerce Object settings, String languageCode, CallbackInfo ci) {
        InventorySorterCompat.end();
    }
}
