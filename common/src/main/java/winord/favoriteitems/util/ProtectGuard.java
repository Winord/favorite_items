package winord.favoriteitems.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.config.ProtectAction;
import winord.favoriteitems.config.ProtectLevel;
import winord.favoriteitems.config.ProtectPresets;
import winord.favoriteitems.config.ProtectSubject;
import winord.favoriteitems.extension.ExtensionGui;

import java.util.function.Supplier;

public final class ProtectGuard {
    private static int mainHandBlockedTick = -1;

    private static ProtectAction lastBlockedAction = null;

    private ProtectGuard() { }

    public static boolean blocksItemUse(Player player, InteractionHand hand) {
        if (!isActive(player)) return false;
        if (followsBlockedMainHand(player, hand)) return true;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ProjectileWeaponItem) return blocksAmmo(player, held);

        return protects(player, hand, () -> ProtectClassifier.forItemUse(held));
    }

    public static boolean blocksBlockUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isActive(player)) return false;
        if (followsBlockedMainHand(player, hand)) return true;

        return protects(player, hand, () -> ProtectClassifier.forBlockUse(player, player.getItemInHand(hand), hit));
    }

    public static boolean blocksEntityUse(Player player, InteractionHand hand, Entity target) {
        if (!isActive(player)) return false;
        if (followsBlockedMainHand(player, hand)) return true;

        return protects(player, hand, () -> ProtectClassifier.forEntityUse(player.getItemInHand(hand), target));
    }

    public static boolean handleModSwap(Player player, int containerId, int slotId, int button) {
        if (FavoriteSlots.isScreenClickActive()) return false;

        var menu = ExternalClickGuard.menuOf(player, containerId);
        if (menu == null || slotId < 0 || slotId >= menu.slots.size()) return false;
        if (button != FavoriteSlots.OFFHAND_INDEX && (button < 0 || button >= FavoriteSlots.HOTBAR_SIZE)) return false;

        Slot slot = menu.getSlot(slotId);
        int from = FavoriteSlots.playerInventoryIndex(slot);
        if (from < 0) {
            int other = FavoriteSlots.resolveIndex(slot.container, slot.getContainerSlot());
            if (other >= 0) {
                FavoriteSlots.swapFavorites(other, button);
                return false;
            }
            return FavoriteSlots.isFavorite(button);
        }

        if (from < FavoriteSlots.ARMOR_START_INDEX && Minecraft.getInstance().screen == null
                && isTargeted(from) && isActive(player) && protectsPlacing(slot.getItem())) {
            ActionBarMessages.protectBlocked(ProtectAction.PLACE_BLOCK);
            return true;
        }

        FavoriteSlots.swapFavorites(from, button);
        return false;
    }

    public static boolean hidesFromBuilding(Player player, int inventoryIndex, ItemStack stack) {
        return isTargeted(inventoryIndex) && isActive(player) && protectsPlacing(stack);
    }

    public static boolean protectsLastItem(Player player, int inventoryIndex, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1 || stack.getMaxStackSize() <= 1) return false;
        if (!isTargeted(inventoryIndex) || !isActive(player)) return false;

        var preset = ProtectPresets.active();
        for (var action : ProtectAction.values()) {
            if (action.subject() != ProtectSubject.ITEM) continue;
            if (preset.level(action) == ProtectLevel.LAST_ITEM && preset.rule(action).matchesItem(stack)) return true;
        }
        return false;
    }

    public static int findBuildableStack(Player player, ItemStack like) {
        var items = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            var candidate = items.get(i);
            if (candidate.isEmpty() || !ItemStack.isSameItemSameComponents(like, candidate)) continue;
            if (!hidesFromBuilding(player, i, candidate)) return i;
        }
        return -1;
    }

    public static int usableSwapSource(Player player, int containerId, int slotId) {
        if (FavoriteSlots.isScreenClickActive() || Minecraft.getInstance().screen != null) return -1;

        var menu = ExternalClickGuard.menuOf(player, containerId);
        if (menu == null || slotId < 0 || slotId >= menu.slots.size()) return -1;

        Slot slot = menu.getSlot(slotId);
        int from = FavoriteSlots.playerInventoryIndex(slot);
        if (from < 0 || from >= FavoriteSlots.ARMOR_START_INDEX || !hidesFromBuilding(player, from, slot.getItem())) return -1;

        int alternative = findBuildableStack(player, slot.getItem());
        return alternative < 0 ? -1 : FavoriteSlots.menuSlotOf(menu, alternative);
    }

    public static boolean swapUsableStackIntoHand(Player player, ItemStack wanted) {
        if (wanted.isEmpty() || FavoriteSlots.isScreenClickActive() || Minecraft.getInstance().screen != null) return false;

        var inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        var held = inventory.getItem(selected);
        if (!ItemStack.isSameItemSameComponents(held, wanted) || !hidesFromBuilding(player, selected, held)) return false;

        int source = findBuildableStack(player, wanted);
        if (source < FavoriteSlots.HOTBAR_SIZE) return false;

        var menu = player.inventoryMenu;
        int menuSlot = FavoriteSlots.menuSlotOf(menu, source);
        var gameMode = Minecraft.getInstance().gameMode;
        if (menuSlot < 0 || gameMode == null) return false;

        gameMode.handleContainerInput(menu.containerId, menuSlot, selected, ContainerInput.SWAP, player);
        return true;
    }

    private static boolean protectsPlacing(ItemStack stack) {
        if (!inScope(stack)) return false;
        if (!ProtectPresets.active().rule(ProtectAction.PLACE_BLOCK).matchesItem(stack)) return false;

        return ProtectPresets.levelOf(ProtectAction.PLACE_BLOCK).covers(stack.getCount());
    }

    private static boolean isTargeted(int inventoryIndex) {
        return FavoriteItemsConfig.protectTarget.covers(inventoryIndex);
    }

    private static boolean inScope(ItemStack stack) {
        return FavoriteItemsConfig.protectScope.covers(stack);
    }

    private static boolean isActive(Player player) {
        return FavoriteItemsConfig.protectEnabled
                && player == Minecraft.getInstance().player
                && !player.getAbilities().instabuild;
    }

    private static boolean followsBlockedMainHand(Player player, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND && player.tickCount == mainHandBlockedTick;
    }

    private static boolean protects(Player player, InteractionHand hand, Supplier<ProtectAction[]> actions) {
        ItemStack stack = player.getItemInHand(hand);
        if (!inScope(stack)) return false;

        int index = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : FavoriteSlots.OFFHAND_INDEX;
        if (!isTargeted(index)) return false;

        ProtectAction[] candidates = actions.get();
        if (!ProtectPresets.levelOf(candidates).covers(stack.getCount())) return false;

        lastBlockedAction = strongest(candidates);
        return true;
    }

    private static ProtectAction strongest(ProtectAction[] actions) {
        ProtectAction best = actions[0];
        for (var action : actions) {
            if (ProtectPresets.levelOf(action).ordinal() > ProtectPresets.levelOf(best).ordinal()) best = action;
        }
        return best;
    }

    private static boolean blocksAmmo(Player player, ItemStack weapon) {
        if (weapon.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(weapon)) return false;

        ItemStack ammo = player.getProjectile(weapon);
        if (!inScope(ammo)) return false;

        if (!ProtectPresets.active().rule(ProtectAction.SHOOT_ARROW).matchesItem(ammo)) return false;

        if (weapon.getItem() instanceof BowItem && ammo.is(Items.ARROW)
                && EnchantmentHelper.getItemEnchantmentLevel(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY), weapon) > 0) return false;

        int slot = slotOf(player.getInventory(), ammo);
        if (slot < 0 || !isTargeted(slot)) return false;

        if (!ProtectPresets.levelOf(ProtectAction.SHOOT_ARROW).covers(ammo.getCount())) return false;

        lastBlockedAction = ProtectAction.SHOOT_ARROW;
        return true;
    }

    private static int slotOf(Inventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == stack) return i;
        }
        return -1;
    }

    public static void notifyBlocked(Player player, InteractionHand hand) {
        if (followsBlockedMainHand(player, hand)) return;

        if (lastBlockedAction != null) ActionBarMessages.protectBlocked(lastBlockedAction);

        var gui = Minecraft.getInstance().gui;
        if (hand == InteractionHand.MAIN_HAND) {
            mainHandBlockedTick = player.tickCount;
            ExtensionGui.flashIcon(gui);
        } else {
            ExtensionGui.flashIcon(gui, FavoriteSlots.OFFHAND_INDEX);
        }
    }
}
