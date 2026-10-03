package nl.worldmorph.sim; import nl.worldmorph.data.WorldMorphState;
public final class KingdomSimulator {
 public void tick(WorldMorphState s){if(s.getSimulationTick()%100!=0)return;for(var x:s.kingdoms().values()){int income=0;for(var settlement:s.settlements().values())if(x.id().equals(settlement.kingdomId()))income+=Math.max(1,settlement.population()/20);int upkeep=Math.max(1,settlementCount(s,x.id()));s.updateKingdom(x.withTreasury(Math.max(0,x.treasury()+income-upkeep)));}}
 private int settlementCount(WorldMorphState s,java.util.UUID id){int n=0;for(var x:s.settlements().values())if(id.equals(x.kingdomId()))n++;return n;}
}