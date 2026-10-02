package nl.worldmorph.sim;
import java.util.*;
public final class CrimeManager {
 public enum Crime{THEFT,MARKET_FRAUD,ASSAULT,TAX_EVASION,TREASON}
 public record Case(UUID id,UUID suspect,Crime crime,int severity,long tick,boolean resolved){}
 private final Map<UUID,Case> cases=new LinkedHashMap<>();
 public Case report(UUID suspect,Crime crime,int severity,long tick){Case c=new Case(UUID.randomUUID(),suspect,crime,Math.max(1,Math.min(100,severity)),tick,false);cases.put(c.id(),c);return c;}
 public Case resolve(UUID id){Case c=cases.get(id);if(c==null)return null;Case n=new Case(c.id(),c.suspect(),c.crime(),c.severity(),c.tick(),true);cases.put(id,n);return n;}
 public Collection<Case> openCases(){return cases.values().stream().filter(c->!c.resolved()).toList();}
}