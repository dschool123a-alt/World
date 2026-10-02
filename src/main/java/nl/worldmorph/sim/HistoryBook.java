package nl.worldmorph.sim;
import java.util.*; import nl.worldmorph.data.WorldMorphState;
public final class HistoryBook {
 public record Entry(long tick,String category,String headline,String detail){}
 private final Deque<Entry> entries=new ArrayDeque<>();
 public void record(WorldMorphState s,String category,String headline,String detail){entries.addLast(new Entry(s.getSimulationTick(),category,headline,detail));while(entries.size()>5000)entries.removeFirst();s.history(category,headline+": "+detail);}
 public List<Entry> recent(int count){return entries.stream().skip(Math.max(0,entries.size()-Math.max(0,count))).toList();}
}