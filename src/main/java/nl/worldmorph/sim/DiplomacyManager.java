package nl.worldmorph.sim;
import java.util.HashMap; import java.util.Map; import java.util.UUID; import nl.worldmorph.data.WorldMorphState;
public final class DiplomacyManager {
 public enum Relation { ALLIED, FRIENDLY, NEUTRAL, TENSE, HOSTILE, AT_WAR }
 public record War(UUID attacker, UUID defender, WarGoal goal, long startedTick) {}
 private final Map<String,Relation> relations=new HashMap<>(); private final Map<String,War>wars=new HashMap<>();
 private static String key(UUID a,UUID b){return a.compareTo(b)<0?a+":"+b:b+":"+a;}
 public Relation relation(UUID a,UUID b){return relations.getOrDefault(key(a,b),Relation.NEUTRAL);}
 public boolean declareWar(WorldMorphState s,UUID a,UUID d,WarGoal g){if(a.equals(d)||!s.kingdoms().containsKey(a)||!s.kingdoms().containsKey(d)||wars.containsKey(key(a,d)))return false; wars.put(key(a,d),new War(a,d,g,s.getSimulationTick()));relations.put(key(a,d),Relation.AT_WAR);s.history("WAR_DECLARED",s.kingdoms().get(a).name()+" declared war on "+s.kingdoms().get(d).name()+" ("+g+")");return true;}
 public boolean atWar(UUID a,UUID b){return wars.containsKey(key(a,b));} public Map<String,War>wars(){return Map.copyOf(wars);}
}