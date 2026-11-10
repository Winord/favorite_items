package winord.favoriteitems.compat.buildprint.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.compat.buildprint.BuildprintCompat;

@Pseudo
@Mixin(targets = "com.buildprint.client.assist.DestroyAssist", remap = false)
public abstract class MixinBuildprintDestroyAssist {
    @Inject(
            method = "rank(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Holder;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void favoriteitems$skipToolThatWouldBreak(ItemStack stack, BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (BuildprintCompat.toolWouldBreakOn(stack, state)) cir.setReturnValue(Integer.MIN_VALUE);
    }
}
