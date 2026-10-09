package winord.favoriteitems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.config.HotbarIndicatorMode;
import winord.favoriteitems.extension.ExtensionGui;
import winord.favoriteitems.util.FavoriteSlots;
import winord.favoriteitems.util.RenderUtil;

@Mixin(Gui.class)
public abstract class MixinGui implements ExtensionGui {
    @Unique private static final int FLASH_TICKS = 10;
    @Unique private static final float FLASH_FADE_DIVISOR = 5f;

    @Unique private int favoriteitems$flashTicksLeft;
    @Unique private int favoriteitems$flashSlot;

    @Shadow protected abstract Player getCameraPlayer();

    @Override
    public void favoriteitems$flashIcon() {
        favoriteitems$flashIcon(getCameraPlayer().getInventory().selected);
    }

    @Override
    public void favoriteitems$flashIcon(int inventoryIndex) {
        favoriteitems$flashTicksLeft = FLASH_TICKS;
        favoriteitems$flashSlot = inventoryIndex;
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void favoriteitems$tickFlash(CallbackInfo ci) {
        if (favoriteitems$flashTicksLeft > 0) --favoriteitems$flashTicksLeft;
    }

    @Unique
    private static int favoriteitems$slotX(int inventoryIndex) {
        int center = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        if (inventoryIndex != FavoriteSlots.OFFHAND_INDEX) return center - 88 + inventoryIndex * 20;

        return Minecraft.getInstance().options.mainHand().get().getOpposite() == HumanoidArm.LEFT
                ? center - 117
                : center + 101;
    }

    @Unique
    private static int favoriteitems$slotY() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight() - 19;
    }

    @Unique
    private float favoriteitems$flashAlpha() {
        return favoriteitems$flashTicksLeft / FLASH_FADE_DIVISOR;
    }

    @Unique
    private float favoriteitems$alphaAt(int x, int y) {
        if (y != favoriteitems$slotY()) return 0f;

        return switch (FavoriteItemsConfig.hotbarIndicatorMode) {
            case DISABLED -> 0f;
            case ALWAYS -> favoriteitems$isFavoriteSlotX(x) ? 1f : 0f;
            case ON_INTERACTION -> favoriteitems$flashTicksLeft != 0 && x == favoriteitems$slotX(favoriteitems$flashSlot)
                    ? favoriteitems$flashAlpha()
                    : 0f;
        };
    }

    @Unique
    private static boolean favoriteitems$isFavoriteSlotX(int x) {
        for (int i = 0; i < FavoriteSlots.HOTBAR_SIZE; i++) {
            if (x == favoriteitems$slotX(i) && FavoriteSlots.isFavorite(i)) return true;
        }
        return x == favoriteitems$slotX(FavoriteSlots.OFFHAND_INDEX) && FavoriteSlots.isFavorite(FavoriteSlots.OFFHAND_INDEX);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void favoriteitems$renderIconUnderItem(
            GuiGraphics graphics, int x, int y, float partialTick, Player player, ItemStack stack, int seed, CallbackInfo ci)
    {
        if (!FavoriteItemsConfig.activeSettings().drawsLayers(false)) return;

        var alpha = favoriteitems$alphaAt(x, y);
        if (alpha > 0f) RenderUtil.renderActiveIcon(graphics, false, x, y, alpha);
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void favoriteitems$renderIconAboveItem(float partialTick, GuiGraphics graphics, CallbackInfo ci) {
        if (!FavoriteItemsConfig.activeSettings().drawsLayers(true)) return;

        var mode = FavoriteItemsConfig.hotbarIndicatorMode;
        if (mode == HotbarIndicatorMode.DISABLED) return;

        int y = favoriteitems$slotY();

        boolean offhandShown = !getCameraPlayer().getOffhandItem().isEmpty();

        if (mode == HotbarIndicatorMode.ALWAYS) {
            for (int i = 0; i < FavoriteSlots.HOTBAR_SIZE; i++) {
                if (FavoriteSlots.isFavorite(i)) RenderUtil.renderActiveIcon(graphics, true, favoriteitems$slotX(i), y, 1f);
            }
            if (offhandShown && FavoriteSlots.isFavorite(FavoriteSlots.OFFHAND_INDEX)) {
                RenderUtil.renderActiveIcon(graphics, true, favoriteitems$slotX(FavoriteSlots.OFFHAND_INDEX), y, 1f);
            }
        } else if (favoriteitems$flashTicksLeft != 0 && (favoriteitems$flashSlot != FavoriteSlots.OFFHAND_INDEX || offhandShown)) {
            RenderUtil.renderActiveIcon(graphics, true, favoriteitems$slotX(favoriteitems$flashSlot), y, favoriteitems$flashAlpha());
        }
    }
}
