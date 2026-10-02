package nl.worldmorph.data;

import net.minecraft.server.level.ServerLevel;

public final class WorldMorphStateAccess {
    private WorldMorphStateAccess() {}

    public static WorldMorphState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WorldMorphState.TYPE);
    }
}
