package nl.worldmorph.sim;
import java.util.HashMap; import java.util.Map;
public final class EconomyManager {
 private final Map<String,Integer> prices=new HashMap<>();
 public EconomyManager(){prices.put("wheat",4);prices.put("wood",3);prices.put("stone",5);prices.put("iron",12);prices.put("bread",7);prices.put("tools",18);}
 public int price(String item){return prices.getOrDefault(item,10);}
 public void trade(String item,int supply,int demand){int p=price(item);if(demand>supply)p=Math.min(1000,p+Math.max(1,(demand-supply)/10));if(supply>demand)p=Math.max(1,p-Math.max(1,(supply-demand)/10));prices.put(item,p);}
 public Map<String,Integer> prices(){return Map.copyOf(prices);}
}