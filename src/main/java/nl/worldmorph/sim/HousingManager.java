package nl.worldmorph.sim;
import java.util.*;
public final class HousingManager {
 public record Housing(int houses,int capacity,int residents){}
 private final Map<UUID,Housing> housing=new HashMap<>();
 public Housing get(UUID settlement){return housing.getOrDefault(settlement,new Housing(0,0,0));}
 public Housing addHouse(UUID settlement,int capacity){Housing h=get(settlement);Housing n=new Housing(h.houses()+1,h.capacity()+Math.max(1,capacity),h.residents());housing.put(settlement,n);return n;}
 public boolean moveIn(UUID settlement,int count){Housing h=get(settlement);if(count<1||h.residents()+count>h.capacity())return false;housing.put(settlement,new Housing(h.houses(),h.capacity(),h.residents()+count));return true;}
 public void moveOut(UUID settlement,int count){Housing h=get(settlement);housing.put(settlement,new Housing(h.houses(),h.capacity(),Math.max(0,h.residents()-Math.max(0,count))));}
}