package nl.worldmorph.sim;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import nl.worldmorph.npc.NpcProfile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoreSimulationTest {
 @Test void marketPricesRespondToSupplyAndDemand(){
  EconomyManager economy=new EconomyManager();
  int start=economy.price("wheat");
  economy.trade("wheat",10,100);
  assertTrue(economy.price("wheat")>start);
  economy.trade("wheat",500,1);
  assertTrue(economy.price("wheat")>=1);
 }
 @Test void simulationBudgetUsesDistanceTiers(){
  SimulationBudget budget=new SimulationBudget();
  assertEquals(SimulationBudget.Tier.FULL,budget.tier(5,true));
  assertEquals(SimulationBudget.Tier.SETTLEMENT,budget.tier(12,false));
  assertEquals(SimulationBudget.Tier.STRATEGIC,budget.tier(32,false));
  assertEquals(SimulationBudget.Tier.DORMANT,budget.tier(80,false));
 }
 @Test void citizenMoodReflectsNeeds(){
  CitizenMoodManager mood=new CitizenMoodManager();
  assertEquals(CitizenMoodManager.Mood.CONTENT,mood.evaluate(new CitizenMoodManager.Factors(100,100,100,100,100)));
  assertEquals(CitizenMoodManager.Mood.REBELLIOUS,mood.evaluate(new CitizenMoodManager.Factors(5,5,5,5,5)));
 }
 @Test void helpRequestIsSingleUseAndChangesMemory(){
  NpcInteractionService service=new NpcInteractionService();
  NpcProfile npc=new NpcProfile(UUID.randomUUID(),"Test NPC");
  UUID player=UUID.randomUUID();
  service.ask(npc,player,10);
  var result=service.respond(npc,player,NpcInteractionService.Choice.HELP,11);
  assertTrue(result.accepted());
  assertEquals(70,npc.loyalty());
  assertFalse(service.respond(npc,player,NpcInteractionService.Choice.HELP,12).accepted());
  assertTrue(npc.memories().stream().anyMatch(m->m.type().equals("PLAYER_HELPED")));
 }
 @Test void homeAssignmentCanMoveAnNpcIn(){
  HomeManager homes=new HomeManager();
  UUID npc=UUID.randomUUID(),player=UUID.randomUUID();
  BlockPos home=new BlockPos(12,64,-4);
  homes.moveIntoPlayerHome(npc,player,home,100);
  assertTrue(homes.hasHome(npc));
  assertEquals(HomeManager.Type.HOST_HOME,homes.get(npc).type());
  assertEquals(home,homes.get(npc).position());
 }
}