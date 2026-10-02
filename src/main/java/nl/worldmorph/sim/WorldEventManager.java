package nl.worldmorph.sim;
import java.util.*; import nl.worldmorph.data.WorldMorphState;
public final class WorldEventManager {
 public enum Type{GOOD_HARVEST,POOR_HARVEST,MERCHANT_CARAVAN,IMMIGRATION,OUTBREAK,STORM,DISCOVERY,LOCAL_FESTIVAL}
 public record Event(UUID id,Type type,UUID settlement,long tick,String detail){}
 private final Deque<Event> recent=new ArrayDeque<>();
 public Event emit(WorldMorphState s,Type type,UUID settlement,String detail){Event e=new Event(UUID.randomUUID(),type,settlement,s.getSimulationTick(),detail);recent.addLast(e);while(recent.size()>500)recent.removeFirst();s.history("WORLD_EVENT",type+": "+detail);return e;}
 public List<Event> recent(){return List.copyOf(recent);}
}