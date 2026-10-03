package nl.worldmorph.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public final class WorldMorphClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (entity.hasCustomName() && entity.getCustomName() != null) {
                String name = entity.getCustomName().getString();
                if (name.equals("Settler") || name.equals("Builder") || name.equals("Farmer")) {
                    Minecraft.getInstance().setScreen(new SettlerDialogueScreen(entity.getUUID(), name));
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });
    }
}
