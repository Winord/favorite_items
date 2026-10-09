package winord.favoriteitems.extension;

import net.minecraft.client.gui.Gui;

public interface ExtensionGui {
    void favoriteitems$flashIcon();

    void favoriteitems$flashIcon(int inventoryIndex);

    static void flashIcon(Gui gui) {
        ((ExtensionGui) gui).favoriteitems$flashIcon();
    }

    static void flashIcon(Gui gui, int inventoryIndex) {
        ((ExtensionGui) gui).favoriteitems$flashIcon(inventoryIndex);
    }
}
