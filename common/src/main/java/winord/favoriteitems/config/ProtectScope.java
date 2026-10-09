package winord.favoriteitems.config;

import net.minecraft.world.item.ItemStack;

public enum ProtectScope {
    STACKABLE,
    ALL;

    public boolean covers(ItemStack stack) {
        return !stack.isEmpty() && (this == ALL || stack.getMaxStackSize() > 1);
    }
}
