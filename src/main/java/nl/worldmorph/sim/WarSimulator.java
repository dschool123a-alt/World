package nl.worldmorph.sim;
import java.util.*;
public final class WarSimulator {
 public record Result(UUID winner,UUID loser,int attackerLosses,int defenderLosses,int duration){}
 public Result resolve(DiplomacyManager.War war,MilitaryManager.Army a,MilitaryManager.Army d,long seed){
  if(a==null||d==null)return null;long atk=Math.max(1,a.strength())*(long)Math.max(10,a.morale());long def=Math.max(1,d.strength())*(long)Math.max(10,d.morale());long roll=Math.floorMod(seed,1000);boolean win=atk+roll>=def+(999-roll);
  int al=(int)Math.min(Integer.MAX_VALUE,Math.max(0,a.strength()*(win?1:2)/10));int dl=(int)Math.min(Integer.MAX_VALUE,Math.max(0,d.strength()*(win?2:1)/10));
  return new Result(win?war.attacker():war.defender(),win?war.defender():war.attacker(),al,dl,1+(int)Math.floorMod(seed,7));
 }
}