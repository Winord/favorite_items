package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.util.FavoriteSlots;

@Mixin({ArmorItem.class, ElytraItem.class})
public abstract class MixinEquipable implements Equipable {
    @Unique
    private boolean favoriteitems$handWasFavorite;
    @Unique
    private boolean favoriteitems$armorWasFavorite;

    @Inject(method = "use", at = @At("HEAD"))
    private void favoriteitems$rememberFavorites(
            Level level, Player user, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir)
    {
        if (!favoriteitems$isLocalUse(level, user)) return;

        favoriteitems$handWasFavorite = FavoriteSlots.isFavorite(favoriteitems$handIndex(user, hand));
        favoriteitems$armorWasFavorite = FavoriteSlots.isFavorite(favoriteitems$armorIndex());
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void favoriteitems$swapFavoriteWithArmor(
            Level level, Player user, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir)
    {
        if (!favoriteitems$isLocalUse(level, user)) return;
        if (!cir.getReturnValue().getResult().consumesAction()) return;
        if (favoriteitems$handWasFavorite == favoriteitems$armorWasFavorite) return;

        int handIndex = favoriteitems$handIndex(user, hand);
        int armorIndex = favoriteitems$armorIndex();
        FavoriteSlots.unfavorite(favoriteitems$handWasFavorite ? handIndex : armorIndex);
        FavoriteSlots.favorite(favoriteitems$handWasFavorite ? armorIndex : handIndex);
    }

    @Unique
    private static boolean favoriteitems$isLocalUse(Level level, Player user) {
        return level.isClientSide() && user == Minecraft.getInstance().player;
    }

    @Unique
    private static int favoriteitems$handIndex(Player user, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? user.getInventory().selected : FavoriteSlots.OFFHAND_INDEX;
    }

    @Unique
    private int favoriteitems$armorIndex() {
        return FavoriteSlots.ARMOR_START_INDEX + this.getEquipmentSlot().getIndex();
    }
}
