package nl.worldmorph.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
        addRenderableWidget(Button.builder(Component.literal("Maybe later"), b -> onClose())
            .bounds(left + 170, top + 128, 135, 22).build());
    }

    private void send(String action) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.connection.sendCommand("worldmorph arrival " + action + " " + npcId);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        int left = (this.width - 330) / 2;
        int top = this.height / 2 - 95;
        graphics.fill(left, top, left + 330, top + 175, 0xE8101016);
        graphics.fill(left + 2, top + 2, left + 328, top + 4, 0xFFD6B36A);
        graphics.drawCenteredString(this.font, Component.literal(npcName), this.width / 2, top + 18, 0xFFF1D7A1);
        graphics.drawCenteredString(this.font, Component.literal("A new beginning"), this.width / 2, top + 40, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("We are looking for a place to settle."), this.width / 2, top + 62, 0xFFE8E8E8);
        graphics.drawCenteredString(this.font, Component.literal("Will you lead us?"), this.width / 2, top + 77, 0xFFE8E8E8);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}
