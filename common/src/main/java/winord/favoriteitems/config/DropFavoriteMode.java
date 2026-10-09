package winord.favoriteitems.config;

import net.minecraft.world.item.ItemStack;

public enum DropFavoriteMode {
    BLOCK,
    BLOCK_LAST_STACK_ITEM,
    ALLOW_STACKABLE,
    ALLOW_EVERYTHING;

    public boolean keepsLastItemOnDropAll(ItemStack stack) {
        return this == BLOCK_LAST_STACK_ITEM && stack.getMaxStackSize() > 1 && stack.getCount() > 1;
    }

    public boolean blocks(ItemStack stack, int dropCount) {
        if (stack.isEmpty()) return true;
        if (this == ALLOW_EVERYTHING) return false;
        if (stack.getMaxStackSize() <= 1) return true;

        return switch (this) {
            case BLOCK -> true;
            case BLOCK_LAST_STACK_ITEM -> dropCount >= stack.getCount();
            case ALLOW_STACKABLE, ALLOW_EVERYTHING -> false;
        };
    }
}
