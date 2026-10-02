package nl.worldmorph;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.data.WorldMorphStateAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorldMorph implements ModInitializer {
    public static final String MOD_ID = "worldmorph";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("WorldMorph loaded. Civilization simulation core is ready.");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            registerCommands(dispatcher)
        );
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("worldmorph")
                .then(Commands.literal("test")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                            () -> Component.literal("WorldMorph test OK - server-side core is running."),
                            false
                        );
                        return 1;
                    }))
                .then(Commands.literal("debug")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        WorldMorphState state = WorldMorphStateAccess.get(level);

                        context.getSource().sendSuccess(
                            () -> Component.literal(
                                "WorldMorph: tick=" + state.getSimulationTick()
                                    + ", kingdoms=" + state.kingdoms().size()
                                    + ", settlements=" + state.settlements().size()
                            ),
                            false
                        );
                        return 1;
                    }))
        );
    }
}
