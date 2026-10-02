package nl.worldmorph.sim;
import java.util.*;
import net.minecraft.core.BlockPos;
public final class ConstructionPlanner {
 public enum Building{HOUSE,FARM,MARKET,BLACKSMITH,GUARD_POST,WELL,ROAD,HALL,WALL,CASTLE}
 public record Project(UUID id,UUID settlement,Building building,BlockPos site,int workRequired,int workDone,boolean complete){}
 private final Map<UUID,Project> projects=new LinkedHashMap<>();
 public Project plan(UUID settlement,Building building,BlockPos site){Project p=new Project(UUID.randomUUID(),settlement,building,site,cost(building),0,false);projects.put(p.id(),p);return p;}
 public Project work(UUID id,int amount){Project p=projects.get(id);if(p==null||p.complete()||amount<1)return p;int done=Math.min(p.workRequired(),p.workDone()+amount);Project n=new Project(p.id(),p.settlement(),p.building(),p.site(),p.workRequired(),done,done>=p.workRequired());projects.put(id,n);return n;}
 private int cost(Building b){return switch(b){case HOUSE->20;case FARM,WELL,GUARD_POST->30;case MARKET,BLACKSMITH,ROAD->50;case HALL->100;case WALL->120;case CASTLE->250;};}
 public Collection<Project> projects(){return List.copyOf(projects.values());}
}