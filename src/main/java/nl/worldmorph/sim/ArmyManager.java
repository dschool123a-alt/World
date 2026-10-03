package nl.worldmorph.sim;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.npc.NpcProfile;

/** Kingdom army roster plus simple server-side marching orders. */
public final class ArmyManager {
    public record Army(UUID kingdomId, List<UUID> soldiers, int training, String formation,
                       int targetX, int targetY, int targetZ, boolean marching) {
        public Army { soldiers = List.copyOf(soldiers); }
    }

    private static final int NO_TARGET = Integer.MIN_VALUE;
    private final Map<UUID, LinkedHashSet<UUID>> soldiersByKingdom = new LinkedHashMap<>();
    private final Map<UUID, Integer> trainingByKingdom = new LinkedHashMap<>();
    private final Map<UUID, String> formationByKingdom = new LinkedHashMap<>();
    private final Map<UUID, Target> targetByKingdom = new LinkedHashMap<>();
    private record Target(int x, int y, int z) {}

    public boolean recruit(WorldMorphState state, UUID kingdomId, NpcProfile npc) {
        if (!state.kingdoms().containsKey(kingdomId) || !npc.alive()) return false;
        if (!state.spendTreasury(kingdomId, 20, "army recruitment")) return false;
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

    public String setFormation(UUID kingdomId, String formation) {
        String normalized = formation.toUpperCase(Locale.ROOT);
        if (!Set.of("LINE", "COLUMN", "WEDGE").contains(normalized)) return null;
        formationByKingdom.put(kingdomId, normalized);
        return normalized;
    }

    public String formation(UUID kingdomId) { return formationByKingdom.getOrDefault(kingdomId, "LINE"); }

    public boolean march(UUID kingdomId, int x, int y, int z) {
        if (soldiersByKingdom.getOrDefault(kingdomId, new LinkedHashSet<>()).isEmpty()) return false;
        targetByKingdom.put(kingdomId, new Target(x, y, z));
        return true;
    }

    public void halt(UUID kingdomId) { targetByKingdom.remove(kingdomId); }
    public boolean isMarching(UUID kingdomId) { return targetByKingdom.containsKey(kingdomId); }

    public Army get(UUID kingdomId) {
        Target target = targetByKingdom.get(kingdomId);
        return new Army(kingdomId,
            new ArrayList<>(soldiersByKingdom.getOrDefault(kingdomId, new LinkedHashSet<>())),
            trainingByKingdom.getOrDefault(kingdomId, 0), formation(kingdomId),
            target == null ? NO_TARGET : target.x(), target == null ? NO_TARGET : target.y(),
            target == null ? NO_TARGET : target.z(), target != null);
    }

    public int armyCount() { return soldiersByKingdom.size(); }

    /** Gives spawned villager soldiers real pathfinding orders. */
    public void tick(ServerLevel level) {
        for (var entry : targetByKingdom.entrySet()) {
            Target target = entry.getValue();
            var soldiers = soldiersByKingdom.get(entry.getKey());
            if (soldiers == null) continue;
            for (UUID id : soldiers) {
                Entity entity = level.getEntity(id);
                if (!(entity instanceof PathfinderMob mob) || !mob.isAlive()) continue;
                double dx = target.x() - mob.getX();
                double dz = target.z() - mob.getZ();
                if (dx * dx + dz * dz < 9.0) continue;
                mob.getNavigation().moveTo(target.x() + 0.5, target.y(), target.z() + 0.5, 1.0);
            }
        }
    }
}
