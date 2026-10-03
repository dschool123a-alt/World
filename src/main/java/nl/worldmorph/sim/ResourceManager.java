package nl.worldmorph.sim;
import java.util.*;
public final class ResourceManager {
 public enum Resource{WOOD,STONE,IRON,GRAIN,FOOD,TOOLS,GOLD}
 private final Map<UUID,EnumMap<Resource,Long>> stores=new HashMap<>();
 public long get(UUID settlement,Resource r){return stores.getOrDefault(settlement,new EnumMap<>(Resource.class)).getOrDefault(r,0L);}
 public long add(UUID settlement,Resource r,long amount){var m=stores.computeIfAbsent(settlement,k->new EnumMap<>(Resource.class));long next=Math.max(0,Math.min(Long.MAX_VALUE,m.getOrDefault(r,0L)+amount));m.put(r,next);return next;}
 public boolean consume(UUID settlement,Resource r,long amount){if(amount<0||get(settlement,r)<amount)return false;add(settlement,r,-amount);return true;}
 public Map<Resource,Long> inventory(UUID settlement){return Map.copyOf(stores.getOrDefault(settlement,new EnumMap<>(Resource.class)));}
}