package nl.worldmorph.sim;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.phys.AABB;
import nl.worldmorph.data.WorldMorphState;

public final class DayOneArrivalManager {
    private boolean arrived;
    private final Map<UUID, UUID> followers = new HashMap<>();

    public void tick(ServerLevel level, WorldMorphState state) {
        if (!arrived && state.getSimulationTick() <= 24000) spawnIfReady(level, state);
        for (var entry : followers.entrySet()) {
            var entity = level.getEntity(entry.getKey());
            var player = level.getServer().getPlayerList().getPlayer(entry.getValue());
            if (entity instanceof Villager villager && player != null && villager.isAlive()) {
                villager.getNavigation().moveTo(player, 1.05D);
            }
        }
    }

    private void spawnIfReady(ServerLevel level, WorldMorphState state) {
        long time = level.getDayTime() % 24000L;
        if (time < 1000L || time > 12000L) return;
        ServerPlayer player = level.players().stream().filter(p -> !p.isSpectator()).findFirst().orElse(null);
        if (player == null || player.blockPosition().getY() < level.getSeaLevel() - 8) return;
        if (!level.canSeeSky(player.blockPosition())) return;

        var pos = player.blockPosition().offset(6, 0, 6);
        if (!level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(12)).isEmpty()) {
            arrived = true;
            return;
        }

        String[] names = {"Settler", "Builder", "Farmer"};
        for (int i = 0; i < 3; i++) {
            Villager villager = new Villager(level);
            villager.moveTo(pos.getX() + i * 1.5, pos.getY() + 1, pos.getZ(), 0, 0);
            villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
            villager.setCustomName(net.minecraft.network.chat.Component.literal(names[i]));
            villager.setCustomNameVisible(true);
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(villager);
        }
        state.history("DAY_ONE_ARRIVAL", "Three settlers arrived near the player on day one.");
        arrived = true;
    }

    public boolean follow(ServerLevel level, ServerPlayer player, UUID npcId) {
        var entity = level.getEntity(npcId);
        if (!(entity instanceof Villager villager) || !villager.isAlive()) return false;
        followers.put(npcId, player.getUUID());
        villager.getNavigation().moveTo(player, 1.05D);
        return true;
    }

    public boolean wait(UUID npcId) {
        return followers.remove(npcId) != null;
    }

    public boolean build(ServerLevel level, WorldMorphState state, ServerPlayer player, UUID npcId) {
        var entity = level.getEntity(npcId);
        if (!(entity instanceof Villager villager) || !villager.isAlive()) return false;
        followers.remove(npcId);
        var kingdom = state.kingdoms().values().stream()
            .filter(k -> k.leader().equals(player.getUUID()))
            .findFirst().orElseGet(() -> state.createKingdom(player.getName().getString() + "'s Realm", player.getUUID()));
        var center = player.blockPosition();
        var existing = state.settlements().values().stream()
            .filter(s -> s.center().distSqr(center) < 64 * 64)
            .findFirst();
        if (existing.isPresent()) return true;
        var settlement = state.createSettlement("New Settlement", center, kingdom.id());
        state.updateSettlement(settlement.withPopulation(3));
        state.history("DAY_ONE_SETTLEMENT", "The first settlers founded New Settlement.");
        villager.getNavigation().stop();
        return true;
    }
}
