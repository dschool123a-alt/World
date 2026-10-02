package nl.worldmorph.sim;
import java.util.*;
public final class EducationManager {
 public enum Skill{FARMING,BUILDING,TRADING,CRAFTING,LEADERSHIP,MEDICINE,ENGINEERING}
 private final Map<UUID,EnumMap<Skill,Integer>> skills=new HashMap<>();
 public int level(UUID npc,Skill skill){return skills.getOrDefault(npc,new EnumMap<>(Skill.class)).getOrDefault(skill,0);}
 public int train(UUID npc,Skill skill,int points){var m=skills.computeIfAbsent(npc,k->new EnumMap<>(Skill.class));int n=Math.max(0,Math.min(100,m.getOrDefault(skill,0)+Math.max(0,points)));m.put(skill,n);return n;}
 public Map<Skill,Integer> profile(UUID npc){EnumMap<Skill,Integer> m=new EnumMap<>(Skill.class);for(Skill s:Skill.values())m.put(s,level(npc,s));return Map.copyOf(m);}
}