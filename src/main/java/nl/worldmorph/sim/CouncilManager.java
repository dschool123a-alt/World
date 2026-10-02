package nl.worldmorph.sim;
import java.util.*;
public final class CouncilManager {
 public record Council(UUID kingdom,Set<UUID> members,int approvalThreshold){public Council{members=Set.copyOf(members);}}
 private final Map<UUID,Council> councils=new HashMap<>();
 public Council appoint(UUID kingdom,Collection<UUID> members,int threshold){Council c=new Council(kingdom,new HashSet<>(members),Math.max(1,Math.min(100,threshold)));councils.put(kingdom,c);return c;}
 public boolean canApprove(UUID kingdom,Map<UUID,Boolean> votes){Council c=councils.get(kingdom);if(c==null||c.members().isEmpty())return false;long yes=votes.entrySet().stream().filter(e->c.members().contains(e.getKey())&&Boolean.TRUE.equals(e.getValue())).count();return yes*100/c.members().size()>=c.approvalThreshold();}
 public Council get(UUID kingdom){return councils.get(kingdom);}
}