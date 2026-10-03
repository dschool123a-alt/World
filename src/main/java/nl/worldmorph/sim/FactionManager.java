package nl.worldmorph.sim;
import java.util.*;
public final class FactionManager {
 public enum Cause{TAXES,FOOD_SHORTAGE,NO_VOICE,MILITARY_RULE,REGIONAL_IDENTITY,ROYAL_ABUSE}
 public record Faction(UUID id,UUID kingdom,String name,Cause cause,int support,Set<UUID> members){public Faction{members=Set.copyOf(members);}}
 private final Map<UUID,Faction> factions=new LinkedHashMap<>();
 public Faction found(UUID kingdom,String name,Cause cause){Faction f=new Faction(UUID.randomUUID(),kingdom,name,cause,5,Set.of());factions.put(f.id(),f);return f;}
 public Faction support(UUID id,UUID citizen,int amount){Faction f=factions.get(id);if(f==null)return null;Set<UUID> m=new HashSet<>(f.members());m.add(citizen);Faction n=new Faction(f.id(),f.kingdom(),f.name(),f.cause(),Math.max(0,Math.min(100,f.support()+amount)),m);factions.put(id,n);return n;}
 public Collection<Faction> all(){return List.copyOf(factions.values());}
}