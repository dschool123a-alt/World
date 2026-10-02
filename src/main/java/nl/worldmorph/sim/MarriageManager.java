package nl.worldmorph.sim;
import java.util.*; import nl.worldmorph.npc.NpcProfile;
public final class MarriageManager {
 public record Marriage(UUID first,UUID second,long startedTick){}
 private final Map<UUID,Marriage> byPerson=new HashMap<>();
 public boolean marry(NpcProfile a,NpcProfile b,long tick){if(a.id().equals(b.id())||!a.alive()||!b.alive()||byPerson.containsKey(a.id())||byPerson.containsKey(b.id()))return false;Marriage m=new Marriage(a.id(),b.id(),tick);byPerson.put(a.id(),m);byPerson.put(b.id(),m);a.addMemory("MARRIED",b.id(),tick,8);b.addMemory("MARRIED",a.id(),tick,8);return true;}
 public Marriage spouse(UUID id){return byPerson.get(id);}
 public void dissolve(UUID id){Marriage m=byPerson.remove(id);if(m!=null)byPerson.remove(m.first().equals(id)?m.second():m.first());}
}