package nl.worldmorph.sim;
import java.util.HashMap; import java.util.Map; import java.util.List; import java.util.ArrayList; import java.util.Collection; import nl.worldmorph.data.EconomyPersistentState;
public final class EconomyManager {
 private final Map<String,Integer> prices=new HashMap<>();
 public EconomyManager(){prices.put("wheat",4);prices.put("wood",3);prices.put("stone",5);prices.put("iron",12);prices.put("bread",7);prices.put("tools",18);}
 public int price(String item){return prices.getOrDefault(item,10);}
 public void trade(String item,int supply,int demand){int p=price(item);if(demand>supply)p=Math.min(1000,p+Math.max(1,(demand-supply)/10));if(supply>demand)p=Math.max(1,p-Math.max(1,(supply-demand)/10));prices.put(item,p);}
 public Map<String,Integer> prices(){return Map.copyOf(prices);}
 public List<EconomyPersistentState.PriceData> snapshots(){return prices.entrySet().stream().map(e->new EconomyPersistentState.PriceData(e.getKey(),e.getValue())).toList();}
 public void restore(Collection<EconomyPersistentState.PriceData> saved){if(saved.isEmpty())return;prices.clear();for(var p:saved)if(p.price()>0)prices.put(p.item(),Math.min(1000,p.price()));}
}