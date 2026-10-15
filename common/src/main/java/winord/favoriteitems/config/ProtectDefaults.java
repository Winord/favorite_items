package winord.favoriteitems.config;

import java.util.List;

import static winord.favoriteitems.config.ProtectGroups.ref;

public final class ProtectDefaults {
    private static final List<String> EVERYTHING = List.of(ref(ProtectGroups.ALL));

    private ProtectDefaults() { }

    public static List<String> include(ProtectAction action) {
        return switch (action) {
            case PLACE_BLOCK -> List.of(ref("placeables"));
            case BONE_MEAL -> List.of("minecraft:bone_meal");
            case DYE -> List.of(ref("dyes"));
            case WAX -> List.of("minecraft:honeycomb");
            case THROW -> List.of("minecraft:snowball", "minecraft:egg", "minecraft:ender_pearl",
                    "minecraft:experience_bottle", "minecraft:ender_eye", "minecraft:wind_charge");
            case EAT -> List.of(ref("edible"));
            case FIREWORK -> List.of("minecraft:firework_rocket");
            case SHOOT_ARROW -> List.of("#minecraft:arrows", "minecraft:firework_rocket");
            case SPAWN_EGG -> List.of(ref("spawn_eggs"));
            case FEED_ANIMAL -> List.of(ref("animals"));
            case USE_ON_VILLAGER -> List.of("minecraft:villager", "minecraft:wandering_trader");
            case USE_ON_ENTITY, OTHER_USE -> EVERYTHING;
            case USE_ON_GUI_BLOCK -> List.of(ref("gui_blocks"));
            case USE_ON_REDSTONE -> List.of("#minecraft:wooden_doors", "#minecraft:wooden_trapdoors",
                    "#minecraft:fence_gates", "#minecraft:buttons", "minecraft:lever", "minecraft:repeater",
                    "minecraft:comparator", "minecraft:daylight_detector", "minecraft:note_block",
                    "minecraft:copper_door", "minecraft:exposed_copper_door", "minecraft:weathered_copper_door", "minecraft:oxidized_copper_door", "minecraft:waxed_copper_door", "minecraft:waxed_exposed_copper_door", "minecraft:waxed_weathered_copper_door", "minecraft:waxed_oxidized_copper_door", "minecraft:copper_trapdoor", "minecraft:exposed_copper_trapdoor", "minecraft:weathered_copper_trapdoor", "minecraft:oxidized_copper_trapdoor", "minecraft:waxed_copper_trapdoor", "minecraft:waxed_exposed_copper_trapdoor", "minecraft:waxed_weathered_copper_trapdoor", "minecraft:waxed_oxidized_copper_trapdoor");
            case USE_ON_BLOCK -> List.of("#minecraft:beds", "#minecraft:cauldrons", "#minecraft:campfires",
                    "#minecraft:candle_cakes", "#minecraft:flower_pots", "minecraft:cake", "minecraft:respawn_anchor",
                    "minecraft:decorated_pot", "minecraft:composter", "minecraft:jukebox", "minecraft:bell",
                    "minecraft:beehive", "minecraft:bee_nest", "minecraft:sweet_berry_bush", "minecraft:cave_vines",
                    "minecraft:cave_vines_plant", "minecraft:chiseled_bookshelf", "minecraft:vault", "#minecraft:wooden_shelves");
        };
    }
}
