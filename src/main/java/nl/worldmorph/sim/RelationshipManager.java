package nl.worldmorph.sim;
import java.util.*; import nl.worldmorph.npc.NpcProfile;
public final class RelationshipManager {
 public int score(NpcProfile a,NpcProfile b){return a.relationships().getOrDefault(b.id(),0);}
 public int change(NpcProfile a,NpcProfile b,int delta){int v=Math.max(-100,Math.min(100,score(a,b)+delta));a.setRelationship(b.id(),v);return v;}
 public boolean friends(NpcProfile a,NpcProfile b){return score(a,b)>=50&&score(b,a)>=50;}
 public boolean enemies(NpcProfile a,NpcProfile b){return score(a,b)<=-50||score(b,a)<=-50;}
}