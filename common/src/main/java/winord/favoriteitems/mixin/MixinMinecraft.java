package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.util.ToolGuard;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardStartAttack(CallbackInfoReturnable<Boolean> cir) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hit = minecraft.hitResult;
        if (player != null && hit != null && !player.isSpectator() && ToolGuard.blocksAttackStart(player, hit)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void favoriteitems$guardContinueAttack(boolean down, CallbackInfo ci) {
        if (!down) return;

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hit = minecraft.hitResult;
        if (player == null || hit == null || player.isSpectator() || !ToolGuard.blocksMining(player, hit)) return;

        var gameMode = minecraft.gameMode;
        if (gameMode != null) gameMode.stopDestroyBlock();
        ci.cancel();
    }
}
