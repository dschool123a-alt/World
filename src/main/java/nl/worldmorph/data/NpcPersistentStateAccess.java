package nl.worldmorph.data;
import net.minecraft.server.level.ServerLevel;
public final class NpcPersistentStateAccess {
 private NpcPersistentStateAccess(){}
 public static NpcPersistentState get(ServerLevel level){return level.getDataStorage().computeIfAbsent(NpcPersistentState.TYPE);}
}