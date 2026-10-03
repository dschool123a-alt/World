package nl.worldmorph.sim;
public final class SimulationBudget {
 public enum Tier{FULL,SETTLEMENT,STRATEGIC,DORMANT}
 public Tier tier(double distanceChunks,boolean playerNearby){if(playerNearby&&distanceChunks<=8)return Tier.FULL;if(distanceChunks<=16)return Tier.SETTLEMENT;if(distanceChunks<=64)return Tier.STRATEGIC;return Tier.DORMANT;}
 public int interval(Tier t){return switch(t){case FULL->1;case SETTLEMENT->20;case STRATEGIC->200;case DORMANT->1200;};}
 public boolean shouldTick(Tier t,long tick){return tick%interval(t)==0;}
}