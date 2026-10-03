package nl.worldmorph.sim;
import java.util.*;
public final class TreatyManager {
 public enum Type{NON_AGGRESSION,TRADE,ALLIANCE,DEFENSIVE_PACT,PEACE}
 public record Treaty(UUID id,UUID first,UUID second,Type type,long signedTick,long expiryTick,boolean active){}
 private final Map<UUID,Treaty> treaties=new LinkedHashMap<>();
 public Treaty sign(UUID a,UUID b,Type type,long tick,long duration){if(a.equals(b))return null;for(Treaty t:treaties.values())if(t.active()&&t.first().equals(a)&&t.second().equals(b)&&t.type()==type)return null;Treaty t=new Treaty(UUID.randomUUID(),a,b,type,tick,tick+Math.max(1,duration),true);treaties.put(t.id(),t);return t;}
 public boolean protectedFromWar(UUID a,UUID b,long tick){return treaties.values().stream().anyMatch(t->t.active()&&t.type()==Type.NON_AGGRESSION&&t.expiryTick()>tick&&((t.first().equals(a)&&t.second().equals(b))||(t.first().equals(b)&&t.second().equals(a))));}
 public void expire(long tick){treaties.replaceAll((id,t)->t.active()&&t.expiryTick()<=tick?new Treaty(t.id(),t.first(),t.second(),t.type(),t.signedTick(),t.expiryTick(),false):t);}
 public Collection<Treaty> all(){return List.copyOf(treaties.values());}
}