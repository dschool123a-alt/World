package nl.worldmorph.sim;
import java.util.*;
public final class QuestManager {
 public enum Status{AVAILABLE,ACCEPTED,COMPLETED,FAILED}
 public record Quest(UUID id,UUID giver,String title,String objective,int reward,Status status,long createdTick){}
 private final Map<UUID,Quest> quests=new LinkedHashMap<>();
 public Quest offer(UUID giver,String title,String objective,int reward,long tick){Quest q=new Quest(UUID.randomUUID(),giver,title,objective,Math.max(0,reward),Status.AVAILABLE,tick);quests.put(q.id(),q);return q;}
 public Quest accept(UUID id){Quest q=quests.get(id);if(q==null||q.status()!=Status.AVAILABLE)return null;return put(q,Status.ACCEPTED);}
 public Quest complete(UUID id){Quest q=quests.get(id);if(q==null||q.status()!=Status.ACCEPTED)return null;return put(q,Status.COMPLETED);}
 public Quest fail(UUID id){Quest q=quests.get(id);if(q==null||q.status()==Status.COMPLETED)return null;return put(q,Status.FAILED);}
 private Quest put(Quest q,Status s){Quest n=new Quest(q.id(),q.giver(),q.title(),q.objective(),q.reward(),s,q.createdTick());quests.put(q.id(),n);return n;}
 public Collection<Quest> all(){return List.copyOf(quests.values());}
}