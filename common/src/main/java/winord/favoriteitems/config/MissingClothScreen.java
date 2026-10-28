package winord.favoriteitems.config;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.net.URI;

import static winord.favoriteitems.FavoriteItems.MOD_ID;

public final class MissingClothScreen extends Screen {
    private static final URI CLOTH_CONFIG_URL = URI.create("https://modrinth.com/project/9s6osm5g");

    private final Screen parent;

    public MissingClothScreen(Screen parent) {
        super(Component.translatable("config." + MOD_ID + ".title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MultiLineTextWidget message = new MultiLineTextWidget(width / 2 - 120, height / 2 - 50,
                Component.translatable("config." + MOD_ID + ".missing_cloth.message"), minecraft.font);
        message.setMaxWidth(240);
        message.setCentered(true);
        message.setX(width / 2 - message.getWidth() / 2);
        addRenderableWidget(message);

        addRenderableWidget(Button.builder(Component.translatable("config." + MOD_ID + ".missing_cloth.modrinth"),
                        b -> minecraft.gui.setScreen(new ConfirmLinkScreen(confirmed -> {
                            if (confirmed) Blaze3D.openUri(CLOTH_CONFIG_URL);
                            minecraft.gui.setScreen(this);
                        }, CLOTH_CONFIG_URL, true)))
                .pos(width / 2 - 115, height / 2 - 10)
                .size(230, 20)
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .pos(width / 2 - 115, height / 2 + 14)
                .size(230, 20)
                .build());
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}