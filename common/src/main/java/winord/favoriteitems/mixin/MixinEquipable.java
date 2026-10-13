package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.util.FavoriteSlots;

@Mixin(Item.class)
public abstract class MixinEquipable {
    @Unique
    private boolean favoriteitems$handWasFavorite;
    @Unique
    private boolean favoriteitems$armorWasFavorite;
    @Unique
    private int favoriteitems$armorIndex = -1;

    @Inject(method = "use", at = @At("HEAD"))
    private void favoriteitems$rememberFavorites(
            Level level, Player user, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir)
    {
        favoriteitems$armorIndex = -1;
        if (!favoriteitems$isLocalUse(level, user)) return;

        var equippable = user.getItemInHand(hand).get(DataComponents.EQUIPPABLE);
        if (equippable == null || !equippable.swappable()) return;
        if (equippable.slot().getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return;

        favoriteitems$armorIndex = FavoriteSlots.ARMOR_START_INDEX + equippable.slot().getIndex();
        favoriteitems$handWasFavorite = FavoriteSlots.isFavorite(favoriteitems$handIndex(user, hand));
        favoriteitems$armorWasFavorite = FavoriteSlots.isFavorite(favoriteitems$armorIndex);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void favoriteitems$swapFavoriteWithArmor(
            Level level, Player user, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir)
    {
        int armorIndex = favoriteitems$armorIndex;
        favoriteitems$armorIndex = -1;
        if (armorIndex < 0) return;
        if (!favoriteitems$isLocalUse(level, user)) return;
        if (!cir.getReturnValue().consumesAction()) return;
        if (favoriteitems$handWasFavorite == favoriteitems$armorWasFavorite) return;

        int handIndex = favoriteitems$handIndex(user, hand);
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
}
