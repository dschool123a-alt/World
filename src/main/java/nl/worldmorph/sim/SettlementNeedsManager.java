package nl.worldmorph.sim;
import java.util.*;
import nl.worldmorph.data.NeedsPersistentState;
public final class SettlementNeedsManager {
 public enum Need{FOOD,HOUSING,SAFETY,WORK,TRADE}
 public record NeedState(int food,int housing,int safety,int jobs,int trade){public int score(Need n){return switch(n){case FOOD->food;case HOUSING->housing;case SAFETY->safety;case WORK->jobs;case TRADE->trade;};}}
 private final Map<UUID,NeedState> needs=new HashMap<>();
 public NeedState get(UUID settlement){return needs.getOrDefault(settlement,new NeedState(50,50,50,50,50));}
 public void change(UUID id,Need need,int delta){NeedState s=get(id);int f=s.food(),h=s.housing(),safe=s.safety(),jobs=s.jobs(),trade=s.trade();switch(need){case FOOD->f=clamp(f+delta);case HOUSING->h=clamp(h+delta);case SAFETY->safe=clamp(safe+delta);case WORK->jobs=clamp(jobs+delta);case TRADE->trade=clamp(trade+delta);}needs.put(id,new NeedState(f,h,safe,jobs,trade));}
 public List<NeedsPersistentState.NeedData> snapshots(){return needs.entrySet().stream().map(e->new NeedsPersistentState.NeedData(e.getKey().toString(),e.getValue().food(),e.getValue().housing(),e.getValue().safety(),e.getValue().jobs(),e.getValue().trade())).toList();}
 public void restore(Collection<NeedsPersistentState.NeedData> values){needs.clear();for(var n:values)try{needs.put(UUID.fromString(n.settlement()),new NeedState(clamp(n.food()),clamp(n.housing()),clamp(n.safety()),clamp(n.jobs()),clamp(n.trade())));}catch(IllegalArgumentException ignored){}}
 private int clamp(int n){return Math.max(0,Math.min(100,n));}
}