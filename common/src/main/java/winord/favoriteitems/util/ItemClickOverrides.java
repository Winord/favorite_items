package winord.favoriteitems.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class ItemClickOverrides {
    public enum Kind {
        NONE,
        CURSOR_ABSORBS_SLOT_ITEM,
        CURSOR_RELEASES_TO_SLOT,
        SLOT_ABSORBS_CURSOR_ITEM,
        SLOT_RELEASES_TO_CURSOR
    }

    private ItemClickOverrides() {
    }

    public static boolean isBundle(ItemStack stack) {
        return !stack.isEmpty() && stack.get(DataComponents.BUNDLE_CONTENTS) != null;
    }

    public static Kind classifyPickup(ItemStack cursor, ItemStack slotStack, int button) {
        var primary = button == 0;

        if (isBundle(cursor)) {
            if (primary && !slotStack.isEmpty()) return Kind.CURSOR_ABSORBS_SLOT_ITEM;
            if (!primary && slotStack.isEmpty()) return Kind.CURSOR_RELEASES_TO_SLOT;
        }
        if (isBundle(slotStack)) {
            if (primary && !cursor.isEmpty()) return Kind.SLOT_ABSORBS_CURSOR_ITEM;
            if (!primary && cursor.isEmpty()) return Kind.SLOT_RELEASES_TO_CURSOR;
        }
        return Kind.NONE;
    }
}
