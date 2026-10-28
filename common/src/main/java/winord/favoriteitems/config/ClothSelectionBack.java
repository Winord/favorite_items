package winord.favoriteitems.config;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import winord.favoriteitems.FavoriteItems;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClothSelectionBack {
    private static final int RIGHT_BUTTON = InputConstants.MOUSE_BUTTON_RIGHT;

    private static Field indexField;
    private static Field valuesField;
    private static Field buttonField;
    private static boolean failed;

    private ClothSelectionBack() { }

    public static boolean stepBackOnRightClick(Object entry, MouseButtonEvent event) {
        if (failed || event.button() != RIGHT_BUTTON || !(entry instanceof SelectionListEntry<?> selection)) return false;
        if (!selection.isEnabled()) return false;

        try {
            resolveFields();
            var button = (Button) buttonField.get(selection);
            if (!button.isMouseOver(event.x(), event.y())) return false;

            var size = ((List<?>) valuesField.get(selection)).size();
            if (size == 0) return false;

            var index = (AtomicInteger) indexField.get(selection);
            index.set(Math.floorMod(index.get() - 1, size));
            button.playDownSound(Minecraft.getInstance().getSoundManager());
            selection.setFocused(button);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            FavoriteItems.LOGGER.warn("Cloth Config selector could not be stepped back with the right mouse button.", e);
            return false;
        }
    }

    private static void resolveFields() throws ReflectiveOperationException {
        if (indexField != null) return;

        var index = SelectionListEntry.class.getDeclaredField("index");
        var values = SelectionListEntry.class.getDeclaredField("values");
        var button = SelectionListEntry.class.getDeclaredField("buttonWidget");
        index.setAccessible(true);
        values.setAccessible(true);
        button.setAccessible(true);
        valuesField = values;
        buttonField = button;
        indexField = index;
    }
}
