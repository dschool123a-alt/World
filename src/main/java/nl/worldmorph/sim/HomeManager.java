package nl.worldmorph.sim;
import java.util.*;
import net.minecraft.core.BlockPos;
import nl.worldmorph.data.HomePersistentState;
public final class HomeManager {
 public enum Type{HOST_HOME,OWN_HOUSE,SHARED_HOUSE}
 public record Home(UUID npc,UUID hostPlayer,UUID settlement,BlockPos position,Type type,long movedInTick){}
 private final Map<UUID,Home> homes=new LinkedHashMap<>();
 public Home moveIntoPlayerHome(UUID npc,UUID player,BlockPos pos,long tick){Home h=new Home(npc,player,null,pos,Type.HOST_HOME,tick);homes.put(npc,h);return h;}
 public Home buildOwnHome(UUID npc,UUID settlement,BlockPos pos,long tick){Home h=new Home(npc,null,settlement,pos,Type.OWN_HOUSE,tick);homes.put(npc,h);return h;}
 public Home get(UUID npc){return homes.get(npc);}
 public boolean hasHome(UUID npc){return homes.containsKey(npc);}
 public Collection<Home> all(){return List.copyOf(homes.values());}
 public boolean move(UUID npc,BlockPos pos,long tick){Home old=homes.get(npc);if(old==null)return false;homes.put(npc,new Home(npc,old.hostPlayer(),old.settlement(),pos,old.type(),tick));return true;}
 public List<HomePersistentState.HomeData> snapshots(){return homes.values().stream().map(h->new HomePersistentState.HomeData(h.npc().toString(),id(h.hostPlayer()),id(h.settlement()),h.position(),h.type().name(),h.movedInTick())).toList();}
 public void restore(Collection<HomePersistentState.HomeData> data){homes.clear();for(var d:data)try{UUID npc=UUID.fromString(d.npc());UUID host=uuid(d.hostPlayer());UUID settlement=uuid(d.settlement());Type type=Type.valueOf(d.type());homes.put(npc,new Home(npc,host,settlement,d.position(),type,d.movedInTick()));}catch(IllegalArgumentException ignored){}}
 private static String id(UUID id){return id==null?\"\":id.toString();}
 private static UUID uuid(String value){if(value==null||value.isBlank())return null;try{return UUID.fromString(value);}catch(IllegalArgumentException ex){return null;}}
}