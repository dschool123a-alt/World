package nl.worldmorph.sim;
import java.util.*;
public final class LawManager {
 public enum Law{FAIR_TAX,HIGH_TAX,FREE_MARKET,FOOD_RESERVE,OPEN_BORDERS,MILITARY_DRAFT,PUBLIC_WORKS,ROYAL_DECREE}
 private final Map<UUID,EnumSet<Law>> laws=new HashMap<>();
 public boolean enact(UUID kingdom,Law law){return laws.computeIfAbsent(kingdom,k->EnumSet.noneOf(Law.class)).add(law);}
 public boolean repeal(UUID kingdom,Law law){var set=laws.get(kingdom);return set!=null&&set.remove(law);}
 public boolean active(UUID kingdom,Law law){return laws.getOrDefault(kingdom,EnumSet.noneOf(Law.class)).contains(law);}
 public Set<Law> list(UUID kingdom){return Set.copyOf(laws.getOrDefault(kingdom,EnumSet.noneOf(Law.class)));}
}