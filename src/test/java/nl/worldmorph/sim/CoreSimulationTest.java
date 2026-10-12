package nl.worldmorph.sim;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import nl.worldmorph.npc.NpcProfile;
import nl.worldmorph.data.WorldMorphState;
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
 @Test void minedConstructionMaterialsAreConsumedFromTheStockpile(){
  WorldMorphState state=new WorldMorphState();
  UUID settlement=UUID.randomUUID();
  state.addConstructionMaterial(settlement,"STONE",2);
  assertEquals(2,state.constructionMaterial(settlement,"STONE"));
  assertTrue(state.consumeConstructionMaterial(settlement,"STONE"));
  assertEquals(1,state.constructionMaterial(settlement,"STONE"));
  assertTrue(state.consumeConstructionMaterial(settlement,"STONE"));
  assertFalse(state.consumeConstructionMaterial(settlement,"STONE"));
  assertEquals(0,state.constructionMaterial(settlement,"STONE"));
 }
 @Test void tradeRouteShipsOnlyStockedGoodsAndArrivesAfterTravelTime(){
  WorldMorphState state=new WorldMorphState();
  UUID leaderA=UUID.randomUUID(),leaderB=UUID.randomUUID();
  var kingdomA=state.createKingdom("North",leaderA);
  var kingdomB=state.createKingdom("South",leaderB);
  var origin=state.createSettlement("NorthVillage",new BlockPos(0,64,0),kingdomA.id());
  var destination=state.createSettlement("SouthVillage",new BlockPos(100,64,0),kingdomB.id());
  ResourceManager resources=new ResourceManager();
  RoadNetwork roads=new RoadNetwork();
  TradeRouteManager routes=new TradeRouteManager();
  resources.add(origin.id(),ResourceManager.Resource.WOOD,10);
  var route=routes.create(origin.id(),destination.id(),"wood",4,20);
  assertNotNull(route);
  routes.tick(state,resources,roads);
  assertEquals(6,resources.get(origin.id(),ResourceManager.Resource.WOOD));
  assertEquals(0,resources.get(destination.id(),ResourceManager.Resource.WOOD));
  for(int i=0;i<100;i++){state.tick();routes.tick(state,resources,roads);}
  assertEquals(4,resources.get(destination.id(),ResourceManager.Resource.WOOD));
  assertTrue(state.history().stream().anyMatch(e->e.type().equals("TRADE_ROUTE_ARRIVED")));
 }
 @Test void tradeRouteRejectsInvalidGoodsAndSelfTrade(){
  TradeRouteManager routes=new TradeRouteManager();
  UUID settlement=UUID.randomUUID();
  assertNull(routes.create(settlement,settlement,"WOOD",1,20));
  assertNull(routes.create(UUID.randomUUID(),UUID.randomUUID(),"DIAMONDS",1,20));
  assertNull(routes.create(UUID.randomUUID(),UUID.randomUUID(),"WOOD",0,20));
 }

}