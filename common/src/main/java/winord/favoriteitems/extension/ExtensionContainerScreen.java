package winord.favoriteitems.extension;

import net.minecraft.world.inventory.Slot;

public interface ExtensionContainerScreen {
    void favoriteitems$trackCursorFavoriteReturn();

    boolean favoriteitems$wouldToggleSlot(int inventoryIndex);

    boolean favoriteitems$blocksWheelMove();

    Slot favoriteitems$hoveredSlot();
}
