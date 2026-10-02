package nl.worldmorph.sim;
import nl.worldmorph.data.WorldMorphState;
public final class RebellionManager {
 public boolean shouldRebel(int stability,int foodNeed,int taxRate,int loyalty){return stability<25||foodNeed<15||taxRate>60||loyalty<20;}
 public boolean resolve(WorldMorphState s,java.util.UUID kingdom){var k=s.kingdoms().get(kingdom);if(k==null||k.stability()>20)return false;int next=Math.max(0,k.stability()-10);s.updateKingdom(k.withStability(next));s.history("REBELLION","Unrest grows in "+k.name());return true;}
}