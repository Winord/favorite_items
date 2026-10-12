package winord.favoriteitems.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BrushItem.class)
public interface BrushItemAccessor {
    @Invoker("calculateHitResult")
    HitResult favoriteitems$calculateHitResult(Player player);
}
