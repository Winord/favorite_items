package winord.favoriteitems.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.FoodOnAStickItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopperGolemStatueBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.config.ProtectAction;
import winord.favoriteitems.config.ProtectDefaults;
import winord.favoriteitems.config.ProtectEntryMatcher;
import winord.favoriteitems.config.ProtectVanilla;
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

    private static final int UNPREDICTABLE_RESERVE = 2;
    private static final int SINGLE_USE_COST = 1;
    private static final int ARMADILLO_BRUSH_COST = 16;
    private static final int HOOKED_ITEM_COST = 3;
    private static final int HOOKED_ENTITY_COST = 5;
    private static final int HOOK_ON_GROUND_COST = 2;
    private static final int FIREWORK_SHOT_COST = 3;
    private static final int CARROT_STICK_COST = 7;

    private static final TagKey<Item> SHEARS_TAG_FABRIC = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "shears"));
    private static final TagKey<Item> SHEARS_TAG_TOOLS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/shear"));

    private static final ProtectEntryMatcher SCREEN_BLOCKS = ProtectEntryMatcher.parse(ProtectVanilla.GUI_BLOCKS);
    private static final ProtectEntryMatcher HAND_USABLE_BLOCKS = ProtectEntryMatcher.parse(ProtectDefaults.include(ProtectAction.USE_ON_REDSTONE));

    private ToolGuard() { }

    public static boolean anyEnabled() {
        if (FavoriteSlots.isSuspended()) return false;

        return FavoriteItemsConfig.axeProtect || FavoriteItemsConfig.shovelProtect || FavoriteItemsConfig.hoeProtect
                || FavoriteItemsConfig.shearsProtect || FavoriteItemsConfig.brushProtect;
    }

    public static boolean breakingProtectEnabled() {
        return FavoriteItemsConfig.toolsBreakingProtect && !FavoriteSlots.isSuspended();
    }

    public static boolean blocksItemUse(Player player, InteractionHand hand) {
        var stack = guardedStack(player.getItemInHand(hand));
        return stack != null && protect(stack, itemUseCost(player, stack));
    }

    public static boolean blocksBlockUse(Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = guardedStack(player.getItemInHand(hand));
        if (stack == null) return false;
        if (!player.isSecondaryUseActive() && opensScreen(player, hit)) return false;

        return protect(stack, blockUseCost(player, stack, hit));
    }

    public static boolean blocksEntityUse(Player player, InteractionHand hand, Entity target) {
        var stack = guardedStack(player.getItemInHand(hand));
        return stack != null && protect(stack, entityUseCost(player, stack, target));
    }

    public static boolean blocksBlockBreaking(Player player, BlockPos pos) {
        var stack = guardedStack(player.getMainHandItem());
        return stack != null && protect(stack, miningCost(stack, player.level(), pos));
    }

    public static boolean blocksEntityAttack(Player player, Entity target) {
        var stack = guardedStack(player.getMainHandItem());
        return stack != null && protect(stack, attackCost(stack, target));
    }

    public static boolean blocksPiercingAttack(Player player) {
        var stack = guardedStack(player.getMainHandItem());
        return stack != null && protect(stack, UNPREDICTABLE_RESERVE);
    }

    public static boolean blocksAttackStart(Player player, HitResult hit) {
        var stack = guardedStack(player.getMainHandItem());
        if (stack == null) return false;
        if (stack.has(DataComponents.PIERCING_WEAPON)) return protect(stack, UNPREDICTABLE_RESERVE);

        return protect(stack, hitCost(player, stack, hit));
    }

    public static boolean blocksMining(Player player, HitResult hit) {
        var stack = guardedStack(player.getMainHandItem());
        if (stack == null || stack.has(DataComponents.PIERCING_WEAPON)) return false;
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) return false;

        return protect(stack, miningCost(stack, player.level(), blockHit.getBlockPos()));
    }

    private static ItemStack guardedStack(ItemStack stack) {
        return breakingProtectEnabled() && isBreakable(stack) ? stack : null;
    }

    private static boolean isBreakable(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;

        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable == null || !isArmorSlot(equippable.slot());
    }

    private static boolean protect(ItemStack stack, int cost) {
        if (cost <= 0 || stack.getMaxDamage() - stack.getDamageValue() > cost) return false;

        ActionBarMessages.toolBreakBlocked(stack);
        return true;
    }

    private static int hitCost(Player player, ItemStack stack, HitResult hit) {
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            return attackCost(stack, entityHit.getEntity());
        }
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            return miningCost(stack, player.level(), blockHit.getBlockPos());
        }
        return 0;
    }

    private static int attackCost(ItemStack stack, Entity target) {
        var weapon = stack.get(DataComponents.WEAPON);
        return weapon != null && target instanceof LivingEntity ? weapon.itemDamagePerAttack() : 0;
    }

    private static int miningCost(ItemStack stack, Level level, BlockPos pos) {
        var tool = stack.get(DataComponents.TOOL);
        if (tool == null || tool.damagePerBlock() <= 0) return 0;

        var state = level.getBlockState(pos);
        if (state.isAir()) return 0;

        boolean costs = stack.getItem() instanceof ShearsItem
                ? !state.is(BlockTags.FIRE)
                : state.getDestroySpeed(level, pos) != 0.0F;
        return costs ? tool.damagePerBlock() : 0;
    }

    private static int itemUseCost(Player player, ItemStack stack) {
        var item = stack.getItem();
        var blocking = stack.get(DataComponents.BLOCKS_ATTACKS);
        if (blocking != null) return smallestShieldHitCost(blocking);
        if (stack.has(DataComponents.KINETIC_WEAPON)) return UNPREDICTABLE_RESERVE;
        if (item instanceof CrossbowItem) return crossbowShotCost(stack);
        if (item instanceof ProjectileWeaponItem) return SINGLE_USE_COST;
        if (item instanceof FishingRodItem) return player.fishing == null ? SINGLE_USE_COST : reelInCost(player.fishing);
        if (item instanceof FoodOnAStickItem<?>) return steeringCost(player, stack);
        return hasKnownBehavior(stack) ? 0 : UNPREDICTABLE_RESERVE;
    }

    private static int smallestShieldHitCost(BlocksAttacks blocking) {
        var damage = blocking.itemDamage();
        return damage.apply(damage.threshold());
    }

    private static int crossbowShotCost(ItemStack stack) {
        var charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (charged == null || charged.isEmpty()) return SINGLE_USE_COST;

        int cost = 0;
        for (var projectile : charged.itemCopies()) {
            cost += projectile.is(Items.FIREWORK_ROCKET) ? FIREWORK_SHOT_COST : SINGLE_USE_COST;
        }
        return cost;
    }

    private static int reelInCost(FishingHook hook) {
        int cost = hook.onGround() ? HOOK_ON_GROUND_COST : SINGLE_USE_COST;
        var hooked = hook.getHookedIn();
        if (hooked != null) cost = Math.max(cost, hooked instanceof ItemEntity ? HOOKED_ITEM_COST : HOOKED_ENTITY_COST);
        return cost;
    }

    private static int steeringCost(Player player, ItemStack stack) {
        if (!player.isPassenger() || !(player.getControlledVehicle() instanceof ItemSteerable)) return 0;
        return stack.is(Items.WARPED_FUNGUS_ON_A_STICK) ? SINGLE_USE_COST : CARROT_STICK_COST;
    }

    private static int blockUseCost(Player player, ItemStack stack, BlockHitResult hit) {
        var level = player.level();
        var state = level.getBlockState(hit.getBlockPos());
        var block = state.getBlock();

        int cost = 0;
        if (isAxe(stack) && (axeActsOn(state, block) || block instanceof WeatheringCopperGolemStatueBlock)) cost = SINGLE_USE_COST;
        if (isShovel(stack) && shovelActsOn(state, block)) cost = SINGLE_USE_COST;
        if (isHoe(stack) && HoeItemAccessor.favoriteitems$tillables().containsKey(block)) cost = SINGLE_USE_COST;
        if (isShears(stack) && shearsActOn(state, block)) cost = SINGLE_USE_COST;
        if (stack.getItem() instanceof BrushItem && block instanceof BrushableBlock) cost = SINGLE_USE_COST;
        if (stack.getItem() instanceof FlintAndSteelItem && flintActsOn(player, hit, state, block)) cost = SINGLE_USE_COST;
        if (cost == 0 && !hasKnownBehavior(stack)) cost = UNPREDICTABLE_RESERVE;
        return cost;
    }

    private static int entityUseCost(Player player, ItemStack stack, Entity target) {
        int cost = 0;
        if (isShears(stack) && shearsActOnEntity(player, target)) cost = SINGLE_USE_COST;
        if (isAxe(stack) && target instanceof CopperGolem) cost = SINGLE_USE_COST;
        if (stack.getItem() instanceof BrushItem && target instanceof Armadillo armadillo && !armadillo.isBaby()) cost = ARMADILLO_BRUSH_COST;
        if (stack.getItem() instanceof FlintAndSteelItem && target instanceof Creeper) cost = SINGLE_USE_COST;
        if (cost == 0 && !hasKnownBehavior(stack)) cost = UNPREDICTABLE_RESERVE;
        return cost;
    }

    private static boolean hasKnownBehavior(ItemStack stack) {
        var item = stack.getItem();
        return item.getClass() == Item.class || isAxe(stack) || isShovel(stack) || isHoe(stack) || isShears(stack)
                || item instanceof BrushItem || item instanceof FlintAndSteelItem || item instanceof MaceItem
                || item instanceof TridentItem || item instanceof ShieldItem || item instanceof ProjectileWeaponItem
                || item instanceof FishingRodItem || item instanceof FoodOnAStickItem<?>;
    }

    private static boolean isAxe(ItemStack stack) {
        return stack.getItem() instanceof AxeItem || stack.is(ItemTags.AXES);
    }

    private static boolean isShovel(ItemStack stack) {
        return stack.getItem() instanceof ShovelItem || stack.is(ItemTags.SHOVELS);
    }

    private static boolean isHoe(ItemStack stack) {
        return stack.getItem() instanceof HoeItem || stack.is(ItemTags.HOES);
    }

    private static boolean flintActsOn(Player player, BlockHitResult hit, BlockState state, Block block) {
        if (block instanceof TntBlock || CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state)) {
            return true;
        }
        if (!player.isSecondaryUseActive() && HAND_USABLE_BLOCKS.matchesBlock(state)) return false;

        return BaseFireBlock.canBePlacedAt(player.level(), hit.getBlockPos().relative(hit.getDirection()), player.getDirection());
    }

    private static boolean shearsActOnEntity(Player player, Entity target) {
        if (target instanceof Shearable shearable && shearable.readyForShearing()) return true;
        if (hasLeashConnection(target)) return true;
        if (target instanceof Wolf wolf && wolf.isOwnedBy(player) && !wolf.getBodyArmorItem().isEmpty()) return true;
        return target instanceof Mob mob && !mob.isVehicle() && !player.isSecondaryUseActive() && hasShearableEquipment(mob);
    }

    private static boolean hasLeashConnection(Entity entity) {
        return entity instanceof Leashable leashable && leashable.isLeashed() || !Leashable.leashableLeashedTo(entity).isEmpty();
    }

    private static boolean hasShearableEquipment(Mob mob) {
        for (var slot : EquipmentSlot.VALUES) {
            var equippable = mob.getItemBySlot(slot).get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.canBeSheared()) return true;
        }
        return false;
    }

    private static boolean opensScreen(Player player, BlockHitResult hit) {
        var level = player.level();
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        return state.getMenuProvider(level, pos) != null || SCREEN_BLOCKS.matchesBlock(state);
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET || slot == EquipmentSlot.BODY;
    }

    public static String blockingToggleForBlockUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!anyEnabled()) return null;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();

        boolean axe = FavoriteItemsConfig.axeProtect && isAxe(stack);
        boolean shovel = FavoriteItemsConfig.shovelProtect && isShovel(stack);
        boolean hoe = FavoriteItemsConfig.hoeProtect && isHoe(stack);
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
        if (FavoriteSlots.isSuspended()) return null;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return null;

        if (FavoriteItemsConfig.shearsProtect && isShears(stack)) {
            if (target instanceof Shearable shearable && shearable.readyForShearing()) return SHEARS;
            if (target instanceof Wolf wolf && wolf.isOwnedBy(player) && !wolf.getBodyArmorItem().isEmpty()) return SHEARS;
            return null;
        }

        if (FavoriteItemsConfig.brushProtect && stack.getItem() instanceof BrushItem
                && target instanceof Armadillo armadillo && !armadillo.isBaby()) {
            return BRUSH;
        }
        return null;
    }

    public static void clientTick(Minecraft client) {
        if (FavoriteSlots.isSuspended()) return;

        var player = client.player;
        if (player == null || client.level == null || client.gameMode == null || !player.isUsingItem()) return;

        var stack = player.getUseItem();
        if (stack.getItem() instanceof BrushItem brush && brushesBrushableBlock(client, player, brush)) {
            if (FavoriteItemsConfig.brushProtect) {
                client.gameMode.releaseUsingItem(player);
                ActionBarMessages.toolBlocked(BRUSH);
            } else if (breakingProtectEnabled() && isBreakable(stack) && protect(stack, SINGLE_USE_COST)) {
                client.gameMode.releaseUsingItem(player);
            }
            return;
        }

        if (breakingProtectEnabled() && isBreakable(stack) && protect(stack, itemUseCost(player, stack))) {
            client.gameMode.releaseUsingItem(player);
        }
    }

    private static boolean brushesBrushableBlock(Minecraft client, Player player, BrushItem brush) {
        var hit = ((BrushItemAccessor) (Object) brush).favoriteitems$calculateHitResult(player);
        return hit instanceof BlockHitResult blockHit
                && client.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof BrushableBlock;
    }

    private static boolean isShears(ItemStack stack) {
        return stack.getItem() instanceof ShearsItem || stack.is(Items.SHEARS)
                || stack.is(SHEARS_TAG_FABRIC) || stack.is(SHEARS_TAG_TOOLS);
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
