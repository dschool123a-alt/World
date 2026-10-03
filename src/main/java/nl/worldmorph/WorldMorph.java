package nl.worldmorph;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
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
  root.then(Commands.literal("debug").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("tick="+s.getSimulationTick()+" | kingdoms="+s.kingdoms().size()+" | settlements="+s.settlements().size()+" | NPCs="+SIMULATION.npcs().size()+" | history="+s.history().size()),false);return 1;}));
  root.then(kingdomCommands()); root.then(settlementCommands()); root.then(npcCommands());
  root.then(jobCommands()); root.then(lawCommands()); root.then(taxCommands()); root.then(familyCommands()); root.then(homeCommands());
  root.then(historyCommands()); root.then(economyCommands()); root.then(reputationCommands()); root.then(technologyCommands()); root.then(warCommands());
  d.register(root);
 }
 private static LiteralArgumentBuilder<CommandSourceStack> kingdomCommands(){
  var root=Commands.literal("kingdom");
  var create=Commands.argument("name",StringArgumentType.string()).executes(c->{var player=c.getSource().getPlayerOrException();var s=WorldMorphStateAccess.get(c.getSource().getLevel());var k=s.createKingdom(StringArgumentType.getString(c,"name"),player.getUUID());c.getSource().sendSuccess(()->Component.literal("Kingdom founded: "+k.name()+" (you are its ruler)."),true);return 1;});
  root.then(Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(create));
  root.then(Commands.literal("list").executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());if(s.kingdoms().isEmpty()){c.getSource().sendSuccess(()->Component.literal("No kingdoms founded yet."),false);return 1;}for(var k:s.kingdoms().values())c.getSource().sendSuccess(()->Component.literal(k.name()+" | treasury "+k.treasury()+" | stability "+k.stability()+" | "+k.government()),false);return s.kingdoms().size();}));
  return root;
 }
 private static LiteralArgumentBuilder<CommandSourceStack> settlementCommands(){
  var create=Commands.argument("name",StringArgumentType.string()).executes(c->{var player=c.getSource().getPlayerOrException();var s=WorldMorphStateAccess.get(c.getSource().getLevel());if(s.kingdoms().isEmpty()){c.getSource().sendFailure(Component.literal("Create a kingdom first with /worldmorph kingdom create <name>."));return 0;}var k=s.kingdoms().values().stream().filter(x->x.leader().equals(player.getUUID())).findFirst().orElse(s.kingdoms().values().iterator().next());var x=s.createSettlement(StringArgumentType.getString(c,"name"),player.blockPosition(),k.id());c.getSource().sendSuccess(()->Component.literal("Settlement founded: "+x.name()+" (population "+x.population()+")."),true);return 1;});
  return Commands.literal("settlement").then(Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(create));
 }
 private static LiteralArgumentBuilder<CommandSourceStack> npcCommands(){
  var root=Commands.literal("npc");
  var create=Commands.argument("name",StringArgumentType.string()).executes(c->{String name=StringArgumentType.getString(c,"name");if(SIMULATION.npcs().getByName(name)!=null){c.getSource().sendFailure(Component.literal("An NPC profile with that name already exists."));return 0;}NpcProfile p=SIMULATION.npcs().create(name);p.addMemory("MET_PLAYER",c.getSource().getPlayerOrException().getUUID(),WorldMorphStateAccess.get(c.getSource().getLevel()).getSimulationTick(),1);c.getSource().sendSuccess(()->Component.literal("NPC profile created: "+p.name()+" | age "+p.age()+" | loyalty "+p.loyalty()+" | ambition "+p.ambition()),true);return 1;});
  root.then(Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(create));
  var spawn=Commands.argument("name",StringArgumentType.string()).executes(c->{var player=c.getSource().getPlayerOrException();var level=c.getSource().getLevel();String name=StringArgumentType.getString(c,"name");if(SIMULATION.npcs().getByName(name)!=null){c.getSource().sendFailure(Component.literal("An NPC with that name already exists."));return 0;}var villagerType=BuiltInRegistries.ENTITY_TYPE.get(Identifier.fromNamespaceAndPath("minecraft","villager")).orElseThrow();Entity villager=villagerType.value().create(level,EntitySpawnReason.COMMAND);if(villager==null){c.getSource().sendFailure(Component.literal("Could not create villager."));return 0;}villager.setPos(player.getX()+1,player.getY(),player.getZ());villager.setCustomName(Component.literal(name));villager.setCustomNameVisible(true);if(!level.addFreshEntity(villager)){c.getSource().sendFailure(Component.literal("Could not add villager to the world."));return 0;}NpcProfile profile=SIMULATION.npcs().create(villager.getUUID(),name);profile.addMemory("MET_PLAYER",player.getUUID(),WorldMorphStateAccess.get(level).getSimulationTick(),1);c.getSource().sendSuccess(()->Component.literal("WorldMorph NPC spawned: "+name+"."),true);return 1;});
  root.then(Commands.literal("spawn").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(spawn));
  root.then(Commands.literal("list").executes(c->{var ps=SIMULATION.npcs().profiles().values();if(ps.isEmpty()){c.getSource().sendSuccess(()->Component.literal("No NPC profiles yet."),false);return 1;}for(var p:ps)c.getSource().sendSuccess(()->Component.literal(p.name()+" | "+(p.alive()?"alive":"dead")+" | job="+p.job()+" | age="+p.age()+" | loyalty="+p.loyalty()+" | memories="+p.memories().size()),false);return ps.size();}));
  var ask=Commands.argument("name",StringArgumentType.string()).executes(c->{var p=SIMULATION.npcs().getByName(StringArgumentType.getString(c,"name"));if(p==null||!p.alive()){c.getSource().sendFailure(Component.literal("NPC profile not found or no longer alive."));return 0;}var player=c.getSource().getPlayerOrException();long tick=WorldMorphStateAccess.get(c.getSource().getLevel()).getSimulationTick();SIMULATION.interactions().ask(p,player.getUUID(),tick);var entity=c.getSource().getLevel().getEntity(p.id());if(entity!=null)SIMULATION.bubbles().show(entity,tick,"Please! Can you help me?",100);c.getSource().sendSuccess(()->Component.literal(p.name()+": Please! Can you help me? Reply with /worldmorph npc help "+p.name()+" or /worldmorph npc refuse "+p.name()+"."),false);return 1;});
  root.then(Commands.literal("ask").then(ask));
  root.then(Commands.literal("help").then(Commands.argument("name",StringArgumentType.string()).executes(c->respondToNpc(c.getSource(),StringArgumentType.getString(c,"name"),NpcInteractionService.Choice.HELP))));
  root.then(Commands.literal("refuse").then(Commands.argument("name",StringArgumentType.string()).executes(c->respondToNpc(c.getSource(),StringArgumentType.getString(c,"name"),NpcInteractionService.Choice.REFUSE))));
  root.then(Commands.literal("memory").then(Commands.argument("name",StringArgumentType.string()).executes(c->{var p=SIMULATION.npcs().getByName(StringArgumentType.getString(c,"name"));if(p==null){c.getSource().sendFailure(Component.literal("NPC profile not found."));return 0;}c.getSource().sendSuccess(()->Component.literal("Memories for "+p.name()+":"),false);for(var m:p.memories())c.getSource().sendSuccess(()->Component.literal(m.type()+" | tick "+m.tick()+" | importance "+m.importance()),false);return p.memories().size();})));
  return root;
 }
 private static LiteralArgumentBuilder<CommandSourceStack> jobCommands(){
  var jobArg=Commands.argument("job",StringArgumentType.word()).executes(c->{var p=SIMULATION.npcs().getByName(StringArgumentType.getString(c,"npc"));if(p==null){c.getSource().sendFailure(Component.literal("NPC profile not found."));return 0;}try{var job=JobType.valueOf(StringArgumentType.getString(c,"job").toUpperCase(Locale.ROOT));SIMULATION.jobs().assign(p,job);c.getSource().sendSuccess(()->Component.literal(p.name()+" is now a "+job+"."),true);return 1;}catch(IllegalArgumentException ex){c.getSource().sendFailure(Component.literal("Jobs: "+java.util.Arrays.toString(JobType.values())));return 0;}});
  var npcArg=Commands.argument("npc",StringArgumentType.word()).then(jobArg);
  return Commands.literal("job").then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(npcArg));
 }
 private static LiteralArgumentBuilder<CommandSourceStack> lawCommands(){
  var lawArg=Commands.argument("law",StringArgumentType.word()).executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());var player=c.getSource().getPlayerOrException();var k=s.kingdoms().values().stream().filter(x->x.leader().equals(player.getUUID())).findFirst().orElse(null);if(k==null){c.getSource().sendFailure(Component.literal("You must lead a kingdom first."));return 0;}try{var law=LawManager.Law.valueOf(StringArgumentType.getString(c,"law").toUpperCase(Locale.ROOT));boolean added=SIMULATION.laws().enact(k.id(),law);if(added)s.history("LAW_ENACTED",k.name()+" enacted "+law);c.getSource().sendSuccess(()->Component.literal(added?"Law enacted: "+law:"That law is already active."),true);return 1;}catch(IllegalArgumentException ex){c.getSource().sendFailure(Component.literal("Laws: "+java.util.Arrays.toString(LawManager.Law.values())));return 0;}});
  return Commands.literal("law").then(Commands.literal("enact").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(lawArg));
 }
 private static LiteralArgumentBuilder<CommandSourceStack> taxCommands(){
  var rate=Commands.argument("rate",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,75)).executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());var player=c.getSource().getPlayerOrException();var k=s.kingdoms().values().stream().filter(x->x.leader().equals(player.getUUID())).findFirst().orElse(null);if(k==null){c.getSource().sendFailure(Component.literal("You must lead a kingdom first."));return 0;}int value=SIMULATION.taxes().setRate(s,k.id(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"rate"));c.getSource().sendSuccess(()->Component.literal("Tax rate for "+k.name()+" set to "+value+" coins per citizen."),true);return 1;});
  return Commands.literal("tax").then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(rate));
 }
 private static LiteralArgumentBuilder<CommandSourceStack> familyCommands(){
  var npc=Commands.argument("npc",StringArgumentType.word()).executes(c->{var p=SIMULATION.npcs().getByName(StringArgumentType.getString(c,"npc"));if(p==null){c.getSource().sendFailure(Component.literal("NPC profile not found."));return 0;}var s=WorldMorphStateAccess.get(c.getSource().getLevel());var f=SIMULATION.families().found(p,p.settlementId());s.history("FAMILY_FOUNDED",p.name()+" founded a family.");c.getSource().sendSuccess(()->Component.literal("Family founded for "+p.name()+"."),true);return 1;});
  return Commands.literal("family").then(Commands.literal("found").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(npc));
 }
 private static LiteralArgumentBuilder<CommandSourceStack> homeCommands(){
  var hostNpc=Commands.argument("npc",StringArgumentType.word()).executes(c->{var npc=SIMULATION.npcs().getByName(StringArgumentType.getString(c,"npc"));if(npc==null){c.getSource().sendFailure(Component.literal("NPC profile not found."));return 0;}var player=c.getSource().getPlayerOrException();long tick=WorldMorphStateAccess.get(c.getSource().getLevel()).getSimulationTick();SIMULATION.homes().moveIntoPlayerHome(npc.id(),player.getUUID(),player.blockPosition(),tick);npc.addMemory("MOVED_IN_WITH_PLAYER",player.getUUID(),tick,5);c.getSource().sendSuccess(()->Component.literal(npc.name()+" now lives with you."),true);return 1;});
  var buildNpc=Commands.argument("npc",StringArgumentType.word()).executes(c->{var npc=SIMULATION.npcs().getByName(StringArgumentType.getString(c,"npc"));if(npc==null){c.getSource().sendFailure(Component.literal("NPC profile not found."));return 0;}var player=c.getSource().getPlayerOrException();var level=c.getSource().getLevel();var origin=player.blockPosition().offset(4,0,4);if(!new HouseBuilder().buildStarterHouse(level,origin)){c.getSource().sendFailure(Component.literal("House site is obstructed; choose a clear area."));return 0;}long tick=WorldMorphStateAccess.get(level).getSimulationTick();SIMULATION.homes().buildOwnHome(npc.id(),npc.settlementId(),origin.offset(2,0,2),tick);npc.addMemory("BUILT_OWN_HOUSE",null,tick,5);WorldMorphStateAccess.get(level).history("HOUSE_BUILT",npc.name()+" received a starter house.");c.getSource().sendSuccess(()->Component.literal("Built a starter house for "+npc.name()+" nearby."),true);return 1;});
  var root=Commands.literal("home");root.then(Commands.literal("host").then(hostNpc));root.then(Commands.literal("build").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(buildNpc));return root;
 }
 private static LiteralArgumentBuilder<CommandSourceStack> historyCommands(){return Commands.literal("history").executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());var entries=s.history();int start=Math.max(0,entries.size()-10);for(int i=start;i<entries.size();i++){var e=entries.get(i);c.getSource().sendSuccess(()->Component.literal("[tick "+e.tick()+"] "+e.type()+": "+e.description()),false);}if(entries.isEmpty())c.getSource().sendSuccess(()->Component.literal("No history recorded yet."),false);return Math.max(1,entries.size()-start);});}
 private static LiteralArgumentBuilder<CommandSourceStack> economyCommands(){return Commands.literal("economy").then(Commands.literal("price").then(Commands.argument("item",StringArgumentType.word()).executes(c->{String item=StringArgumentType.getString(c,"item").toLowerCase(Locale.ROOT);c.getSource().sendSuccess(()->Component.literal("Market price for "+item+": "+SIMULATION.economy().price(item)+" coins."),false);return 1;})));}
 private static LiteralArgumentBuilder<CommandSourceStack> reputationCommands(){return Commands.literal("reputation").then(Commands.argument("group",StringArgumentType.word()).executes(c->{var player=c.getSource().getPlayerOrException();try{var g=ReputationManager.Group.valueOf(StringArgumentType.getString(c,"group").toUpperCase(Locale.ROOT));c.getSource().sendSuccess(()->Component.literal(g+" reputation: "+SIMULATION.reputation().get(player.getUUID(),g)+"/100"),false);return 1;}catch(IllegalArgumentException ex){c.getSource().sendFailure(Component.literal("Groups: farmers, soldiers, builders, merchants, nobles, families, scholars."));return 0;}}));}
 private static LiteralArgumentBuilder<CommandSourceStack> technologyCommands(){return Commands.literal("technology").then(Commands.literal("list").executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());var entity=c.getSource().getEntity();var k=s.kingdoms().values().stream().filter(x->entity!=null&&x.leader().equals(entity.getUUID())).findFirst().orElse(null);if(k==null){c.getSource().sendFailure(Component.literal("You must lead a kingdom to inspect its technology."));return 0;}c.getSource().sendSuccess(()->Component.literal("Known technologies: "+SIMULATION.technology().known(k.id())),false);return 1;}));}
 private static LiteralArgumentBuilder<CommandSourceStack> warCommands(){return Commands.literal("war").then(Commands.literal("declare").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{var s=WorldMorphStateAccess.get(c.getSource().getLevel());if(s.kingdoms().size()<2){c.getSource().sendFailure(Component.literal("Need at least two kingdoms."));return 0;}var it=s.kingdoms().keySet().iterator();var a=it.next();var other=it.next();boolean ok=SIMULATION.diplomacy().declareWar(s,a,other,WarGoal.OTHER);c.getSource().sendSuccess(()->Component.literal(ok?"War declared between the first two kingdoms.":"War declaration rejected."),true);return ok?1:0;}));}
 private static int respondToNpc(CommandSourceStack source,String name,NpcInteractionService.Choice choice){
  var p=SIMULATION.npcs().getByName(name);if(p==null){source.sendFailure(Component.literal("NPC profile not found."));return 0;}
  try{var player=source.getPlayerOrException();long tick=WorldMorphStateAccess.get(source.getLevel()).getSimulationTick();var result=SIMULATION.interactions().respond(p,player.getUUID(),choice,tick);if(!result.accepted()){source.sendFailure(Component.literal(result.message()));return 0;}source.sendSuccess(()->Component.literal(result.message()+" Loyalty: "+p.loyalty()+"/100."),false);return 1;}catch(Exception ex){source.sendFailure(Component.literal("This action requires a player."));return 0;}
 }
}