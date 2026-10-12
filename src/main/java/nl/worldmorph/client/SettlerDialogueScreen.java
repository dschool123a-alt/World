package nl.worldmorph.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class SettlerDialogueScreen extends Screen {
    private final UUID npcId;
    private final String npcName;

    public SettlerDialogueScreen(UUID npcId, String npcName) {
        super(Component.literal("Settler"));
        this.npcId = npcId;
        this.npcName = npcName;
    }

    @Override
    protected void init() {
        int left = (this.width - 330) / 2;
        int top = this.height / 2 - 95;
        addRenderableWidget(Button.builder(Component.literal("Come with me"), b -> send("follow"))
            .bounds(left + 25, top + 100, 280, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Wait here"), b -> send("wait"))
            .bounds(left + 25, top + 128, 135, 22).build());
        addRenderableWidget(Button.builder(Component.literal("I found a place!"), b -> send("build"))
            .bounds(left + 25, top + 156, 135, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Maybe later"), b -> onClose())
            .bounds(left + 170, top + 128, 135, 22).build());
    }

    private void send(String action) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.connection.sendCommand("worldmorph arrival " + action + " " + npcId);
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
