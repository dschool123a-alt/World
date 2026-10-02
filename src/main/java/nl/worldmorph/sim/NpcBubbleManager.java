package nl.worldmorph.sim;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
public final class NpcBubbleManager {
 private record Bubble(UUID entity,String originalName,boolean wasVisible,long expiresTick){}
 private final Map<UUID,Bubble> active=new HashMap<>();
 public void show(Villager villager,long tick,String message,int durationTicks){String name=villager.getCustomName()==null?null:villager.getCustomName().getString();boolean visible=villager.isCustomNameVisible();Bubble b=new Bubble(villager.getUUID(),name,visible,tick+Math.max(1,durationTicks));active.put(villager.getUUID(),b);villager.setCustomName(Component.literal(message));villager.setCustomNameVisible(true);}
 public void tick(ServerLevel level,long tick){Iterator<Bubble> it=active.values().iterator();while(it.hasNext()){Bubble b=it.next();if(tick<b.expiresTick())continue;Entity e=level.getEntity(b.entity());if(e instanceof Villager v){v.setCustomName(b.originalName()==null?null:Component.literal(b.originalName()));v.setCustomNameVisible(b.wasVisible());}it.remove();}}
 public int activeCount(){return active.size();}
}