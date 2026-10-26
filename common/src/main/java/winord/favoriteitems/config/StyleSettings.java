package winord.favoriteitems.config;

public final class StyleSettings {
    public boolean iconEnabled = true;

    public boolean dyeableEnabled = true;
    public int dyeableColor = 0xFFFFFF;
    public boolean dyeableAboveItem = true;
    public int dyeableOpacityPercent = 100;

    public boolean staticEnabled = true;
    public boolean staticAboveItem = true;
    public int staticOpacityPercent = 100;
    public boolean staticAboveDyeable = true;

    public boolean backgroundEnabled = false;
    public int backgroundColor = 0xFFD700;
    public int backgroundOpacityPercent = 40;

    public boolean dyeableShadowEnabled = true;
    public boolean staticShadowEnabled = true;

    public StyleSettings copy() {
        var s = new StyleSettings();
        s.iconEnabled = iconEnabled;
        s.dyeableEnabled = dyeableEnabled;
        s.dyeableColor = dyeableColor;
        s.dyeableAboveItem = dyeableAboveItem;
        s.dyeableOpacityPercent = dyeableOpacityPercent;
        s.staticEnabled = staticEnabled;
        s.staticAboveItem = staticAboveItem;
        s.staticOpacityPercent = staticOpacityPercent;
        s.staticAboveDyeable = staticAboveDyeable;
        s.backgroundEnabled = backgroundEnabled;
        s.backgroundColor = backgroundColor;
        s.backgroundOpacityPercent = backgroundOpacityPercent;
        s.dyeableShadowEnabled = dyeableShadowEnabled;
        s.staticShadowEnabled = staticShadowEnabled;
        return s;
    }

    public float dyeableAlpha() {
        return Math.max(0, Math.min(100, dyeableOpacityPercent)) / 100f;
    }

    public float staticAlpha() {
        return Math.max(0, Math.min(100, staticOpacityPercent)) / 100f;
    }

    public int backgroundArgb() {
        int alpha = Math.round(backgroundOpacityPercent * 255f / 100f);
        return (alpha << 24) | (backgroundColor & 0xFFFFFF);
    }

    public boolean drawsLayers(boolean above) {
        return iconEnabled
                && ((dyeableEnabled && dyeableAboveItem == above) || (staticEnabled && staticAboveItem == above));
    }

    public static boolean showsAnything(boolean icon, boolean dyeable, boolean fixed, boolean background) {
        return background || (icon && (dyeable || fixed));
    }

    public void normalize() {
        if (showsAnything(iconEnabled, dyeableEnabled, staticEnabled, backgroundEnabled)) return;

        iconEnabled = true;
        if (!dyeableEnabled && !staticEnabled) dyeableEnabled = true;
    }

    public static StyleSettings defaultFor(FavoriteStyle style) {
        return switch (style) {
            case BOOKMARK -> preset(true, 0xe60000, true, true, true, true, 0xFF0000, true);
            case STAR -> preset(false, 0x9AD1FF, true, true, true, false, 0xFFD700, false);
            case TERRARIA -> preset(true, 0x9AD1FF, false, false, false, false, 0x9AD1FF, false);
            case LEAF -> preset(false, 0x1C9426, true, true, true, false, 0xFF7300, false);
            case GEM -> preset(false, 0x03FC0B, true, true, true, false, 0x7900D6, false);
            case HEART -> preset(false, 0x03FC0B, true, true, true, false, 0xED020A, false);
            case LOCK -> preset(false, 0xFCC603, true, true, true, false, 0xFCC603, false);
            case CUSTOM -> preset(true, 0xFF0000, false, false, false, true, 0xFF0000, true);
            case CIRCLE -> {
                var s = preset(true, 0xFF8C8C, false, false, false, false, 0xFFFFFF, false);
                s.dyeableOpacityPercent = 50;
                yield s;
            }
            case MINIMAL -> {
                var s = preset(true, 0xFFFFFF, false, false, false, true, 0x00FF00, false);
                s.iconEnabled = false;
                s.backgroundEnabled = true;
                s.backgroundOpacityPercent = 15;
                yield s;
            }
        };
    }

    private static StyleSettings preset(boolean dyeable, int dyeableColor, boolean dyeableAbove,
                                        boolean fixed, boolean fixedAbove, boolean fixedAboveDyeable,
                                        int backgroundColor, boolean shadow) {
        var s = new StyleSettings();
        s.iconEnabled = true;
        s.dyeableEnabled = dyeable;
        s.dyeableColor = dyeableColor;
        s.dyeableAboveItem = dyeableAbove;
        s.dyeableOpacityPercent = 100;
        s.staticEnabled = fixed;
        s.staticAboveItem = fixedAbove;
        s.staticOpacityPercent = 100;
        s.staticAboveDyeable = fixedAboveDyeable;
        s.backgroundColor = backgroundColor;
        s.backgroundOpacityPercent = 20;
        s.dyeableShadowEnabled = shadow;
        s.staticShadowEnabled = shadow;
        return s;
    }
}
