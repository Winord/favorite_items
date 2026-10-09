package winord.favoriteitems.compat.litematica;

import net.minecraft.world.item.ItemStack;

public final class LitematicaCompat {
    private static int pickDepth = 0;
    private static ItemStack pickedStack = ItemStack.EMPTY;

    private LitematicaCompat() { }

    public static void enterPick() {
        if (pickDepth++ == 0) pickedStack = ItemStack.EMPTY;
    }

    public static void notePickedStack(ItemStack stack) {
        pickedStack = stack.copy();
    }

    public static ItemStack pickedStack() {
        return pickedStack;
    }

    public static void exitPick() {
        if (pickDepth > 0) pickDepth--;
    }

    public static boolean isPicking() {
        return pickDepth > 0;
    }

    public static void resetState() {
        pickDepth = 0;
        pickedStack = ItemStack.EMPTY;
    }
}
