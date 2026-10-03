package nl.worldmorph.sim;

import java.util.*;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.npc.NpcProfile;

/** Tracks kingdom armies made from WorldMorph citizen profiles. */
public final class ArmyManager {
    public record Army(UUID kingdomId, List<UUID> soldiers, int training) {
        public Army { soldiers = List.copyOf(soldiers); }
    }
    private final Map<UUID, LinkedHashSet<UUID>> soldiersByKingdom = new LinkedHashMap<>();
    private final Map<UUID, Integer> trainingByKingdom = new LinkedHashMap<>();

    public boolean recruit(WorldMorphState state, UUID kingdomId, NpcProfile npc) {
        if (!state.kingdoms().containsKey(kingdomId) || !npc.alive()) return false;
        LinkedHashSet<UUID> soldiers = soldiersByKingdom.computeIfAbsent(kingdomId, ignored -> new LinkedHashSet<>());
        if (!soldiers.add(npc.id())) return false;
        npc.setKingdom(kingdomId);
        npc.setJob("SOLDIER");
        return true;
    }

    public boolean discharge(UUID kingdomId, UUID npcId, NpcProfile npc) {
        LinkedHashSet<UUID> soldiers = soldiersByKingdom.get(kingdomId);
        if (soldiers == null || !soldiers.remove(npcId)) return false;
        if (npc != null && "SOLDIER".equals(npc.job())) npc.setJob("UNEMPLOYED");
        return true;
    }

    public int train(UUID kingdomId, int points) {
        int value = Math.max(0, Math.min(100, trainingByKingdom.getOrDefault(kingdomId, 0) + points));
        trainingByKingdom.put(kingdomId, value);
        return value;
    }

    public Army get(UUID kingdomId) {
        return new Army(kingdomId, new ArrayList<>(soldiersByKingdom.getOrDefault(kingdomId, new LinkedHashSet<>())),
            trainingByKingdom.getOrDefault(kingdomId, 0));
    }

    public int armyCount() { return soldiersByKingdom.size(); }
}
