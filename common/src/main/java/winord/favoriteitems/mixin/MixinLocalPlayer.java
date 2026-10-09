package winord.favoriteitems.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.extension.ExtensionGui;
import winord.favoriteitems.util.ActionBarMessages;
import winord.favoriteitems.util.FavoriteSlots;
import winord.favoriteitems.util.SlotClicks;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends Player {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$blockFavoriteDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (FavoriteSlots.isHandSwapInFlight()) {
            cir.setReturnValue(false);
            return;
        }

        var selected = this.getInventory().selected;
        if (!FavoriteSlots.isFavorite(selected)) return;

        var stack = this.getInventory().getItem(selected);
        var mode = FavoriteItemsConfig.dropFavorites;
        if (!mode.blocks(stack, entireStack ? stack.getCount() : 1)) return;

        if (entireStack && mode.keepsLastItemOnDropAll(stack) && SlotClicks.throwAllButOne(inventoryMenu, FavoriteSlots.menuSlotOf(inventoryMenu, selected))) {
            cir.setReturnValue(true);
            return;
        }

        ExtensionGui.flashIcon(Minecraft.getInstance().gui);
        ActionBarMessages.dropBlocked();
        cir.setReturnValue(false);
    }

    public MixinLocalPlayer(Level level, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(level, pos, yaw, gameProfile);
    }
}
