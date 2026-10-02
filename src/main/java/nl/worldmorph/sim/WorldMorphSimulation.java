package nl.worldmorph.sim;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import nl.worldmorph.data.*;
import nl.worldmorph.npc.NpcManager;
public final class WorldMorphSimulation {
 private final EconomyManager economy=new EconomyManager(); private final DiplomacyManager diplomacy=new DiplomacyManager(); private final SettlementSimulator settlements=new SettlementSimulator(); private final KingdomSimulator kingdoms=new KingdomSimulator(); private final NpcManager npcs=new NpcManager();
 private final FamilyManager families=new FamilyManager(); private final ReputationManager reputation=new ReputationManager(); private final LawManager laws=new LawManager(); private final TradeManager trade=new TradeManager(); private final TechnologyManager technology=new TechnologyManager(); private final HistoryBook history=new HistoryBook(); private final MilitaryManager military=new MilitaryManager(); private final SimulationBudget budget=new SimulationBudget(); private final NpcInteractionService interactions=new NpcInteractionService(); private final TreatyManager treaties=new TreatyManager(); private final JobManager jobs=new JobManager(); private final PersonalityEngine personalities=new PersonalityEngine(); private final MarketManager markets=new MarketManager(); private final WorldEventManager events=new WorldEventManager(); private final SettlementNeedsManager needs=new SettlementNeedsManager(); private final FactionManager factions=new FactionManager(); private final ConstructionPlanner construction=new ConstructionPlanner(); private final SuccessionManager succession=new SuccessionManager(); private final RebellionManager rebellion=new RebellionManager();
 private ServerLevel loadedLevel;
 public void tick(MinecraftServer server){
  ServerLevel level=server.overworld();WorldMorphState state=WorldMorphStateAccess.get(level);NpcPersistentState npcState=NpcPersistentStateAccess.get(level);
  if(loadedLevel!=level){npcs.restore(npcState.all());loadedLevel=level;}
  state.tick();settlements.tick(state);kingdoms.tick(state);npcs.simulate(state.getSimulationTick());treaties.expire(state.getSimulationTick());
  if(state.getSimulationTick()%20==0)npcState.replaceAll(npcs.snapshots());
  if(state.getSimulationTick()%200==0){economy.trade("wheat",100,100);military.dailyUpdate();}
  if(state.getSimulationTick()%1200==0)for(var k:state.kingdoms().values())if(rebellion.resolve(state,k.id())){}
 }
 public EconomyManager economy(){return economy;} public DiplomacyManager diplomacy(){return diplomacy;} public NpcManager npcs(){return npcs;} public FamilyManager families(){return families;} public ReputationManager reputation(){return reputation;} public LawManager laws(){return laws;} public TradeManager trade(){return trade;} public TechnologyManager technology(){return technology;} public HistoryBook history(){return history;} public MilitaryManager military(){return military;} public SimulationBudget budget(){return budget;} public NpcInteractionService interactions(){return interactions;} public TreatyManager treaties(){return treaties;} public JobManager jobs(){return jobs;} public PersonalityEngine personalities(){return personalities;} public MarketManager markets(){return markets;} public WorldEventManager events(){return events;} public SettlementNeedsManager needs(){return needs;} public FactionManager factions(){return factions;} public ConstructionPlanner construction(){return construction;} public SuccessionManager succession(){return succession;}
}