package nl.worldmorph.sim;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
public final class NpcBubbleManager {
 private record Bubble(UUID entity,String originalName,boolean wasVisible,long expiresTick){}
 private final Map<UUID,Bubble> active=new HashMap<>();
 public void show(Entity entity,long tick,String message,int durationTicks){String name=entity.getCustomName()==null?null:entity.getCustomName().getString();boolean visible=entity.isCustomNameVisible();active.put(entity.getUUID(),new Bubble(entity.getUUID(),name,visible,tick+Math.max(1,durationTicks)));entity.setCustomName(Component.literal(message));entity.setCustomNameVisible(true);}
 public void tick(ServerLevel level,long tick){Iterator<Bubble> it=active.values().iterator();while(it.hasNext()){Bubble b=it.next();if(tick<b.expiresTick())continue;Entity e=level.getEntity(b.entity());if(e!=null){e.setCustomName(b.originalName()==null?null:Component.literal(b.originalName()));e.setCustomNameVisible(b.wasVisible());}it.remove();}}
 public int activeCount(){return active.size();}
}