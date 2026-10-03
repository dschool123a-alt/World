package nl.worldmorph.sim;
import java.util.*;
public final class CulturalIdentityManager {
 public record Culture(UUID id,String name,Map<String,Integer> traits){public Culture{traits=Map.copyOf(traits);}}
 private final Map<UUID,Culture> cultures=new LinkedHashMap<>();
 public Culture create(String name){Culture c=new Culture(UUID.randomUUID(),name,Map.of("tradition",50,"openness",50,"craftsmanship",50));cultures.put(c.id(),c);return c;}
 public Culture change(UUID id,String trait,int delta){Culture c=cultures.get(id);if(c==null)return null;Map<String,Integer> m=new HashMap<>(c.traits());m.put(trait,Math.max(0,Math.min(100,m.getOrDefault(trait,50)+delta)));Culture n=new Culture(c.id(),c.name(),m);cultures.put(id,n);return n;}
 public Culture get(UUID id){return cultures.get(id);}
}