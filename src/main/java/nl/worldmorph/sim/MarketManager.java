package nl.worldmorph.sim;
import java.util.*;
public final class MarketManager {
 public record Stock(int quantity,int basePrice){}
 private final Map<UUID,Map<String,Stock>> stock=new HashMap<>();
 public void restock(UUID settlement,String good,int amount,int basePrice){if(amount<0||basePrice<1)return;stock.computeIfAbsent(settlement,k->new HashMap<>()).compute(good,(k,v)->new Stock((v==null?0:v.quantity())+amount,basePrice));}
 public int price(UUID settlement,String good,int demand){Stock s=stock.getOrDefault(settlement,Map.of()).get(good);if(s==null)return 10;int scarcity=Math.max(-s.basePrice()/2,Math.min(s.basePrice()*4,(demand-s.quantity())*s.basePrice()/100));return Math.max(1,s.basePrice()+scarcity);}
 public boolean buy(UUID settlement,String good,int amount){if(amount<1)return false;Map<String,Stock> m=stock.get(settlement);if(m==null)return false;Stock s=m.get(good);if(s==null||s.quantity()<amount)return false;m.put(good,new Stock(s.quantity()-amount,s.basePrice()));return true;}
}