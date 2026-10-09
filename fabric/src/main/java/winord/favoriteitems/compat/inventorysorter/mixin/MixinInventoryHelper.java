package winord.favoriteitems.compat.inventorysorter.mixin;

import net.kyrptonaught.inventorysorter.InventoryHelper;
import net.kyrptonaught.inventorysorter.SortCases;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import winord.favoriteitems.util.FavoriteSlots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@Pseudo @Mixin(InventoryHelper.class)
public class MixinInventoryHelper {
    @Unique private static ItemStack[] favoritePositions;
    @Unique private static final Set<ItemStack> favoriteStacksThisSort = Collections.newSetFromMap(new IdentityHashMap<>());

    @Inject(
            method = "sortInv(Lnet/minecraft/world/Container;IILnet/kyrptonaught/inventorysorter/SortCases$SortType;)V",
            at = @At("HEAD"))
    private static void favoriteitems$startSort(
            Container inv, int startSlot, int invSize, SortCases.SortType sortType, CallbackInfo ci)
    {
        favoritePositions = new ItemStack[invSize];
        favoriteStacksThisSort.clear();
    }

    @Inject(
            method = "sortInv(Lnet/minecraft/world/Container;IILnet/kyrptonaught/inventorysorter/SortCases$SortType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/kyrptonaught/inventorysorter/InventoryHelper;addStackWithMerge(Ljava/util/List;Lnet/minecraft/world/item/ItemStack;)V"),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void favoriteitems$collectFavoriteStacks(
            Container inv, int startSlot, int invSize, SortCases.SortType sortType, CallbackInfo ci, List<ItemStack> stacks,
            int i)
    {
        var itemStack = inv.getItem(startSlot + i);
        var isFavorite = FavoriteSlots.isFavorite(FavoriteSlots.resolveIndex(inv, startSlot + i));

        favoritePositions[i] = isFavorite ? itemStack : ItemStack.EMPTY;
        if (isFavorite) favoriteStacksThisSort.add(itemStack);
    }

    @ModifyVariable(
            method = "sortInv(Lnet/minecraft/world/Container;IILnet/kyrptonaught/inventorysorter/SortCases$SortType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I",
                    ordinal = 0))
    private static List<ItemStack> favoriteitems$restoreFavoritePositions(List<ItemStack> stacks) {
        var nextFreeIndex = 0;
        for (ItemStack stack : stacks) {
            while (!favoritePositions[nextFreeIndex].isEmpty()) nextFreeIndex++;

            favoritePositions[nextFreeIndex] = stack;
        }
        return new ArrayList<>(List.of(favoritePositions));
    }

    @Inject(method = "addStackWithMerge", at = @At("HEAD"), cancellable = true)
    private static void favoriteitems$skipFavoriteStack(List<ItemStack> stacks, ItemStack newStack, CallbackInfo ci) {
        if (favoriteStacksThisSort.contains(newStack))
            ci.cancel();
    }
}
