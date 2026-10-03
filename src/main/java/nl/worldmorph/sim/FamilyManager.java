package nl.worldmorph.sim;
import java.util.*;
import nl.worldmorph.npc.NpcProfile;
public final class FamilyManager {
 public record Family(UUID id,UUID head,Set<UUID> members,UUID homeSettlement){public Family{members=Set.copyOf(members);}}
 private final Map<UUID,Family> families=new LinkedHashMap<>();
 public Family found(NpcProfile founder,UUID settlement){UUID id=UUID.randomUUID();Family f=new Family(id,founder.id(),Set.of(founder.id()),settlement);families.put(id,f);founder.setFamily(id);return f;}
 public boolean add(UUID id,NpcProfile p){Family f=families.get(id);if(f==null||!p.alive())return false;Set<UUID> m=new LinkedHashSet<>(f.members());m.add(p.id());families.put(id,new Family(f.id(),f.head(),m,f.homeSettlement()));p.setFamily(id);return true;}
 public Optional<Family> get(UUID id){return Optional.ofNullable(families.get(id));} public Collection<Family> all(){return List.copyOf(families.values());}
}