package winord.favoriteitems.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import winord.favoriteitems.FavoriteItemsClient;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.compat.ipn.IpnCompat;
import winord.favoriteitems.extension.ExtensionContainerScreen;
import winord.favoriteitems.sound.FavoriteItemsSounds;
import winord.favoriteitems.util.ActionBarMessages;
import winord.favoriteitems.util.FavoriteSlots;
import winord.favoriteitems.util.RenderUtil;
import winord.favoriteitems.util.SlotClicks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.minecraft.world.inventory.ClickType.PICKUP;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen<T extends AbstractContainerMenu> extends Screen
        implements ExtensionContainerScreen
{
    @Unique private static final int NONE = -1;
    @Unique private static final float ITEM_Z = 232f;
    @Unique private static final int QUICK_CRAFT_START = 0;
    @Unique private static final int QUICK_CRAFT_ADD = 1;

    @Shadow @Final protected T menu;
    @Shadow @Final protected Set<Slot> quickCraftSlots;
    @Shadow protected Slot hoveredSlot;

    @Unique private boolean favoriteitems$cursorFavorite = false;

    @Unique private int favoriteitems$quickMoveOrigin = NONE;
    @Unique private ItemStack favoriteitems$quickMoveStack = ItemStack.EMPTY;
    @Unique private Map<Integer, Integer> favoriteitems$quickMoveCounts = null;

    @Unique private Slot favoriteitems$dragOnlySlot = null;
    @Unique private boolean favoriteitems$dragTouchedMultiple = false;
    @Unique private boolean favoriteitems$dragBlocked = false;

    @Unique private int favoriteitems$keyDragMode = 0;
    @Unique private double favoriteitems$keyDragX;
    @Unique private double favoriteitems$keyDragY;

    @Shadow
    private Slot getHoveredSlot(double mouseX, double mouseY) {
        throw new AssertionError();
    }

    @Unique
    private boolean favoriteitems$isEnderChest() {
        return FavoriteSlots.isEnderChestTitle(getTitle());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void favoriteitems$onEnderChestOpened(CallbackInfo ci) {
        if (favoriteitems$isEnderChest()) FavoriteSlots.onEnderChestOpened();
    }

    @Unique
    private int favoriteitems$indexOf(Slot slot) {
        var playerIndex = FavoriteSlots.playerInventoryIndex(slot);
        if (playerIndex >= 0) return playerIndex;
        return favoriteitems$isEnderChest() ? FavoriteSlots.enderChestIndex(slot.getContainerSlot()) : NONE;
    }

    @Unique
    private Slot favoriteitems$slotOf(int index) {
        if (index < 0) return null;

        boolean enderChest = FavoriteSlots.isEnderChestIndex(index);
        int containerSlot = enderChest ? index - FavoriteSlots.ENDER_CHEST_OFFSET : index;
        for (var slot : menu.slots) {
            if ((slot.container instanceof Inventory) != enderChest && slot.getContainerSlot() == containerSlot) return slot;
        }
        return null;
    }

    @Unique
    private ItemStack favoriteitems$stackOf(int index) {
        var slot = favoriteitems$slotOf(index);
        return slot != null ? slot.getItem() : ItemStack.EMPTY;
    }

    @Unique
    private Slot favoriteitems$findToppableFavorite(ItemStack moving) {
        if (moving.getMaxStackSize() <= 1) return null;

        for (var index : FavoriteSlots.currentFavorites()) {
            if (FavoriteSlots.isEnderChestIndex(index)) continue;

            var candidate = favoriteitems$slotOf(index);
            if (candidate == null) continue;

            var stack = candidate.getItem();
            if (!stack.isEmpty() && stack.getCount() < stack.getMaxStackSize() && ItemStack.isSameItemSameComponents(stack, moving)) {
                return candidate;
            }
        }
        return null;
    }

    @Unique
    private void favoriteitems$topOffFavorites(Slot source) {
        for (int attempts = 0; attempts < FavoriteSlots.INVENTORY_SIZE && source.hasItem(); attempts++) {
            var target = favoriteitems$findToppableFavorite(source.getItem());
            if (target == null) return;

            SlotClicks.mergeInto(menu, source.index, target.index);
        }
    }

    @Unique
    private void favoriteitems$trackQuickMove(int index, ItemStack stack) {
        favoriteitems$quickMoveOrigin = index;
        favoriteitems$quickMoveStack = stack.copy();
        favoriteitems$quickMoveCounts = favoriteitems$snapshotCounts();
    }

    @Unique
    private boolean favoriteitems$wouldEquipArmor(Slot slot) {
        if (!(menu instanceof InventoryMenu) || slot.index < 9 || slot.index > 44) return false;

        var equipmentSlot = minecraft.player.getEquipmentSlotForItem(slot.getItem());
        return equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !menu.getSlot(8 - equipmentSlot.getIndex()).hasItem();
    }

    @Unique
    private boolean favoriteitems$wouldGatherFavorite(ItemStack carried) {
        if (minecraft.hasSingleplayerServer() || carried.isEmpty() || carried.getCount() >= carried.getMaxStackSize()) return false;

        for (var candidate : menu.slots) {
            if (candidate.hasItem() && ItemStack.isSameItemSameComponents(candidate.getItem(), carried)
                    && FavoriteSlots.isFavorite(menu, candidate.index)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private Map<Integer, Integer> favoriteitems$snapshotCounts() {
        var counts = new HashMap<Integer, Integer>();

        var inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) counts.put(i, inventory.getItem(i).getCount());

        if (favoriteitems$isEnderChest()) {
            for (var slot : menu.slots) {
                if (!(slot.container instanceof Inventory)) {
                    counts.put(FavoriteSlots.enderChestIndex(slot.getContainerSlot()), slot.getItem().getCount());
                }
            }
        }
        return counts;
    }

    @Unique
    private boolean favoriteitems$hasForeignContainer() {
        if (favoriteitems$isEnderChest()) return false;

        for (var slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) return true;
        }
        return false;
    }

    @Unique
    private boolean favoriteitems$toggleFavorite(int index) {
        var nowFavorite = FavoriteSlots.toggle(index);

        var sound = FavoriteItemsSounds.forStyle(FavoriteItemsConfig.soundStyle);
        if (sound != null) {
            float pitch = (nowFavorite ? 1.0f : 0.8f) + minecraft.player.level().random.nextFloat() * 0.1f;
            minecraft.player.playSound(sound, 0.4f, pitch);
        }
        return nowFavorite;
    }

    @Unique
    private boolean favoriteitems$isCursorFavorite() {
        return favoriteitems$cursorFavorite && !menu.getCarried().isEmpty();
    }

    @Override
    public void favoriteitems$trackCursorFavoriteReturn() {
        if (!favoriteitems$isCursorFavorite()) return;
        favoriteitems$cursorFavorite = false;

        if (minecraft == null || minecraft.player == null) return;
        FavoriteSlots.onCarriedFavoriteReturned(minecraft.player.getInventory(), menu.getCarried());
    }

    @Override
    public boolean favoriteitems$wouldToggleSlot(int inventoryIndex) {
        if (inventoryIndex < 0 || minecraft == null || minecraft.player == null) return false;
        if (FavoriteSlots.isSuspended() || !favoriteitems$isFavoriteKeyDown() || !menu.getCarried().isEmpty()) return false;

        return !minecraft.player.getInventory().getItem(inventoryIndex).isEmpty();
    }

    @Override
    public boolean favoriteitems$blocksWheelMove() {
        if (FavoriteSlots.isSuspended()) return false;
        if (favoriteitems$isCursorFavorite()) return true;
        if (!menu.getCarried().isEmpty() || hoveredSlot == null) return false;

        return FavoriteSlots.isFavorite(favoriteitems$indexOf(hoveredSlot));
    }

    @Override
    public Slot favoriteitems$hoveredSlot() {
        return hoveredSlot;
    }

    @Inject(method = "slotClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleInventoryMouseClick(IIILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))
    private void favoriteitems$markScreenClickStart(Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci) {
        FavoriteSlots.enterScreenClick();
    }

    @Inject(method = "slotClicked", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleInventoryMouseClick(IIILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))
    private void favoriteitems$markScreenClickEnd(Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci) {
        FavoriteSlots.exitScreenClick();
    }

    @Unique
    private boolean favoriteitems$isFavoriteKeyDown() {
        var key = ((KeyMappingAccessor) (Object) FavoriteItemsClient.FAVORITE_KEY).favoriteitems$getKey();
        return key.getType() == InputConstants.Type.KEYSYM
                && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"), cancellable = true)
    private void favoriteitems$toggleOnKeyClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (FavoriteSlots.isSuspended() || button != 0) return;

        var slot = getHoveredSlot(mouseX, mouseY);
        if (slot == null || !slot.hasItem() || !favoriteitems$isFavoriteKeyDown() || !menu.getCarried().isEmpty()) return;

        var index = favoriteitems$indexOf(slot);
        if (index < 0) return;

        favoriteitems$keyDragMode = favoriteitems$toggleFavorite(index) ? 1 : -1;
        favoriteitems$keyDragX = mouseX;
        favoriteitems$keyDragY = mouseY;
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void favoriteitems$dragToggle(
            double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir)
    {
        if (favoriteitems$keyDragMode == 0) return;

        if (button != 0 || FavoriteSlots.isSuspended() || !menu.getCarried().isEmpty() || !favoriteitems$isFavoriteKeyDown()) {
            favoriteitems$keyDragMode = 0;
            return;
        }

        var dx = mouseX - favoriteitems$keyDragX;
        var dy = mouseY - favoriteitems$keyDragY;
        var steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 4.0));
        for (int i = 1; i <= steps; i++) {
            favoriteitems$applyKeyDrag(getHoveredSlot(favoriteitems$keyDragX + dx * i / steps, favoriteitems$keyDragY + dy * i / steps));
        }

        favoriteitems$keyDragX = mouseX;
        favoriteitems$keyDragY = mouseY;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void favoriteitems$endKeyDrag(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0) favoriteitems$keyDragMode = 0;
    }

    @Unique
    private void favoriteitems$applyKeyDrag(Slot slot) {
        if (slot == null) return;

        var index = favoriteitems$indexOf(slot);
        if (index < 0) return;

        var favorite = FavoriteSlots.isFavorite(index);
        boolean needsChange = favoriteitems$keyDragMode > 0 ? !favorite && slot.hasItem() : favorite;
        if (needsChange) favoriteitems$toggleFavorite(index);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void favoriteitems$trackCursorFavoriteOnClose(CallbackInfo ci) {
        favoriteitems$trackCursorFavoriteReturn();
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void favoriteitems$renderIconUnderItem(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (!FavoriteSlots.isFavorite(favoriteitems$indexOf(slot))) return;

        var settings = FavoriteItemsConfig.activeSettings();
        if (settings.backgroundEnabled && slot.hasItem()) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, settings.backgroundArgb());
        }
        RenderUtil.renderActiveIcon(graphics, false, slot.x, slot.y, 1f);
    }

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void favoriteitems$renderIconAboveItem(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (FavoriteSlots.isFavorite(favoriteitems$indexOf(slot))) {
            RenderUtil.renderActiveIcon(graphics, true, slot.x, slot.y, 1f);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void favoriteitems$renderCursorIcon(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!favoriteitems$isCursorFavorite() || FavoriteSlots.isSuspended()) return;

        var settings = FavoriteItemsConfig.activeSettings();
        if (!settings.iconEnabled) return;

        int x = mouseX - 8, y = mouseY - 8;
        RenderUtil.renderActiveIcon(graphics, false, x, y, 1f);

        var cursorStack = menu.getCarried();
        if (settings.drawsLayers(false) && !cursorStack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, ITEM_Z);
            graphics.renderItem(cursorStack, x, y);
            graphics.renderItemDecorations(this.font, cursorStack, x, y);
            graphics.pose().popPose();
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0f, 0f, ITEM_Z);
        RenderUtil.renderActiveIcon(graphics, true, x, y, 1f);
        graphics.pose().popPose();
    }

    @Inject(method = "slotClicked", at = @At("TAIL"))
    private void favoriteitems$followQuickMove(Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci) {
        if (favoriteitems$quickMoveOrigin == NONE) return;

        var origin = favoriteitems$quickMoveOrigin;
        var movedStack = favoriteitems$quickMoveStack;
        var countsBefore = favoriteitems$quickMoveCounts;
        favoriteitems$quickMoveOrigin = NONE;
        favoriteitems$quickMoveStack = ItemStack.EMPTY;
        favoriteitems$quickMoveCounts = null;

        var originAfter = favoriteitems$stackOf(origin).getCount();
        if (originAfter == countsBefore.getOrDefault(origin, 0)) return;

        var bestIndex = origin;
        var bestCount = originAfter;
        for (var entry : favoriteitems$snapshotCounts().entrySet()) {
            var index = entry.getKey();
            if (index == origin) continue;

            var after = entry.getValue();
            if (after > countsBefore.getOrDefault(index, 0) && after > bestCount
                    && ItemStack.isSameItemSameComponents(favoriteitems$stackOf(index), movedStack)) {
                bestCount = after;
                bestIndex = index;
            }
        }

        if (bestIndex != origin) {
            FavoriteSlots.unfavorite(origin);
            FavoriteSlots.favorite(bestIndex);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    protected void applyFavoriteRules(
            Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci)
    {
        if (FavoriteSlots.isSuspended()) {
            favoriteitems$cursorFavorite = false;
            favoriteitems$quickMoveOrigin = NONE;
            return;
        }

        if (IpnCompat.cancelsScreenClick(slot, slotId, button, clickType)) return;

        var cursorStack = menu.getCarried();
        if (cursorStack.isEmpty()) favoriteitems$cursorFavorite = false;
        var cursorIsFavorite = favoriteitems$cursorFavorite;

        if (clickType == ClickType.QUICK_CRAFT) {
            favoriteitems$handleQuickCraft(slot, button, cursorIsFavorite, ci);
            return;
        }

        if (slot == null) {
            if (clickType == PICKUP && slotId == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE && cursorIsFavorite) {
                var dropCount = button == 0 ? cursorStack.getCount() : 1;
                if (FavoriteItemsConfig.dropFavorites.blocks(cursorStack, dropCount)) {
                    ActionBarMessages.dropBlocked();
                    ci.cancel();
                } else if (dropCount >= cursorStack.getCount()) {
                    favoriteitems$cursorFavorite = false;
                }
            }
            return;
        }

        var index = favoriteitems$indexOf(slot);
        var slotIsFavoritable = index >= 0;
        var slotIsFavorite = FavoriteSlots.isFavorite(index);

        var slotStack = slot.getItem();
        var depositCount = button != 0 ? 1 : cursorStack.getCount();
        var sameItem = ItemStack.isSameItemSameComponents(cursorStack, slotStack);
        var canFullyMerge = sameItem
                && slotStack.getMaxStackSize() >= slotStack.getCount() + depositCount
                && cursorStack.getCount() == depositCount;

        switch (clickType) {
            case PICKUP -> {
                if (!slotIsFavoritable) {
                    if (cursorIsFavorite) ci.cancel();
                } else if (!cursorStack.isEmpty() && !slot.mayPlace(cursorStack)) {
                    return;
                } else if (!slotStack.isEmpty() && !slot.mayPickup(minecraft.player)) {
                    return;
                } else if (slotStack.isEmpty() && !cursorStack.isEmpty()) {
                    if (cursorIsFavorite) {
                        if (depositCount == cursorStack.getCount()) {
                            FavoriteSlots.favorite(index);
                            favoriteitems$cursorFavorite = false;
                        } else {
                            ci.cancel();
                        }
                    }
                } else if (cursorStack.isEmpty() && slotIsFavorite) {
                    if (button == 0 || slotStack.getCount() <= 1) {
                        FavoriteSlots.unfavorite(index);
                        favoriteitems$cursorFavorite = true;
                    }
                } else if (!cursorStack.isEmpty() && sameItem) {
                    if (cursorIsFavorite) {
                        if (canFullyMerge) {
                            FavoriteSlots.favorite(index);
                            favoriteitems$cursorFavorite = false;
                        } else {
                            ci.cancel();
                        }
                    }
                } else if (!cursorStack.isEmpty() && !slotStack.isEmpty()) {
                    if (cursorIsFavorite) FavoriteSlots.favorite(index); else FavoriteSlots.unfavorite(index);
                    favoriteitems$cursorFavorite = slotIsFavorite;
                }
            }
            case QUICK_MOVE -> {
                if (slotIsFavorite) {
                    if (FavoriteSlots.isEnderChestIndex(index)) {
                        favoriteitems$topOffFavorites(slot);
                        if (slot.hasItem()) {
                            favoriteitems$trackQuickMove(index, slot.getItem());
                        } else {
                            FavoriteSlots.unfavorite(index);
                            ci.cancel();
                        }
                    } else if (favoriteitems$wouldEquipArmor(slot) || !favoriteitems$hasForeignContainer()) {
                        favoriteitems$trackQuickMove(index, slotStack);
                    } else {
                        ci.cancel();
                    }
                } else if (FavoriteSlots.playerInventoryIndex(slot) < 0 && slotStack.getMaxStackSize() > 1
                        && favoriteitems$findToppableFavorite(slotStack) != null) {
                    ci.cancel();
                    favoriteitems$topOffFavorites(slot);
                }
            }
            case SWAP -> {
                var hotbarIndex = button;
                var hotbarIsFavorite = FavoriteSlots.isFavorite(hotbarIndex);

                if (hotbarIsFavorite && !slotIsFavoritable) {
                    ci.cancel();
                } else if (slotIsFavoritable && hotbarIsFavorite != slotIsFavorite) {
                    if (!slotStack.isEmpty() && !slot.mayPickup(minecraft.player)) return;

                    var hotbarStack = minecraft.player.getInventory().getItem(hotbarIndex);
                    if (!hotbarStack.isEmpty() && !slot.mayPlace(hotbarStack)) return;

                    FavoriteSlots.unfavorite(hotbarIsFavorite ? hotbarIndex : index);
                    FavoriteSlots.favorite(hotbarIsFavorite ? index : hotbarIndex);
                }
            }
            case THROW -> {
                if (cursorIsFavorite) {
                    ActionBarMessages.dropBlocked();
                    ci.cancel();
                } else if (slotIsFavorite) {
                    var mode = FavoriteItemsConfig.dropFavorites;
                    var dropCount = button == 0 ? 1 : slotStack.getCount();
                    if (mode.blocks(slotStack, dropCount)) {
                        if (button == 0 || !mode.keepsLastItemOnDropAll(slotStack) || !SlotClicks.throwAllButOne(menu, slot.index)) {
                            ActionBarMessages.dropBlocked();
                        }
                        ci.cancel();
                    }
                }
            }
            case PICKUP_ALL -> {
                if (cursorIsFavorite || slotIsFavorite || favoriteitems$wouldGatherFavorite(cursorStack)) ci.cancel();
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"), cancellable = true)
    private void favoriteitems$blockDragSpread(
            double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir)
    {
        if (favoriteitems$cursorFavorite && menu.getCarried().getCount() > 1 && !FavoriteSlots.isSuspended()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void favoriteitems$handleQuickCraft(Slot slot, int button, boolean cursorIsFavorite, CallbackInfo ci) {
        var stage = AbstractContainerMenu.getQuickcraftHeader(button);

        if (stage == QUICK_CRAFT_START) {
            favoriteitems$dragOnlySlot = null;
            favoriteitems$dragTouchedMultiple = false;
            favoriteitems$dragBlocked = cursorIsFavorite && menu.getCarried().getCount() > 1 && quickCraftSlots.size() > 1;
            return;
        }

        if (stage == QUICK_CRAFT_ADD) {
            if (slot == null) return;

            if (favoriteitems$dragBlocked || (cursorIsFavorite && !favoriteitems$canReceiveFavorite(slot, button))) {
                ci.cancel();
                return;
            }

            if (favoriteitems$dragOnlySlot == null) favoriteitems$dragOnlySlot = slot;
            else if (favoriteitems$dragOnlySlot != slot) favoriteitems$dragTouchedMultiple = true;
            return;
        }

        if (cursorIsFavorite) {
            if (!favoriteitems$dragTouchedMultiple && favoriteitems$dragOnlySlot != null) {
                FavoriteSlots.favorite(favoriteitems$indexOf(favoriteitems$dragOnlySlot));
                favoriteitems$cursorFavorite = false;
            } else if (favoriteitems$dragTouchedMultiple) {
                favoriteitems$cursorFavorite = false;
            }
        }

        favoriteitems$dragOnlySlot = null;
        favoriteitems$dragTouchedMultiple = false;
        favoriteitems$dragBlocked = false;
    }

    @Unique
    private boolean favoriteitems$canReceiveFavorite(Slot slot, int button) {
        if (favoriteitems$indexOf(slot) < 0) return false;

        var cursorStack = menu.getCarried();
        var targetStack = slot.getItem();
        var depositCount = AbstractContainerMenu.getQuickcraftType(button) == 1 ? 1 : cursorStack.getCount();

        if (targetStack.isEmpty()) return depositCount == cursorStack.getCount();
        if (!ItemStack.isSameItemSameComponents(cursorStack, targetStack)) return true;

        return targetStack.getMaxStackSize() >= targetStack.getCount() + depositCount
                && cursorStack.getCount() == depositCount;
    }

    protected MixinAbstractContainerScreen(Component title) {
        super(title);
    }
}
