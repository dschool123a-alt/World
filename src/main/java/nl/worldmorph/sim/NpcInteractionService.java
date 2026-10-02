package nl.worldmorph.sim;
import java.util.*; import nl.worldmorph.npc.NpcProfile;
public final class NpcInteractionService {
 public enum Choice{HELP,REFUSE}
 public record Request(UUID npc,UUID player,long createdTick,long expiresTick,boolean resolved){}
 public record Outcome(boolean accepted,String message){}
 private final Map<UUID,Request> requests=new HashMap<>();
 public Request ask(NpcProfile npc,UUID player,long tick){Request r=new Request(npc.id(),player,tick,tick+100,false);requests.put(npc.id(),r);npc.addMemory("ASKED_PLAYER_FOR_HELP",player,tick,2);return r;}
 public Outcome respond(NpcProfile npc,UUID player,Choice choice,long tick){
  Request r=requests.get(npc.id());if(r==null||r.resolved()||!r.player().equals(player)||tick>r.expiresTick())return new Outcome(false,"No active request from this NPC.");
  requests.put(npc.id(),new Request(r.npc(),r.player(),r.createdTick(),r.expiresTick(),true));
  if(choice==Choice.HELP){npc.changeLoyalty(20);npc.addMemory("PLAYER_HELPED",player,tick,8);npc.changeMoney(5);return new Outcome(true,npc.name()+" will remember your help.");}
  npc.changeLoyalty(-12);npc.addMemory("PLAYER_REFUSED",player,tick,5);return new Outcome(true,npc.name()+" will remember that you refused.");
 }
 public Optional<Request> active(UUID id){return Optional.ofNullable(requests.get(id)).filter(r->!r.resolved());}
}