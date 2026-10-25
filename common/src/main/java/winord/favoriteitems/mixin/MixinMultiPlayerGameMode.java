package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.inventory.ContainerInput;
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

        if (ToolGuard.blocksItemUse(player, hand)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if (ProtectGuard.blocksItemUse(player, hand)) favoriteitems$failProtected(player, hand, cir);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardUseItemOn(
            LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (favoriteitems$holdForHandSwap(cir)) return;

        if (ToolGuard.blocksBlockUse(player, hand, hit)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        var blockingToggle = ToolGuard.blockingToggleForBlockUse(player, hand, hit);
        if (blockingToggle != null) {
            favoriteitems$failTool(blockingToggle, cir);
            return;
        }

        if (ProtectGuard.blocksBlockUse(player, hand, hit)) favoriteitems$failProtected(player, hand, cir);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardInteract(
            Player player, Entity target, EntityHitResult hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (favoriteitems$holdForHandSwap(cir)) return;

        if (ToolGuard.blocksEntityUse(player, hand, target)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        var blockingToggle = ToolGuard.blockingToggleForEntityUse(player, hand, target);
        if (blockingToggle != null) {
            favoriteitems$failTool(blockingToggle, cir);
            return;
        }

        if (ProtectGuard.blocksEntityUse(player, hand, target)) favoriteitems$failProtected(player, hand, cir);
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardStartDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (favoriteitems$blocksBlockBreaking(pos)) cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardContinueDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (favoriteitems$blocksBlockBreaking(pos)) cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardAttack(Player player, Entity target, CallbackInfo ci) {
        if (ToolGuard.blocksEntityAttack(player, target)) ci.cancel();
    }

    @Inject(method = "piercingAttack", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardPiercingAttack(PiercingWeapon weapon, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && ToolGuard.blocksPiercingAttack(player)) ci.cancel();
    }

    @Inject(method = "handlePickItemFromBlock", at = @At("HEAD"))
    private void favoriteitems$trackBlockPick(BlockPos pos, boolean includeData, CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) return;

        var stack = level.getBlockState(pos).getCloneItemStack(level, pos, includeData && player.hasInfiniteMaterials());
        favoriteitems$trackPick(player, stack);
    }

    @Inject(method = "handlePickItemFromEntity", at = @At("HEAD"))
    private void favoriteitems$trackEntityPick(Entity entity, boolean includeData, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null) favoriteitems$trackPick(player, entity.getPickResult());
    }

    private static void favoriteitems$trackPick(LocalPlayer player, ItemStack picked) {
        if (picked == null || picked.isEmpty()) return;

        var inventory = player.getInventory();
        int from = inventory.findSlotMatchingItem(picked);
        if (from >= 0 && !Inventory.isHotbarSlot(from)) FavoriteSlots.onPickItemRequested(inventory, from);
    }

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardModClick(
            int containerId, int slotId, int button, ContainerInput clickType, Player player, CallbackInfo ci)
    {
        if (clickType == ContainerInput.SWAP) {
            if (LitematicaCompat.isPicking()) {
                int usable = ProtectGuard.usableSwapSource(player, containerId, slotId);
                if (usable >= 0) {
                    ci.cancel();
                    ((MultiPlayerGameMode) (Object) this).handleContainerInput(containerId, usable, button, clickType, player);
                    return;
                }
            }

            if (ProtectGuard.handleModSwap(player, containerId, slotId, button)) ci.cancel();
        } else if (ExternalClickGuard.vetoes(player, containerId, slotId, button, clickType)) {
            ci.cancel();
        }
    }

    private static boolean favoriteitems$blocksBlockBreaking(BlockPos pos) {
        var player = Minecraft.getInstance().player;
        return player != null && ToolGuard.blocksBlockBreaking(player, pos);
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
