package nl.worldmorph.sim;
import java.util.*;
public final class TechnologyManager {
 public enum Tech{FARMING,MASONRY,IRONWORKING,WRITING,CURRENCY,ENGINEERING,NAVIGATION}
 private final Map<UUID,EnumSet<Tech>> unlocked=new HashMap<>();
 public boolean unlock(UUID kingdom,Tech tech){return unlocked.computeIfAbsent(kingdom,k->EnumSet.noneOf(Tech.class)).add(tech);}
 public boolean has(UUID kingdom,Tech tech){return unlocked.getOrDefault(kingdom,EnumSet.noneOf(Tech.class)).contains(tech);}
 public Set<Tech> known(UUID kingdom){return Set.copyOf(unlocked.getOrDefault(kingdom,EnumSet.noneOf(Tech.class)));}
}