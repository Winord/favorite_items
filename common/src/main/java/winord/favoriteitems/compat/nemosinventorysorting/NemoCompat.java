package winord.favoriteitems.compat.nemosinventorysorting;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.platform.Platform;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class NemoCompat {
    public static final String MOD_ID = "nemos_inventory_sorting";
    public static final String LOCKED_SLOTS_FILE = "locked-slots-config.json";

    private static final int ALT_MODIFIER = InputConstants.MOD_ALT;

    private static Boolean loaded = null;

    private NemoCompat() { }

    public static boolean isLoaded() {
        if (loaded == null) loaded = Platform.isModLoaded(MOD_ID);
        return loaded;
    }

    public static boolean isLockedSlotsFile(String path) {
        return path != null && path.endsWith(LOCKED_SLOTS_FILE);
    }

    public static MouseButtonEvent withoutAlt(MouseButtonEvent event) {
        if (!event.hasAltDown() || !isLoaded()) return event;

        var info = new MouseButtonInfo(event.button(), event.modifiers() & ~ALT_MODIFIER);
        return new MouseButtonEvent(event.x(), event.y(), info);
    }
}
