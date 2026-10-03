package nl.worldmorph.sim;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.npc.NpcManager;
import nl.worldmorph.npc.NpcProfile;

/**
 * Keeps early settlements safe while they are learning to mine/build.
 * Once a settlement reaches 50 population, normal hostile/environmental damage applies.
 */
public final class WorkerSafetyManager {
    private final NpcManager npcs;
    private WorldMorphState state;
    private static boolean installed;

    public WorkerSafetyManager(NpcManager npcs) {
        this.npcs = npcs;
        install();
    }

    public void tick(WorldMorphState state) {
        this.state = state;
    }

    private void install() {
        if (installed) return;
        installed = true;

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!protectedWorker(entity)) return true;
            // Players can still intentionally attack a citizen; the protection is for
            // mining/building hazards and hostile mobs.
            if (source.getEntity() instanceof Player) return true;
            return false;
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!protectedWorker(entity)) return true;
            if (source.getEntity() instanceof Player) return true;
            if (entity.getHealth() <= 0.0F) entity.setHealth(entity.getMaxHealth());
            return false;
        });
    }

    private boolean protectedWorker(LivingEntity entity) {
        if (state == null || !entity.isAlive()) return false;
        NpcProfile profile = npcs.get(entity.getUUID());
        if (profile == null || !profile.alive()) return false;
        if (!"MINER".equals(profile.job()) && !"BUILDER".equals(profile.job())) return false;
        if (profile.settlementId() == null) return false;
        WorldMorphState.SettlementData settlement = state.settlements().get(profile.settlementId());
        return settlement != null && settlement.population() < 50;
    }
}
