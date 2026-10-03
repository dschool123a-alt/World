package nl.worldmorph.sim;
import java.util.*;
public final class DiplomacyAI {
 public enum Action{PROPOSE_TRADE,SEEK_ALLIANCE,REQUEST_PEACE,REMAIN_NEUTRAL,DECLARE_HOSTILE}
 public Action choose(int relation,int treasury,int stability,boolean atWar,boolean sharedThreat){if(atWar&&stability<25)return Action.REQUEST_PEACE;if(atWar)return Action.REMAIN_NEUTRAL;if(sharedThreat&&relation>20)return Action.SEEK_ALLIANCE;if(treasury<30&&relation>0)return Action.PROPOSE_TRADE;if(relation<-60)return Action.DECLARE_HOSTILE;return Action.REMAIN_NEUTRAL;}
}