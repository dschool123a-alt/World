package nl.worldmorph.npc;
import java.util.*;
import nl.worldmorph.data.NpcPersistentState;
public final class NpcManager {
 private final Map<UUID,NpcProfile> profiles=new LinkedHashMap<>();
 public NpcProfile create(String name){NpcProfile p=new NpcProfile(UUID.randomUUID(),name);profiles.put(p.id(),p);return p;}
 public NpcProfile get(UUID id){return profiles.get(id);}
 public NpcProfile getByName(String name){return profiles.values().stream().filter(p->p.name().equalsIgnoreCase(name)).findFirst().orElse(null);}
 public int size(){return profiles.size();}
 public Map<UUID,NpcProfile> profiles(){return Map.copyOf(profiles);}
 public NpcProfile createForSettlement(String name,UUID settlement,UUID kingdom,long tick){NpcProfile p=create(name);p.setSettlement(settlement);p.setKingdom(kingdom);p.setAmbition(Math.floorMod(p.id().getLeastSignificantBits(),101));p.addMemory("ARRIVED",null,tick,1);return p;}
 public List<NpcPersistentState.NpcData> snapshots(){
  List<NpcPersistentState.NpcData> out=new ArrayList<>();
  for(NpcProfile p:profiles.values()){
   List<NpcPersistentState.MemoryData> memories=p.memories().stream().map(m->new NpcPersistentState.MemoryData(m.type(),m.target()==null?"":m.target().toString(),m.tick(),m.importance())).toList();
   out.add(new NpcPersistentState.NpcData(p.id().toString(),p.name(),p.age(),p.money(),p.ambition(),p.loyalty(),p.alive(),p.job(),id(p.familyId()),id(p.settlementId()),id(p.kingdomId()),p.personality().name(),memories));
  }return List.copyOf(out);
 }
 public void restore(Collection<NpcPersistentState.NpcData> data){
  profiles.clear();for(var d:data)try{
   NpcProfile p=new NpcProfile(UUID.fromString(d.id()),d.name());p.setAge(d.age());p.setMoney(d.money());p.setAmbition(d.ambition());p.setLoyalty(d.loyalty());p.setAlive(d.alive());p.setJob(d.job());p.setFamily(uuid(d.familyId()));p.setSettlement(uuid(d.settlementId()));p.setKingdom(uuid(d.kingdomId()));
   try{p.setPersonality(NpcPersonality.valueOf(d.personality()));}catch(IllegalArgumentException ignored){}
   for(var m:d.memories())p.addMemory(m.type(),uuid(m.target()),m.tick(),m.importance());profiles.put(p.id(),p);
  }catch(IllegalArgumentException ignored){}
 }
 private static String id(UUID id){return id==null?"":id.toString();}
 private static UUID uuid(String id){if(id==null||id.isBlank())return null;try{return UUID.fromString(id);}catch(IllegalArgumentException ex){return null;}}
 public void simulate(long tick){if(tick%1200!=0)return;for(NpcProfile p:profiles.values())if(p.alive()){p.ageOne();if(p.age()>90){p.die();p.addMemory("DIED_OLD_AGE",null,tick,10);}}}
}