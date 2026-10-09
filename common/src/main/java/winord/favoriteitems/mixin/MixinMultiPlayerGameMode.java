package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.litematica.LitematicaCompat;
import winord.favoriteitems.util.ActionBarMessages;
import winord.favoriteitems.util.ExternalClickGuard;
import winord.favoriteitems.util.FavoriteSlots;
import winord.favoriteitems.util.ProtectGuard;
import winord.favoriteitems.util.ToolGuard;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardUseItem(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (favoriteitems$holdForHandSwap(cir)) return;

        if (ProtectGuard.blocksItemUse(player, hand)) favoriteitems$failProtected(player, hand, cir);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardUseItemOn(
            LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (favoriteitems$holdForHandSwap(cir)) return;

        var blockingToggle = ToolGuard.blockingToggleForBlockUse(player, hand, hit);
        if (blockingToggle != null) {
            favoriteitems$failTool(blockingToggle, cir);
            return;
        }

        if (ProtectGuard.blocksBlockUse(player, hand, hit)) favoriteitems$failProtected(player, hand, cir);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardInteract(
            Player player, Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (favoriteitems$holdForHandSwap(cir)) return;

        var blockingToggle = ToolGuard.blockingToggleForEntityUse(player, hand, target);
        if (blockingToggle != null) {
            favoriteitems$failTool(blockingToggle, cir);
            return;
        }

        if (ProtectGuard.blocksEntityUse(player, hand, target)) favoriteitems$failProtected(player, hand, cir);
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardInteractAt(
            Player player, Entity target, EntityHitResult hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (favoriteitems$holdForHandSwap(cir)) return;

        if (ProtectGuard.blocksEntityUse(player, hand, target)) favoriteitems$failProtected(player, hand, cir);
    }

    @Inject(method = "handlePickItem", at = @At("HEAD"))
    private void favoriteitems$trackPickItem(int slot, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null) FavoriteSlots.onPickItemRequested(player.getInventory(), slot);
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardModClick(
            int containerId, int slotId, int button, ClickType clickType, Player player, CallbackInfo ci)
    {
        if (clickType == ClickType.SWAP) {
            if (LitematicaCompat.isPicking()) {
                int usable = ProtectGuard.usableSwapSource(player, containerId, slotId);
                if (usable >= 0) {
                    ci.cancel();
                    ((MultiPlayerGameMode) (Object) this).handleInventoryMouseClick(containerId, usable, button, clickType, player);
                    return;
                }
            }

            if (ProtectGuard.handleModSwap(player, containerId, slotId, button)) ci.cancel();
        } else if (ExternalClickGuard.vetoes(player, containerId, slotId, button, clickType)) {
            ci.cancel();
        }
    }

    private static boolean favoriteitems$holdForHandSwap(CallbackInfoReturnable<InteractionResult> cir) {
        if (!FavoriteSlots.isHandSwapInFlight()) return false;

        cir.setReturnValue(InteractionResult.FAIL);
        return true;
    }

    private static void favoriteitems$failProtected(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        ProtectGuard.notifyBlocked(player, hand);
        cir.setReturnValue(InteractionResult.FAIL);
    }

    private static void favoriteitems$failTool(String toggleLabelKey, CallbackInfoReturnable<InteractionResult> cir) {
        ActionBarMessages.toolBlocked(toggleLabelKey);
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
