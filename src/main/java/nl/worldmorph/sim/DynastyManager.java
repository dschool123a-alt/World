package nl.worldmorph.sim;
import java.util.*;
public final class DynastyManager {
 public record Dynasty(UUID id,String name,Set<UUID> members,UUID currentRuler){public Dynasty{members=Set.copyOf(members);}}
 private final Map<UUID,Dynasty> dynasties=new LinkedHashMap<>();
 public Dynasty found(String name,UUID founder){Dynasty d=new Dynasty(UUID.randomUUID(),name,Set.of(founder),founder);dynasties.put(d.id(),d);return d;}
 public boolean add(UUID dynasty,UUID member){Dynasty d=dynasties.get(dynasty);if(d==null)return false;Set<UUID> m=new HashSet<>(d.members());m.add(member);dynasties.put(dynasty,new Dynasty(d.id(),d.name(),m,d.currentRuler()));return true;}
 public Dynasty setRuler(UUID dynasty,UUID ruler){Dynasty d=dynasties.get(dynasty);if(d==null||!d.members().contains(ruler))return null;Dynasty n=new Dynasty(d.id(),d.name(),d.members(),ruler);dynasties.put(dynasty,n);return n;}
 public Collection<Dynasty> all(){return List.copyOf(dynasties.values());}
}