package nl.worldmorph.data;
import net.minecraft.server.level.ServerLevel;
public final class NeedsPersistentStateAccess {
 private NeedsPersistentStateAccess(){}
 public static NeedsPersistentState get(ServerLevel level){return level.getDataStorage().computeIfAbsent(NeedsPersistentState.TYPE);}
}