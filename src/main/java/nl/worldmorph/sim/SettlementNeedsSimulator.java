package nl.worldmorph.sim;
import nl.worldmorph.data.WorldMorphState;
public final class SettlementNeedsSimulator {
 public void tick(WorldMorphState state,SettlementNeedsManager needs,CitizenMoodManager moods){
  if(state.getSimulationTick()%1200!=0)return;
  for(var settlement:state.settlements().values()){
   var n=needs.get(settlement.id());
   int taxFairness=state.kingdoms().containsKey(settlement.kingdomId())?100-state.kingdoms().get(settlement.kingdomId()).taxRate():50;
   var mood=moods.evaluate(new CitizenMoodManager.Factors(n.food(),n.safety(),n.housing(),taxFairness,50));
   int population=settlement.population();
   if(n.food()<20||n.housing()<15)population=Math.max(0,population-1);
   else if(n.food()>65&&n.housing()>65&&n.safety()>40)population=Math.min(1000,population+1);
   if(population!=settlement.population()){state.updateSettlement(settlement.withPopulation(population));state.history("POPULATION_CHANGE",settlement.name()+" population is now "+population);}
   var kingdom=state.kingdoms().get(settlement.kingdomId());
   if(kingdom!=null&&mood!=CitizenMoodManager.Mood.CONTENT&&mood!=CitizenMoodManager.Mood.HOPEFUL){state.updateKingdom(kingdom.withStability(kingdom.stability()+moods.stabilityEffect(mood)));}
  }
 }
}