package nl.worldmorph;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.data.WorldMorphStateAccess;
import nl.worldmorph.npc.NpcProfile;
import nl.worldmorph.sim.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

public final class WorldMorph implements ModInitializer {
 public static final String MOD_ID="worldmorph";
 public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
 private static final WorldMorphSimulation SIMULATION=new WorldMorphSimulation();

 @Override public void onInitialize(){
  LOGGER.info("WorldMorph civilization simulation loaded.");
  CommandRegistrationCallback.EVENT.register((dispatcher,registryAccess,environment)->registerCommands(dispatcher));
  ServerTickEvents.END_SERVER_TICK.register(SIMULATION::tick);
 }
 private static void registerCommands(CommandDispatcher<CommandSourceStack> d){
  var root=Commands.literal("worldmorph");
  root.then(Commands.literal("test").executes(c->{c.getSource().sendSuccess(()->Component.literal("WorldMorph OK - simulation running."),false);return 1;}));
  root.then(Commands.literal("debug").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("tick="+s.getSimulationTick()+" | kingdoms="+s.kingdoms().size()+" | settlements="+s.settlements().size()+" | npc profiles="+SIMULATION.npcs().size()+" | history="+s.history().size()),false);return 1;}));

  var kingdom=Commands.literal("kingdom");
  kingdom.then(Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("name",StringArgumentType.string()).executes(c->{var player=c.getSource().getPlayerOrException();var state=WorldMorphStateAccess.get(c.getSource().getLevel());var k=state.createKingdom(StringArgumentType.getString(c,"name"),player.getUUID());c.getSource().sendSuccess(()->Component.literal("Kingdom founded: "+k.name()+" (you are its ruler)."),true);return 1;})));
  kingdom.then(Commands.literal("list").executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());if(s.kingdoms().isEmpty()){c.getSource().sendSuccess(()->Component.literal("No kingdoms founded yet."),false);return 1;}for(var k:s.kingdoms().values())c.getSource().sendSuccess(()->Component.literal(k.name()+" | treasury "+k.treasury()+" | stability "+k.stability()+" | "+k.government()),false);return s.kingdoms().size();}));
  root.then(kingdom);

  root.then(Commands.literal("settlement").then(Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("name",StringArgumentType.string()).executes(c->{var player=c.getSource().getPlayerOrException();var s=WorldMorphStateAccess.get(c.getSource().getLevel());if(s.kingdoms().isEmpty()){c.getSource().sendFailure(Component.literal("Create a kingdom first with /worldmorph kingdom create <name>."));return 0;}var k=s.kingdoms().values().stream().filter(x->x.leader().equals(player.getUUID())).findFirst().orElse(s.kingdoms().values().iterator().next());var x=s.createSettlement(StringArgumentType.getString(c,"name"),player.blockPosition(),k.id());c.getSource().sendSuccess(()->Component.literal("Settlement founded: "+x.name()+" (population "+x.population()+")."),true);return 1;}))));

  var npc=Commands.literal("npc");
  npc.then(Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("name",StringArgumentType.string()).executes(c->{String name=StringArgumentType.getString(c,"name");if(SIMULATION.npcs().getByName(name)!=null){c.getSource().sendFailure(Component.literal("An NPC profile with that name already exists."));return 0;}NpcProfile p=SIMULATION.npcs().create(name);p.addMemory("MET_PLAYER",c.getSource().getPlayerOrException().getUUID(),WorldMorphStateAccess.get(c.getSource().getLevel()).getSimulationTick(),1);c.getSource().sendSuccess(()->Component.literal("NPC profile created: "+p.name()+" | age "+p.age()+" | loyalty "+p.loyalty()+" | ambition "+p.ambition()),true);return 1;})));
  npc.then(Commands.literal("list").executes(c->{var ps=SIMULATION.npcs().profiles().values();if(ps.isEmpty()){c.getSource().sendSuccess(()->Component.literal("No NPC profiles yet. Create one with /worldmorph npc create <name>."),false);return 1;}for(var p:ps)c.getSource().sendSuccess(()->Component.literal(p.name()+" | "+(p.alive()?"alive":"dead")+" | job="+p.job()+" | age="+p.age()+" | loyalty="+p.loyalty()+" | memories="+p.memories().size()),false);return ps.size();}));
  npc.then(Commands.literal("ask").then(Commands.argument("name",StringArgumentType.string()).executes(c->{var p=SIMULATION.npcs().getByName(StringArgumentType.getString(c,"name"));if(p==null||!p.alive()){c.getSource().sendFailure(Component.literal("NPC profile not found or no longer alive."));return 0;}var player=c.getSource().getPlayerOrException();long tick=WorldMorphStateAccess.get(c.getSource().getLevel()).getSimulationTick();SIMULATION.interactions().ask(p,player.getUUID(),tick);c.getSource().sendSuccess(()->Component.literal(p.name()+": Please! Can you help me? Reply with /worldmorph npc help "+p.name()+" or /worldmorph npc refuse "+p.name()+"."),false);return 1;})));
  npc.then(Commands.literal("help").then(Commands.argument("name",StringArgumentType.string()).executes(c->respondToNpc(c.getSource(),StringArgumentType.getString(c,"name"),NpcInteractionService.Choice.HELP))));
  npc.then(Commands.literal("refuse").then(Commands.argument("name",StringArgumentType.string()).executes(c->respondToNpc(c.getSource(),StringArgumentType.getString(c,"name"),NpcInteractionService.Choice.REFUSE))));
  npc.then(Commands.literal("memory").then(Commands.argument("name",StringArgumentType.string()).executes(c->{var p=SIMULATION.npcs().getByName(StringArgumentType.getString(c,"name"));if(p==null){c.getSource().sendFailure(Component.literal("NPC profile not found."));return 0;}c.getSource().sendSuccess(()->Component.literal("Memories for "+p.name()+":"),false);for(var m:p.memories())c.getSource().sendSuccess(()->Component.literal(m.type()+" | tick "+m.tick()+" | importance "+m.importance()),false);return p.memories().size();})));
  root.then(npc);

  root.then(Commands.literal("economy").then(Commands.literal("price").then(Commands.argument("item",StringArgumentType.word()).executes(c->{String item=StringArgumentType.getString(c,"item").toLowerCase(Locale.ROOT);c.getSource().sendSuccess(()->Component.literal("Market price for "+item+": "+SIMULATION.economy().price(item)+" coins."),false);return 1;}))));
  root.then(Commands.literal("reputation").then(Commands.argument("group",StringArgumentType.word()).executes(c->{var player=c.getSource().getPlayerOrException();try{var g=ReputationManager.Group.valueOf(StringArgumentType.getString(c,"group").toUpperCase(Locale.ROOT));c.getSource().sendSuccess(()->Component.literal(g+" reputation: "+SIMULATION.reputation().get(player.getUUID(),g)+"/100"),false);return 1;}catch(IllegalArgumentException ex){c.getSource().sendFailure(Component.literal("Groups: farmers, soldiers, builders, merchants, nobles, families, scholars."));return 0;}})));
  root.then(Commands.literal("technology").then(Commands.literal("list").executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());var k=s.kingdoms().values().stream().filter(x->c.getSource().getEntity()!=null&&x.leader().equals(c.getSource().getEntity().getUUID())).findFirst().orElse(null);if(k==null){c.getSource().sendFailure(Component.literal("You must lead a kingdom to inspect its technology."));return 0;}c.getSource().sendSuccess(()->Component.literal("Known technologies: "+SIMULATION.technology().known(k.id())),false);return 1;})));
  root.then(Commands.literal("war").then(Commands.literal("declare").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());if(s.kingdoms().size()<2){c.getSource().sendFailure(Component.literal("Need at least two kingdoms."));return 0;}var it=s.kingdoms().keySet().iterator();var a=it.next();var b=it.next();boolean ok=SIMULATION.diplomacy().declareWar(s,a,b,WarGoal.OTHER);c.getSource().sendSuccess(()->Component.literal(ok?"War declared between the first two kingdoms.":"War declaration rejected."),true);return ok?1:0;})));
  d.register(root);
 }
 private static int respondToNpc(CommandSourceStack source,String name,NpcInteractionService.Choice choice){
  var p=SIMULATION.npcs().getByName(name);if(p==null){source.sendFailure(Component.literal("NPC profile not found."));return 0;}
  try{var player=source.getPlayerOrException();long tick=WorldMorphStateAccess.get(source.getLevel()).getSimulationTick();var result=SIMULATION.interactions().respond(p,player.getUUID(),choice,tick);if(!result.accepted()){source.sendFailure(Component.literal(result.message()));return 0;}source.sendSuccess(()->Component.literal(result.message()+" Loyalty: "+p.loyalty()+"/100."),false);return 1;}catch(Exception ex){source.sendFailure(Component.literal("This action requires a player."));return 0;}
 }
}