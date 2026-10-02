package nl.worldmorph.sim;
import net.minecraft.server.MinecraftServer; import net.minecraft.server.level.ServerLevel; import nl.worldmorph.data.WorldMorphState; import nl.worldmorph.data.WorldMorphStateAccess;
public final class WorldMorphSimulation {
 private final EconomyManager economy=new EconomyManager(); private final DiplomacyManager diplomacy=new DiplomacyManager();
 public void tick(MinecraftServer server){ServerLevel l=server.overworld();WorldMorphState s=WorldMorphStateAccess.get(l);s.tick();if(s.getSimulationTick()%200==0){for(WorldMorphState.SettlementData x:s.settlements().values())if(x.population()<1000)s.settlements().put(x.id(),x.grow());economy.trade("wheat",100,100);}}
 public EconomyManager economy(){return economy;} public DiplomacyManager diplomacy(){return diplomacy;}
}