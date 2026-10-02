package nl.worldmorph.sim;
import java.util.*; import net.minecraft.core.BlockPos;
public final class TerritoryManager {
 public record Claim(UUID kingdom,BlockPos center,int radius,long claimedTick){}
 private final Map<String,Claim> claims=new LinkedHashMap<>();
 private String key(BlockPos p){return p.getX()+":"+p.getZ();}
 public boolean claim(UUID kingdom,BlockPos center,int radius,long tick){if(radius<1||radius>512)return false;String k=key(center);Claim old=claims.get(k);if(old!=null&&!old.kingdom().equals(kingdom))return false;claims.put(k,new Claim(kingdom,center,radius,tick));return true;}
 public UUID ownerAt(BlockPos pos){return claims.values().stream().filter(c->Math.abs(pos.getX()-c.center().getX())<=c.radius()&&Math.abs(pos.getZ()-c.center().getZ())<=c.radius()).min(Comparator.comparingLong(c->pos.distToCenterSqr(c.center().getX(),pos.getY(),c.center().getZ()))).map(Claim::kingdom).orElse(null);}
 public Collection<Claim> claims(){return List.copyOf(claims.values());}
}