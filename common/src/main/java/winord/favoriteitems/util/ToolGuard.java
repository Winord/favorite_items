package winord.favoriteitems.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.mixin.AxeItemAccessor;
import winord.favoriteitems.mixin.BrushItemAccessor;
import winord.favoriteitems.mixin.HoeItemAccessor;
import winord.favoriteitems.mixin.ShovelItemAccessor;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class ToolGuard {
    public static final String AXE = "config." + MOD_ID + ".axe_protect";
    public static final String SHOVEL = "config." + MOD_ID + ".shovel_protect";
    public static final String HOE = "config." + MOD_ID + ".hoe_protect";
    public static final String SHEARS = "config." + MOD_ID + ".shears_protect";
    public static final String BRUSH = "config." + MOD_ID + ".brush_protect";

    private static final TagKey<Item> SHEARS_TAG_FABRIC = TagKey.create(Registries.ITEM, new ResourceLocation("c", "shears"));
    private static final TagKey<Item> SHEARS_TAG_FORGE = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "shears"));

    private ToolGuard() { }

    public static boolean anyEnabled() {
        if (FavoriteSlots.isSuspended()) return false;

        return FavoriteItemsConfig.axeProtect || FavoriteItemsConfig.shovelProtect || FavoriteItemsConfig.hoeProtect
                || FavoriteItemsConfig.shearsProtect || FavoriteItemsConfig.brushProtect;
    }

    public static String blockingToggleForBlockUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!anyEnabled()) return null;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();

        boolean axe = FavoriteItemsConfig.axeProtect && (item instanceof AxeItem || stack.is(ItemTags.AXES));
        boolean shovel = FavoriteItemsConfig.shovelProtect && (item instanceof ShovelItem || stack.is(ItemTags.SHOVELS));
        boolean hoe = FavoriteItemsConfig.hoeProtect && (item instanceof HoeItem || stack.is(ItemTags.HOES));
        boolean shears = FavoriteItemsConfig.shearsProtect && isShears(stack);
        boolean brush = FavoriteItemsConfig.brushProtect && item instanceof BrushItem;
        if (!axe && !shovel && !hoe && !shears && !brush) return null;

        BlockState state = player.level().getBlockState(hit.getBlockPos());
        Block block = state.getBlock();

        if (axe && axeActsOn(state, block)) return AXE;
        if (shovel && shovelActsOn(state, block)) return SHOVEL;
        if (hoe && HoeItemAccessor.favoriteitems$tillables().containsKey(block)) return HOE;
        if (shears && shearsActOn(state, block)) return SHEARS;
        if (brush && block instanceof BrushableBlock) return BRUSH;
        return null;
    }

    public static String blockingToggleForEntityUse(Player player, InteractionHand hand, Entity target) {
        if (!FavoriteItemsConfig.shearsProtect || FavoriteSlots.isSuspended()) return null;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !isShears(stack)) return null;

        return target instanceof Shearable shearable && shearable.readyForShearing() ? SHEARS : null;
    }

    public static void clientTick(Minecraft client) {
        if (!FavoriteItemsConfig.brushProtect || FavoriteSlots.isSuspended()) return;

        var player = client.player;
        if (player == null || client.level == null || client.gameMode == null || !player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof BrushItem brush)) return;

        var hit = ((BrushItemAccessor) (Object) brush).favoriteitems$calculateHitResult(player);
        if (hit instanceof BlockHitResult blockHit
                && client.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof BrushableBlock) {
            client.gameMode.releaseUsingItem(player);
            ActionBarMessages.toolBlocked(BRUSH);
        }
    }

    private static boolean isShears(ItemStack stack) {
        return stack.getItem() instanceof ShearsItem || stack.is(Items.SHEARS)
                || stack.is(SHEARS_TAG_FABRIC) || stack.is(SHEARS_TAG_FORGE);
    }

    private static boolean axeActsOn(BlockState state, Block block) {
        return AxeItemAccessor.favoriteitems$strippables().containsKey(block)
                || WeatheringCopper.getPrevious(state).isPresent()
                || HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(block);
    }

    private static boolean shovelActsOn(BlockState state, Block block) {
        return ShovelItemAccessor.favoriteitems$flattenables().containsKey(block)
                || CampfireBlock.isLitCampfire(state);
    }

    private static boolean shearsActOn(BlockState state, Block block) {
        return block instanceof PumpkinBlock
                || (block instanceof BeehiveBlock && state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS)
                || (block instanceof GrowingPlantHeadBlock head && !head.isMaxAge(state));
    }
}
