package winord.favoriteitems.extension;

import net.minecraft.client.gui.Gui;

public interface ExtensionHud {
    void favoriteitems$flashIcon();

    void favoriteitems$flashIcon(int inventoryIndex);

    static void flashIcon(Gui gui) {
        ((ExtensionHud) gui.hud).favoriteitems$flashIcon();
    }

    static void flashIcon(Gui gui, int inventoryIndex) {
        ((ExtensionHud) gui.hud).favoriteitems$flashIcon(inventoryIndex);
    }
}
