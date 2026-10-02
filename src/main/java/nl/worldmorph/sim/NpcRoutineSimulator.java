package nl.worldmorph.sim;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.npc.NpcManager;
import nl.worldmorph.npc.NpcProfile;
public final class NpcRoutineSimulator {
 public void tick(WorldMorphState state,NpcManager npcs,SettlementNeedsManager needs,ResourceManager resources){
  if(state.getSimulationTick()%1200!=0)return;
  for(NpcProfile npc:npcs.profiles().values()){
   if(!npc.alive())continue;
   npc.changeMoney(1);
   if(npc.settlementId()==null)continue;
   switch(npc.job()){
    case "FARMER"->{resources.add(npc.settlementId(),ResourceManager.Resource.FOOD,3);needs.change(npc.settlementId(),SettlementNeedsManager.Need.FOOD,2);}
    case "BUILDER"->{resources.add(npc.settlementId(),ResourceManager.Resource.WOOD,1);needs.change(npc.settlementId(),SettlementNeedsManager.Need.HOUSING,1);}
    case "MERCHANT"->needs.change(npc.settlementId(),SettlementNeedsManager.Need.TRADE,1);
    case "BLACKSMITH"->resources.add(npc.settlementId(),ResourceManager.Resource.TOOLS,1);
    case "WOODCUTTER"->resources.add(npc.settlementId(),ResourceManager.Resource.WOOD,2);
    case "MINER"->resources.add(npc.settlementId(),ResourceManager.Resource.STONE,2);
    case "SCHOLAR"->needs.change(npc.settlementId(),SettlementNeedsManager.Need.WORK,1);
    default->{}
   }
   npc.addMemory("DAILY_ROUTINE_"+npc.job(),null,state.getSimulationTick(),1);
  }
 }
}