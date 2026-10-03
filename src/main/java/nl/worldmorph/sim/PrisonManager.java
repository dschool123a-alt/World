package nl.worldmorph.sim;
import java.util.*;
public final class PrisonManager {
 public record Sentence(UUID prisoner,UUID kingdom,int durationDays,int daysServed){}
 private final Map<UUID,Sentence> sentences=new HashMap<>();
 public Sentence sentence(UUID prisoner,UUID kingdom,int days){if(days<1)return null;Sentence s=new Sentence(prisoner,kingdom,days,0);sentences.put(prisoner,s);return s;}
 public boolean imprisoned(UUID npc){return sentences.containsKey(npc);}
 public void dayPasses(){for(var e:new ArrayList<>(sentences.entrySet())){Sentence s=e.getValue();int served=s.daysServed()+1;if(served>=s.durationDays())sentences.remove(e.getKey());else sentences.put(e.getKey(),new Sentence(s.prisoner(),s.kingdom(),s.durationDays(),served));}}
 public Collection<Sentence> active(){return List.copyOf(sentences.values());}
}