# Favorite Items
Favorite Items is a small **client-side** mod that lets you favorite inventory slots so the items in them can't be
dropped or sent to a container by accident. Nothing needs to be installed on the server, so it works on any server,
vanilla included.

Hold the **Favorite Item** key (default: `Left Alt`, configurable under *Controls → Favorite Items*) and left-click a stack
to favorite or unfavorite it. Favorited stacks get an icon (a star by default; ten styles to choose from).

## Features
- **Terraria-style favorites**: the favorite follows the item. Pick a favorited stack up and place it in another slot
  and the favorite moves with it. This also holds for number-key swaps, equipping armor or an elytra by
  right-clicking it, and Pick Block (middle click) on a favorited stack in the inventory.
- **Blocks the ways a favorited item could be lost**:
    - Dropping it with `Q` / `Ctrl+Q`, from the hotbar or from inside the inventory, or by dropping it outside the
      screen (what is still allowed is set by *Drop Favorites*).
    - Shift-clicking it into another container.
    - Number-key swapping it into another container.
    - Spreading a favorited stack over several slots by click-and-drag.
    - Starting a double-click gather on it.
- **Smart shift-click**: shift-clicking a matching item out of a container first tops off your favorited stack instead
  of scattering the rest into a new slot.
- **Shift-click in your own inventory**: shift-clicking a favorited stack does nothing. Double shift-clicking it tops
  the favorited stack up to a full stack from identical stacks that aren't favorited, if there are any.
- **Additional Protection** (off by default): optionally stops a favorited item in your hand from being *used* (placing
  blocks, eating, throwing, using on entities or blocks, ...), per action and per preset, with include/exclude lists.
- **Tool protection**: optional toggles that stop axes, shovels, hoes, shears and brushes from doing their special action.
- **Optional (Extra tab):** keep favorites on servers with a lobby (off by default), and keep favorites after death
  (off by default): items you pick up near where you died get their favorite back, as many as you lost.
- **Ender Chest support**: Ender Chest slots can be favorited as well.
- **Toggle keys**: the *Favorite Items* group in *Controls* also has keys, unbound by default, that switch Additional
  Protection and the Axes / Shovels / Hoes / Shears / Brushes protection on and off without opening the settings.
- **Automatic cleanup**: when a favorited stack is used up (eaten, thrown, placed, ...) the favorite is removed.

Favorites are stored per world/server under `config/favoriteitems/worlds/` and `config/favoriteitems/servers/`, so
favoriting something in one world or on one server doesn't affect any other. With the lobby option on, each favorite
remembers its item, so on servers with a lobby the favorites stay dormant there and come back when you return to the game mode.

## Compatibility
Favorited slots are left alone by these sorting mods (each integration only activates when the mod is installed):
- [Inventory Sorting](https://modrinth.com/mod/inventory-sorting)
- [ClientSort](https://modrinth.com/mod/clientsort)
- Nemo's Inventory Sorting: sort, move-all, move-same and drop-all buttons, on the 1.20.1 builds as well as the newer
  builds that have their own slot locking.
- Inventory Profiles Next: sorting, move-all, scrolling and slot locks leave favorites alone, and auto refill can skip
  them or top up a favorited stack whose last item is protected.
- Litematica: Easy Place and Pick Block skip a favorited stack that Additional Protection guards.

By default favorites also stay put when the *Move Same Items* buttons and keys (Nemo's Inventory Sorting, ClientSort,
Inventory Profiles Next) run. The Extra tab can change that so that stackable favorites move too, one item staying in
the favorite slot.

Download the latest version over at [Modrinth](https://modrinth.com/mod/favorite_items)!

## Customizing further
The icon (`Custom` style) and toggle sound (`Custom` option) both ship with placeholder assets whose only
job is to be replaced. Enable a resource pack (*Options → Resource Packs*, works alongside any texture pack) that
contains these files and it overrides the built-in placeholders automatically, no config needed beyond picking
`Custom` in the mod's own config screen:
```
assets/favoriteitems/textures/gui/custom_mask.png      18x18, dyeable layer (draw it in grayscale) -- tinted by the Custom style's Dyeable layer color
assets/favoriteitems/textures/gui/custom_overlay.png   18x18, static colors layer -- fixed colors, never tinted
assets/favoriteitems/sounds/custom.ogg                 the sound played when Custom is picked in "Favorite toggle sound"
```
A ready-to-edit starter pack with this exact layout (plus a `pack.mcmeta`) lives in [`custom-resourcepack/`](custom-resourcepack)
-- copy it into your `resourcepacks` folder (or zip it), swap the three files for your own, and enable it like any
other resource pack.

## Known limitations
- Closing the inventory while carrying a favorited stack on the cursor puts the favorite back on the slot the stack
  returns to; with several identical stacks it may pick the wrong one.
- Shift-clicking an equipped favorite armor piece is blocked; take it off with a normal click.
- Additional Protection is client-side: commands and server-side actions are not stopped.

## License
All Rights Reserved — see [`LICENSE`](LICENSE).
