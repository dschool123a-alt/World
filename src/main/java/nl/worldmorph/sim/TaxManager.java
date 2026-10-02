package nl.worldmorph.sim;
import nl.worldmorph.data.WorldMorphState;
public final class TaxManager {
 public int setRate(WorldMorphState s,java.util.UUID kingdom,int rate){var k=s.kingdoms().get(kingdom);if(k==null)return -1;int safe=Math.max(0,Math.min(75,rate));s.updateKingdom(new WorldMorphState.KingdomData(k.id(),k.name(),k.leader(),k.government(),k.treasury(),k.stability(),safe));return safe;}
 public int collect(WorldMorphState s,java.util.UUID kingdom,int taxableCitizens){var k=s.kingdoms().get(kingdom);if(k==null||taxableCitizens<0)return 0;long income=(long)taxableCitizens*k.taxRate();s.updateKingdom(k.withTreasury((int)Math.min(Integer.MAX_VALUE,(long)k.treasury()+income)));if(k.taxRate()>40)s.updateKingdom(s.kingdoms().get(kingdom).withStability(Math.max(0,k.stability()-Math.max(1,k.taxRate()/20))));return (int)Math.min(Integer.MAX_VALUE,income);}
}