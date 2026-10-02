package nl.worldmorph.sim;
public final class ImmigrationManager {
 public int arrivals(int housingVacancies,int foodSecurity,int safety,int tradeAccess){if(housingVacancies<=0||foodSecurity<20||safety<20)return 0;int score=foodSecurity+safety+tradeAccess;return Math.min(housingVacancies,Math.max(0,score/100));}
 public int departures(int foodSecurity,int safety,int loyalty){int risk=(100-foodSecurity)+(100-safety)+(100-loyalty);return Math.max(0,risk/75);}
}