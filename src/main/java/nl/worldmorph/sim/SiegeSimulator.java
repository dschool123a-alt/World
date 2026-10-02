package nl.worldmorph.sim;
public final class SiegeSimulator {
 public record SiegeState(int wallIntegrity,int supplyDays,int morale,boolean breached){}
 public SiegeState tick(SiegeState s,int pressure,int foodDelivered){int wall=Math.max(0,s.wallIntegrity()-Math.max(0,pressure));int supply=Math.max(0,s.supplyDays()-(foodDelivered>0?0:1));int morale=Math.max(0,Math.min(100,s.morale()+(foodDelivered>0?2:-4)));return new SiegeState(wall,supply,morale,wall==0);}
 public boolean shouldSurrender(SiegeState s){return s.breached()||s.supplyDays()==0||s.morale()<15;}
}