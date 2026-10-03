package nl.worldmorph.sim;

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

    public void tick(ServerLevel level, WorldMorphState state) {
        if (arrived || state.getSimulationTick() > 24000) return;

        // Only trigger during daytime and when a player is actually near the surface.
        long time = level.getDayTime() % 24000L;
        if (time < 1000L || time > 12000L) return;

        ServerPlayer player = level.players().stream()
            .filter(p -> !p.isSpectator())
            .filter(p -> p.blockPosition().getY() >= level.getMinBuildHeight() + 20)
            .findFirst().orElse(null);
        if (player == null) return;

        // Do not interrupt mining/exploration underground.
        if (player.blockPosition().getY() < level.getSeaLevel() - 8) return;

        var pos = player.blockPosition().offset(6, 0, 6);
        if (!level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(12)).isEmpty()) {
            arrived = true;
            return;
        }

        for (int i = 0; i < 3; i++) {
            Villager villager = new Villager(level);
            villager.moveTo(pos.getX() + i * 1.5, pos.getY() + 1, pos.getZ(), 0, 0);
            villager.setVillagerData(new VillagerData(
                VillagerType.PLAINS, VillagerProfession.NONE, 1));
            villager.setCustomName(net.minecraft.network.chat.Component.literal(
                i == 0 ? "Settler" : i == 1 ? "Builder" : "Farmer"));
            villager.setCustomNameVisible(true);
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
                EntitySpawnReason.EVENT, null);
            level.addFreshEntity(villager);
        }

        state.history("DAY_ONE_ARRIVAL", "Three settlers arrived near the player on day one.");
        arrived = true;
    }
}
