package nl.worldmorph;

import com.mojang.brigadier.CommandDispatcher;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.data.WorldMorphStateAccess;
import nl.worldmorph.sim.WarGoal;
import nl.worldmorph.sim.WorldMorphSimulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorldMorph implements ModInitializer {
 public static final String MOD_ID="worldmorph"; public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
 private static final WorldMorphSimulation SIMULATION=new WorldMorphSimulation();
 @Override public void onInitialize(){
  LOGGER.info("WorldMorph civilization simulation loaded.");
  CommandRegistrationCallback.EVENT.register((d,r,e)->registerCommands(d));
  ServerTickEvents.END_SERVER_TICK.register(SIMULATION::tick);
 }
 private static void registerCommands(CommandDispatcher<CommandSourceStack>d){
  var root=Commands.literal("worldmorph");
  root.then(Commands.literal("test").executes(c->{c.getSource().sendSuccess(()->Component.literal("WorldMorph OK - civilization simulation is running."),false);return 1;}));
  root.then(Commands.literal("debug").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{WorldMorphState s=WorldMorphStateAccess.get(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("tick="+s.getSimulationTick()+" | kingdoms="+s.kingdoms().size()+" | settlements="+s.settlements().size()+" | history="+s.history().size()),false);return 1;}));
  root.then(Commands.literal("kingdom").then(Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("name",net.minecraft.commands.arguments.StringArgumentType.string()).executes(c->{String n=net.minecraft.commands.arguments.StringArgumentType.getString(c,"name");var p=c.getSource().getPlayerOrException();var s=WorldMorphStateAccess.get(c.getSource().getLevel());var k=s.createKingdom(n,p.getUUID());c.getSource().sendSuccess(()->Component.literal("Kingdom created: "+k.name()),true);return 1;}))));
  root.then(Commands.literal("settlement").then(Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("name",net.minecraft.commands.arguments.StringArgumentType.string()).executes(c->{var p=c.getSource().getPlayerOrException();var s=WorldMorphStateAccess.get(c.getSource().getLevel());if(s.kingdoms().isEmpty()){c.getSource().sendFailure(Component.literal("Create a kingdom first."));return 0;}var k=s.kingdoms().values().iterator().next();var x=s.createSettlement(net.minecraft.commands.arguments.StringArgumentType.getString(c,"name"),p.blockPosition(),k.id());c.getSource().sendSuccess(()->Component.literal("Settlement founded: "+x.name()),true);return 1;}))));
  root.then(Commands.literal("war").then(Commands.literal("declare").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());if(s.kingdoms().size()<2){c.getSource().sendFailure(Component.literal("Need at least two kingdoms."));return 0;}var it=s.kingdoms().keySet().iterator();UUID a=it.next(),b=it.next();boolean ok=SIMULATION.diplomacy().declareWar(s,a,b,WarGoal.OTHER);c.getSource().sendSuccess(()->Component.literal(ok?"War declared.":"War declaration rejected."),true);return ok?1:0;})));
  d.register(root);
 }
}