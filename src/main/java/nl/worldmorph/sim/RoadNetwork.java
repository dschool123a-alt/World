package nl.worldmorph.sim;
import java.util.*; import net.minecraft.core.BlockPos;
public final class RoadNetwork {
 public record Road(UUID id,UUID settlement,BlockPos start,BlockPos end,int quality){}
 private final Map<UUID,Road> roads=new LinkedHashMap<>();
 public Road build(UUID settlement,BlockPos start,BlockPos end,int quality){Road r=new Road(UUID.randomUUID(),settlement,start,end,Math.max(1,Math.min(100,quality)));roads.put(r.id(),r);return r;}
 public List<Road> inSettlement(UUID settlement){return roads.values().stream().filter(r->r.settlement().equals(settlement)).toList();}
 public int travelCost(BlockPos a,BlockPos b){return Math.max(1,(int)Math.ceil(Math.sqrt(a.distSqr(b))));}
}