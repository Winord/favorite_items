package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.util.FavoriteSlots;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void favoriteitems$noteInventoryContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (packet.containerId() == 0) FavoriteSlots.onInventoryContent();
    }

    @Inject(method = "handlePlayerCombatKill", at = @At("TAIL"))
    private void favoriteitems$noteDeath(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && packet.playerId() == player.getId()) FavoriteSlots.onPlayerKilled(player);
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void favoriteitems$noteRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        FavoriteSlots.onRespawned();
    }
}
