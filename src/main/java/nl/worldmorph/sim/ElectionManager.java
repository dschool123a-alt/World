package nl.worldmorph.sim;
import java.util.*;
public final class ElectionManager {
 public record Election(UUID kingdom,long openedTick,long closesTick,Map<UUID,Integer> votes,boolean closed){public Election{votes=Map.copyOf(votes);}}
 private final Map<UUID,Election> elections=new HashMap<>();
 public Election open(UUID kingdom,long tick,long duration){if(elections.containsKey(kingdom)&&!elections.get(kingdom).closed())return null;Election e=new Election(kingdom,tick,tick+Math.max(1,duration),Map.of(),false);elections.put(kingdom,e);return e;}
 public boolean vote(UUID kingdom,UUID voter,UUID candidate,long tick){Election e=elections.get(kingdom);if(e==null||e.closed()||tick>e.closesTick())return false;Map<UUID,Integer> votes=new HashMap<>(e.votes());votes.merge(candidate,1,Integer::sum);elections.put(kingdom,new Election(kingdom,e.openedTick(),e.closesTick(),votes,false));return true;}
 public UUID winner(UUID kingdom,long tick){Election e=elections.get(kingdom);if(e==null||tick<e.closesTick())return null;return e.votes().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);}
 public void close(UUID kingdom,long tick){Election e=elections.get(kingdom);if(e!=null)elections.put(kingdom,new Election(kingdom,e.openedTick(),e.closesTick(),e.votes(),true));}
}