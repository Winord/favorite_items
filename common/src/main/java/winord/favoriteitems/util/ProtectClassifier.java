package winord.favoriteitems.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.phys.BlockHitResult;
import winord.favoriteitems.config.ProtectAction;
import winord.favoriteitems.config.ProtectPreset;
import winord.favoriteitems.config.ProtectPresets;

public final class ProtectClassifier {
    private static final ProtectAction[] NONE = new ProtectAction[0];

    private static final ProtectAction[] ITEM_PRIORITY = {
            ProtectAction.BONE_MEAL, ProtectAction.WAX, ProtectAction.DYE, ProtectAction.FIREWORK,
            ProtectAction.THROW, ProtectAction.SPAWN_EGG };

    private ProtectClassifier() { }

    public static ProtectAction[] forItemUse(ItemStack stack) {
        return of(classifyItem(stack, false));
    }

    public static ProtectAction[] forBlockUse(Player player, ItemStack stack, BlockHitResult hit) {
        var state = player.level().getBlockState(hit.getBlockPos());
        ProtectPreset preset = ProtectPresets.active();

        if (!player.isSecondaryUseActive()) {
            if (preset.rule(ProtectAction.USE_ON_GUI_BLOCK).matchesBlock(state)) {
                var byItem = state.is(BlockTags.ALL_SIGNS) && stack.getItem() instanceof SignApplicator
                        ? classifyItem(stack, false)
                        : null;
                return byItem != null
                        ? new ProtectAction[] { ProtectAction.USE_ON_GUI_BLOCK, byItem }
                        : of(ProtectAction.USE_ON_GUI_BLOCK);
            }
            if (preset.rule(ProtectAction.USE_ON_REDSTONE).matchesBlock(state)) return of(ProtectAction.USE_ON_REDSTONE);

            if (preset.rule(ProtectAction.USE_ON_BLOCK).matchesBlock(state)) {
                var byItem = classifyItem(stack, true);
                return byItem != null
                        ? new ProtectAction[] { ProtectAction.USE_ON_BLOCK, byItem }
                        : of(ProtectAction.USE_ON_BLOCK);
            }
        }
        return of(classifyItem(stack, true));
    }

    public static ProtectAction[] forEntityUse(ItemStack stack, Entity target) {
        ProtectPreset preset = ProtectPresets.active();

        if (preset.rule(ProtectAction.USE_ON_VILLAGER).matchesEntity(target)) return of(ProtectAction.USE_ON_VILLAGER);

        if (preset.rule(ProtectAction.FEED_ANIMAL).matchesEntity(target)
                && (!(target instanceof Animal animal) || animal.isFood(stack))) {
            return of(ProtectAction.FEED_ANIMAL);
        }

        var byItem = classifyItem(stack, false);
        if (byItem == ProtectAction.DYE || byItem == ProtectAction.SPAWN_EGG) return of(byItem);

        return preset.rule(ProtectAction.USE_ON_ENTITY).matchesEntity(target) ? of(ProtectAction.USE_ON_ENTITY) : NONE;
    }

    private static ProtectAction classifyItem(ItemStack stack, boolean onBlock) {
        ProtectPreset preset = ProtectPresets.active();

        for (var action : ITEM_PRIORITY) {
            if (preset.rule(action).matchesItem(stack)) return action;
        }

        boolean placeable = preset.rule(ProtectAction.PLACE_BLOCK).matchesItem(stack);
        if (onBlock && placeable) return ProtectAction.PLACE_BLOCK;

        if (preset.rule(ProtectAction.EAT).matchesItem(stack)) return ProtectAction.EAT;
        if (placeable) return ProtectAction.PLACE_BLOCK;

        return preset.rule(ProtectAction.OTHER_USE).matchesItem(stack) ? ProtectAction.OTHER_USE : null;
    }

    private static ProtectAction[] of(ProtectAction action) {
        return action != null ? new ProtectAction[] { action } : NONE;
    }
}
