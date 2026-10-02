package nl.worldmorph.sim;
import java.util.*; import nl.worldmorph.data.WorldMorphState;
public final class TradeManager {
 public record Trade(UUID from,UUID to,String good,int quantity,int unitPrice,long tick){}
 private final List<Trade> ledger=new ArrayList<>();
 public boolean trade(WorldMorphState s,UUID from,UUID to,String good,int quantity,int unitPrice){
  if(from.equals(to)||quantity<1||unitPrice<1||!s.kingdoms().containsKey(from)||!s.kingdoms().containsKey(to))return false;
  var seller=s.kingdoms().get(from);var buyer=s.kingdoms().get(to);long total=(long)quantity*unitPrice;
  if(total>Integer.MAX_VALUE||buyer.treasury()<total)return false;
  s.updateKingdom(buyer.withTreasury((int)(buyer.treasury()-total)));s.updateKingdom(seller.withTreasury((int)Math.min(Integer.MAX_VALUE,seller.treasury()+total)));
  ledger.add(new Trade(from,to,good,quantity,unitPrice,s.getSimulationTick()));if(ledger.size()>2000)ledger.remove(0);
  s.history("TRADE",quantity+" "+good+" traded from "+seller.name()+" to "+buyer.name());return true;
 }
 public List<Trade> ledger(){return List.copyOf(ledger);}
}