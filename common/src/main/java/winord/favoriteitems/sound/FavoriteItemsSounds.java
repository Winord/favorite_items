package winord.favoriteitems.sound;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import winord.favoriteitems.config.FavoriteSoundStyle;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class FavoriteItemsSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> MAGIC = register("magic");
    public static final RegistrySupplier<SoundEvent> CUSTOM = register("custom");

    private FavoriteItemsSounds() { }

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, name)));
    }

    public static void register() {
        SOUNDS.register();
    }

    public static SoundEvent forStyle(FavoriteSoundStyle style) {
        return switch (style) {
            case DEFAULT -> SoundEvents.UI_BUTTON_CLICK.value();
            case MAGIC -> MAGIC.get();
            case CUSTOM -> CUSTOM.get();
            case BONE -> SoundEvents.BONE_BLOCK_BREAK;
            case DISABLED -> null;
        };
    }
}
