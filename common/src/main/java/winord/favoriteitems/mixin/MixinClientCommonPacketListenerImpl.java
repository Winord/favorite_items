package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.util.FavoriteSlots;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MixinClientCommonPacketListenerImpl {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$trackHandSwap(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ServerboundPlayerActionPacket action)
                || action.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) return;

        if (FavoriteSlots.isHandSwapPending()) {
            ci.cancel();
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player != null) FavoriteSlots.onHandSwapRequested(player.getInventory());
    }
}
