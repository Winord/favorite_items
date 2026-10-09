package winord.favoriteitems.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class MixinCreativeModeInventoryScreen<T extends AbstractContainerMenu> extends MixinAbstractContainerScreen<T> {
    @Inject(
        method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V",
        at = @At("HEAD"),
        cancellable = true)
    protected void applyFavoriteRules(
        Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci)
    {
        super.applyFavoriteRules(slot, slotId, button, clickType, ci);
    }

    protected MixinCreativeModeInventoryScreen(Component title) {
        super(title);
    }
}
