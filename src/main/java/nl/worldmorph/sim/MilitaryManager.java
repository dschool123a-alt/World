package nl.worldmorph.sim;
import java.util.*;
public final class MilitaryManager {
 public enum UnitType{MILITIA,KNIGHT,ARCHER,SCOUT,ENGINEER}
 public record Army(UUID id,UUID kingdom,Map<UnitType,Integer> units,int morale,int supplyDays){public Army{units=Map.copyOf(units);}public int strength(){return units.entrySet().stream().mapToInt(e->e.getValue()*(e.getKey().ordinal()+2)).sum();}}
 private final Map<UUID,Army> armies=new LinkedHashMap<>();
 public Army muster(UUID kingdom,UnitType type,int count){if(count<1)return null;Army a=armies.values().stream().filter(x->x.kingdom().equals(kingdom)).findFirst().orElse(new Army(UUID.randomUUID(),kingdom,Map.of(),70,7));EnumMap<UnitType,Integer> units=new EnumMap<>(UnitType.class);units.putAll(a.units());units.merge(type,count,Integer::sum);Army n=new Army(a.id(),kingdom,units,a.morale(),a.supplyDays());armies.put(n.id(),n);return n;}
 public Collection<Army> armies(){return List.copyOf(armies.values());}
 public void dailyUpdate(){for(var a:new ArrayList<>(armies.values()))armies.put(a.id(),new Army(a.id(),a.kingdom(),a.units(),Math.max(0,a.morale()-(a.supplyDays()==0?12:1)),Math.max(0,a.supplyDays()-1)));}
}