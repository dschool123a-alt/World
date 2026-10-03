package nl.worldmorph.data;
import net.minecraft.server.level.ServerLevel;
public final class HomePersistentStateAccess {
 private HomePersistentStateAccess(){}
 public static HomePersistentState get(ServerLevel level){return level.getDataStorage().computeIfAbsent(HomePersistentState.TYPE);}
}