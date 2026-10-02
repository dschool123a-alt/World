package nl.worldmorph.sim;
import nl.worldmorph.npc.*;
public final class PersonalityEngine {
 public int loyaltyChange(NpcProfile p,String event){int n=switch(event){case "HELPED"->p.personality()==NpcPersonality.KIND?15:8;case "PROMISE_DELIVERED"->12;case "PROMISE_BROKEN"->-20;case "TAX_INCREASE"->-8;case "FAMILY_PROTECTED"->20;case "ATTACKED"->-35;default->0;};p.changeLoyalty(n);return n;}
 public boolean mayLeave(NpcProfile p){return p.loyalty()<15||!p.alive();}
 public boolean mayChallengeRuler(NpcProfile p){return p.alive()&&p.ambition()>75&&p.loyalty()<30;}
}