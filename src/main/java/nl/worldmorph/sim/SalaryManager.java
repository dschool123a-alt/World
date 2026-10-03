package nl.worldmorph.sim;
import java.util.*;
public final class SalaryManager {
 public record Contract(UUID worker,UUID kingdom,int dailyPay,boolean employed){}
 private final Map<UUID,Contract> contracts=new HashMap<>();
 public Contract hire(UUID worker,UUID kingdom,int pay){if(pay<0)return null;Contract c=new Contract(worker,kingdom,pay,true);contracts.put(worker,c);return c;}
 public Contract fire(UUID worker){Contract c=contracts.get(worker);if(c==null)return null;Contract n=new Contract(c.worker(),c.kingdom(),c.dailyPay(),false);contracts.put(worker,n);return n;}
 public List<Contract> due(){return contracts.values().stream().filter(Contract::employed).toList();}
 public Map<UUID,Contract> contracts(){return Map.copyOf(contracts);}
}