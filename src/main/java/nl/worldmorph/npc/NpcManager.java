package nl.worldmorph.npc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcManager {
    private final Map<UUID, NpcProfile> profiles = new LinkedHashMap<>();

    public NpcProfile create(String name) {
        NpcProfile profile = new NpcProfile(UUID.randomUUID(), name);
        profiles.put(profile.id(), profile);
        return profile;
    }

    public NpcProfile get(UUID id) {
        return profiles.get(id);
    }

    public int size() {
        return profiles.size();
    }
}
