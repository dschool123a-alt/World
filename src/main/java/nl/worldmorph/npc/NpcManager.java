package nl.worldmorph.npc;
import java.util.*;
public final class NpcManager {
 private final Map<UUID,NpcProfile> profiles=new LinkedHashMap<>();
 public NpcProfile create(String name){NpcProfile p=new NpcProfile(UUID.randomUUID(),name);profiles.put(p.id(),p);return p;}
 public NpcProfile get(UUID id){return profiles.get(id);}
 public NpcProfile getByName(String name){return profiles.values().stream().filter(p->p.name().equalsIgnoreCase(name)).findFirst().orElse(null);}
 public int size(){return profiles.size();}
 public Map<UUID,NpcProfile> profiles(){return Map.copyOf(profiles);}
 public NpcProfile createForSettlement(String name,UUID settlement,UUID kingdom,long tick){NpcProfile p=create(name);p.setSettlement(settlement);p.setKingdom(kingdom);p.setAmbition(Math.floorMod(p.id().getLeastSignificantBits(),101));p.addMemory("ARRIVED",null,tick,1);return p;}
 public void simulate(long tick){if(tick%1200!=0)return;for(NpcProfile p:profiles.values())if(p.alive()){p.ageOne();if(p.age()>90){p.die();p.addMemory("DIED_OLD_AGE",null,tick,10);}}}
}