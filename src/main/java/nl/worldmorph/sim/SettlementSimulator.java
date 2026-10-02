package nl.worldmorph.sim;
import nl.worldmorph.data.WorldMorphState;
public final class SettlementSimulator {
 public void tick(WorldMorphState state){if(state.getSimulationTick()%200!=0)return;for(var settlement:state.settlements().values()){var updated=settlement.withPopulation(settlement.population());if(!updated.type().equals(settlement.type()))state.updateSettlement(updated);}}
}