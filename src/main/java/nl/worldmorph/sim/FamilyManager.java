package nl.worldmorph.sim;
import java.util.*;
import nl.worldmorph.npc.NpcProfile;
public final class FamilyManager {
 public record Family(UUID id,UUID head,Set<UUID> members,UUID homeSettlement){public Family{members=Set.copyOf(members);}}
 private final Map<UUID,Family> families=new LinkedHashMap<>();
 public Family found(NpcProfile founder,UUID settlement){UUID id=UUID.randomUUID();Family f=new Family(id,founder.id(),Set.of(founder.id()),settlement);families.put(id,f);founder.setFamily(id);return f;}
 public boolean add(UUID id,NpcProfile p){Family f=families.get(id);if(f==null||!p.alive())return false;Set<UUID> m=new LinkedHashSet<>(f.members());m.add(p.id());families.put(id,new Family(f.id(),f.head(),m,f.homeSettlement()));p.setFamily(id);return true;}
 public boolean merge(UUID firstId,UUID secondId,UUID preferredHead){
  if(firstId==null||secondId==null||firstId.equals(secondId)) return false;
  Family a=families.get(firstId), b=families.get(secondId); if(a==null||b==null) return false;
  Set<UUID> members=new LinkedHashSet<>(a.members()); members.addAll(b.members());
  UUID head=members.contains(preferredHead)?preferredHead:a.head();
  Family merged=new Family(a.id(),head,members,a.homeSettlement());
  families.put(a.id(),merged); families.remove(b.id());
  for(UUID member:members) if(member.equals(a.head())||member.equals(b.head())) { /* profiles are relinked by callers */ }
  return true;
 }
 public Optional<Family> get(UUID id){return Optional.ofNullable(families.get(id));} public Collection<Family> all(){return List.copyOf(families.values());}
}