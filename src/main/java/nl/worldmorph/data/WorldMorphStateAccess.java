package nl.worldmorph.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class WorldMorphStateAccess {
    private WorldMorphStateAccess() {}

    public static WorldMorphState get(ServerLevel level) {
        SavedData.Factory<WorldMorphState> factory =
            new SavedData.Factory<>(WorldMorphState::create, WorldMorphState::load, null);
        return level.getDataStorage().computeIfAbsent(factory, WorldMorphState.DATA_ID);
    }
}
