package winord.favoriteitems;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientScreenInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import winord.favoriteitems.compat.ipn.IpnCompat;
import winord.favoriteitems.config.FavoriteItemsConfig;
import winord.favoriteitems.config.ProtectPresets;
import winord.favoriteitems.config.StylePreviewOverlay;
import winord.favoriteitems.util.FavoriteSlots;
import winord.favoriteitems.util.ToggleKeybinds;
import winord.favoriteitems.util.ToolGuard;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class FavoriteItemsClient {
    public static final KeyMapping FAVORITE_KEY = new KeyMapping(
            "key." + MOD_ID + ".favorite_item",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
            ToggleKeybinds.CATEGORY);

    private FavoriteItemsClient() { }

    public static void initClient() {
        FavoriteItemsConfig.load();
        ProtectPresets.load();
        KeyMappingRegistry.register(FAVORITE_KEY);
        ToggleKeybinds.register();

        ClientGuiEvent.INIT_POST.register((screen, access) -> StylePreviewOverlay.onInit(screen));
        ClientGuiEvent.RENDER_POST.register((screen, graphics, mouseX, mouseY, delta) ->
                StylePreviewOverlay.render(screen, graphics, mouseX, mouseY));
        ClientScreenInputEvent.MOUSE_CLICKED_PRE.register((client, screen, event, doubleClick) -> {
            StylePreviewOverlay.onMouseClicked(screen, event.x(), event.y());
            return EventResult.pass();
        });

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            IpnCompat.logStatus();
            var client = Minecraft.getInstance();
            var integratedServer = client.getSingleplayerServer();
            if (integratedServer != null) {
                var saveFolderName = integratedServer.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
                FavoriteSlots.enterScope(FavoriteSlots.worldScopeFile(saveFolderName));
                return;
            }

            var serverData = client.getCurrentServer();
            if (serverData != null) {
                FavoriteSlots.enterScope(FavoriteSlots.serverScopeFile(serverData.ip));
            } else {
                FavoriteItems.LOGGER.warn(
                        "Joined a world with neither an integrated server nor a server list entry; favorites won't be saved this session.");
            }
        });
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> FavoriteSlots.leaveScope());

        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null) return;

            FavoriteSlots.clientTick(client.player.getInventory());
            ToolGuard.clientTick(client);
            ToggleKeybinds.tick();
        });
    }
}
