package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.util.FavoriteSlots;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
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

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void favoriteitems$noteInventoryContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (packet.getContainerId() == 0) FavoriteSlots.onInventoryContent();
    }

    @Inject(method = "handlePlayerCombatKill", at = @At("TAIL"))
    private void favoriteitems$noteDeath(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && packet.getPlayerId() == player.getId()) FavoriteSlots.onPlayerKilled(player);
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void favoriteitems$noteRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        FavoriteSlots.onRespawned();
    }
}
