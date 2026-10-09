package winord.favoriteitems.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SlotClicks {
    private SlotClicks() { }

    public static boolean throwAllButOne(AbstractContainerMenu menu, int menuSlot) {
        if (menuSlot < 0 || !menu.getCarried().isEmpty() || !canClick()) return false;

        click(menu, menuSlot, 0);
        click(menu, menuSlot, 1);
        click(menu, AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, 0);
        return true;
    }

    public static void mergeInto(AbstractContainerMenu menu, int from, int to) {
        if (!canClick()) return;

        click(menu, from, 0);
        click(menu, to, 0);
        if (!menu.getCarried().isEmpty()) click(menu, from, 0);
    }

    public static void moveAllButOne(AbstractContainerMenu menu, int from, List<Slot> to) {
        moveAllButOne(menu, from, to, true);
    }

    public static void moveAllButOne(AbstractContainerMenu menu, int from, List<Slot> to, boolean intoEmptySlots) {
        if (!menu.getCarried().isEmpty() || !canClick()) return;

        click(menu, from, 0);
        click(menu, from, 1);
        for (var slot : to) {
            if (menu.getCarried().isEmpty()) break;
            ItemStack held = slot.getItem();
            if (!held.isEmpty() && held.getCount() < slot.getMaxStackSize(held)
                    && ItemStack.isSameItemSameTags(held, menu.getCarried()) && slot.mayPlace(menu.getCarried())) {
                click(menu, slot.index, 0);
            }
        }
        if (intoEmptySlots) {
            for (var slot : to) {
                if (menu.getCarried().isEmpty()) break;
                if (!slot.hasItem() && slot.mayPlace(menu.getCarried())) click(menu, slot.index, 0);
            }
        }
        if (!menu.getCarried().isEmpty()) click(menu, from, 0);
    }

    private static boolean canClick() {
        var minecraft = Minecraft.getInstance();
        return minecraft.gameMode != null && minecraft.player != null;
    }

    private static void click(AbstractContainerMenu menu, int slot, int button) {
        var minecraft = Minecraft.getInstance();
        minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, ClickType.PICKUP, minecraft.player);
    }
}
