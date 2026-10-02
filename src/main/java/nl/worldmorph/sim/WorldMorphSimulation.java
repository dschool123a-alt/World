package nl.worldmorph.sim;
import net.minecraft.server.MinecraftServer; import net.minecraft.server.level.ServerLevel; import nl.worldmorph.data.*; import nl.worldmorph.npc.NpcManager;
public final class WorldMorphSimulation {
 private final EconomyManager economy=new EconomyManager(); private final DiplomacyManager diplomacy=new DiplomacyManager(); private final SettlementSimulator settlements=new SettlementSimulator(); private final KingdomSimulator kingdoms=new KingdomSimulator(); private final NpcManager npcs=new NpcManager();
 public void tick(MinecraftServer server){ServerLevel l=server.overworld();WorldMorphState s=WorldMorphStateAccess.get(l);s.tick();settlements.tick(s);kingdoms.tick(s);npcs.simulate(s.getSimulationTick());if(s.getSimulationTick()%200==0)economy.trade("wheat",100,100);}
 public EconomyManager economy(){return economy;} public DiplomacyManager diplomacy(){return diplomacy;} public NpcManager npcs(){return npcs;}
}