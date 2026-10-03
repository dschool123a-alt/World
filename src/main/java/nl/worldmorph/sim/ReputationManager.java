package nl.worldmorph.sim;
import java.util.*;
public final class ReputationManager {
 public enum Group{FARMERS,SOLDIERS,BUILDERS,MERCHANTS,NOBLES,FAMILIES,SCHOLARS}
 private final Map<UUID,EnumMap<Group,Integer>> scores=new HashMap<>();
 public int get(UUID ruler,Group g){return scores.getOrDefault(ruler,new EnumMap<>(Group.class)).getOrDefault(g,50);}
 public int change(UUID ruler,Group g,int d){var m=scores.computeIfAbsent(ruler,k->new EnumMap<>(Group.class));int n=Math.max(0,Math.min(100,m.getOrDefault(g,50)+d));m.put(g,n);return n;}
 public Map<Group,Integer> summary(UUID ruler){EnumMap<Group,Integer> m=new EnumMap<>(Group.class);for(Group g:Group.values())m.put(g,get(ruler,g));return Map.copyOf(m);}
}