package nl.worldmorph.sim;
import java.util.*;
public final class AchievementManager {
 public enum Achievement{FIRST_HOUSE,FIRST_FAMILY,FIRST_VILLAGE,FIRST_TRADE,FIRST_ALLIANCE,FIRST_CITY,FIRST_ELECTION,FIRST_PEACE_TREATY,ONE_HUNDRED_CITIZENS,THOUSAND_CITIZENS}
 private final Map<UUID,EnumSet<Achievement>> unlocked=new HashMap<>();
 public boolean unlock(UUID player,Achievement achievement){return unlocked.computeIfAbsent(player,k->EnumSet.noneOf(Achievement.class)).add(achievement);}
 public boolean has(UUID player,Achievement achievement){return unlocked.getOrDefault(player,EnumSet.noneOf(Achievement.class)).contains(achievement);}
 public Set<Achievement> list(UUID player){return Set.copyOf(unlocked.getOrDefault(player,EnumSet.noneOf(Achievement.class)));}
}