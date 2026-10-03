package nl.worldmorph.sim;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.npc.NpcManager;
import nl.worldmorph.npc.NpcProfile;

/**
 * Royal administration unlocked at 100 citizens.
 * Orders are deliberately delayed: the assistant sends a physical mounted messenger.
 */
public final class RoyalAdministrationManager {
    private static final int UNLOCK_POPULATION = 100;
    private static final long MIN_ASSIGNMENT = 6000; // 5 Minecraft minutes
    private final Map<UUID, UUID> assistants = new HashMap<>();
    private final Map<UUID, Messenger> messengers = new HashMap<>();
    private final Map<UUID, String> pendingOrders = new HashMap<>();

    private record Messenger(UUID rider, UUID horse, UUID kingdom, UUID targetKingdom,
                             BlockPos target, BlockPos returnPos, long arriveTick,
                             String type, String reason) {}

    public void tick(ServerLevel level, WorldMorphState state, NpcManager npcs, RoadNetwork roads) {
        long tick = state.getSimulationTick();
        for (WorldMorphState.KingdomData kingdom : List.copyOf(state.kingdoms().values())) {
            if (population(state, kingdom.id()) < UNLOCK_POPULATION) continue;
            ensureAssistant(level, state, npcs, kingdom);
            followLeader(level, kingdom);
        }
        for (Messenger m : List.copyOf(messengers.values())) {
            Entity rider = level.getEntity(m.rider());
            Entity horse = level.getEntity(m.horse());
            if (rider == null || horse == null) {
                messengers.remove(m.rider());
                continue;
            }
            if (tick < m.arriveTick()) {
                moveMounted(level, rider, horse, m.target(), 0.7);
                continue;
            }
            if (rider.blockPosition().closerThan(m.target(), 4)) {
                if (m.returnPos().closerThan(rider.blockPosition(), 4)) {
                    finishReturn(level, state, rider, horse, m);
                    continue;
                }
                deliver(level, state, rider, m);
                long returnTicks = Math.max(MIN_ASSIGNMENT / 2, roads.travelTicks(m.target(), m.returnPos(), true));
                messengers.put(m.rider(), new Messenger(m.rider(), m.horse(), m.kingdom(), m.targetKingdom(),
                        m.returnPos(), m.returnPos(), tick + returnTicks, "RETURN", m.reason()));
            } else {
                moveMounted(level, rider, horse, m.target(), 0.7);
            }
        }
    }

    public boolean unlocked(WorldMorphState state, UUID kingdom) {
        return population(state, kingdom) >= UNLOCK_POPULATION;
    }

    public String status(UUID kingdom) {
        return pendingOrders.getOrDefault(kingdom, "Royal assistant is available.");
    }

    public boolean orderWar(ServerLevel level, WorldMorphState state, NpcManager npcs,
                            RoadNetwork roads, UUID leader, String targetName, String reason) {
        WorldMorphState.KingdomData own = state.kingdoms().values().stream()
                .filter(k -> k.leader().equals(leader)).findFirst().orElse(null);
        WorldMorphState.KingdomData target = state.kingdoms().values().stream()
                .filter(k -> k.name().equalsIgnoreCase(targetName)).findFirst().orElse(null);
        if (own == null || target == null || own.id().equals(target.id()) || !unlocked(state, own.id())) return false;
        if (pendingOrders.containsKey(own.id())) return false;
        WorldMorphState.SettlementData destination = capital(state, target.id());
        if (destination == null) return false;

        pendingOrders.put(own.id(), "WAR: " + target.name());
        spawnMessenger(level, state, npcs, roads, own, target, destination, "WAR", reason == null ? "Unknown" : reason);
        state.history("ROYAL_ORDER", own.name() + " ordered a war mission against " + target.name() + ": " + reason);
        return true;
    }

    public boolean orderPeace(ServerLevel level, WorldMorphState state, NpcManager npcs,
                              RoadNetwork roads, UUID leader, String targetName, String reason) {
        WorldMorphState.KingdomData own = state.kingdoms().values().stream()
                .filter(k -> k.leader().equals(leader)).findFirst().orElse(null);
        WorldMorphState.KingdomData target = state.kingdoms().values().stream()
                .filter(k -> k.name().equalsIgnoreCase(targetName)).findFirst().orElse(null);
        if (own == null || target == null || !unlocked(state, own.id()) || pendingOrders.containsKey(own.id())) return false;
        WorldMorphState.SettlementData destination = capital(state, target.id());
        if (destination == null) return false;
        pendingOrders.put(own.id(), "PEACE: " + target.name());
        spawnMessenger(level, state, npcs, roads, own, target, destination, "PEACE", reason == null ? "Peace proposal" : reason);
        return true;
    }

    public boolean orderReport(WorldMorphState state, UUID leader) {
        WorldMorphState.KingdomData own = state.kingdoms().values().stream()
                .filter(k -> k.leader().equals(leader)).findFirst().orElse(null);
        return own != null && unlocked(state, own.id());
    }

    private void ensureAssistant(ServerLevel level, WorldMorphState state, NpcManager npcs,
                                 WorldMorphState.KingdomData kingdom) {
        UUID existing = assistants.get(kingdom.id());
        Entity entity = existing == null ? null : level.getEntity(existing);
        if (entity != null && entity.isAlive()) return;
        UUID id = UUID.randomUUID();
        NpcProfile profile = npcs.create(id, "Royal Assistant");
        profile.setKingdom(kingdom.id());
        WorldMorphState.SettlementData capital = capital(state, kingdom.id());
        if (capital == null) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, capital.center().getX(), capital.center().getZ());
        Entity assistant = create(level, "villager", id, capital.center().getX() + .5, y, capital.center().getZ() + .5, "Royal Assistant");
        if (assistant != null) {
            assistants.put(kingdom.id(), id);
            state.history("ROYAL_ASSISTANT", kingdom.name() + " appointed a Royal Assistant after reaching 100 citizens.");
        }
    }

    private void followLeader(ServerLevel level, WorldMorphState.KingdomData kingdom) {
        ServerPlayer leader = level.getServer().getPlayerList().getPlayer(kingdom.leader());
        UUID assistantId = assistants.get(kingdom.id());
        Entity assistant = assistantId == null ? null : level.getEntity(assistantId);
        if (leader == null || assistant == null || !leader.isAlive()) return;
        if (assistant.distanceTo(leader) > 10 && !pendingOrders.containsKey(kingdom.id())) {
            double x = leader.getX() - leader.getLookAngle().x * 2;
            double z = leader.getZ() - leader.getLookAngle().z * 2;
            assistant.setPos(x, leader.getY(), z);
        }
    }

    private void spawnMessenger(ServerLevel level, WorldMorphState state, NpcManager npcs,
                                RoadNetwork roads, WorldMorphState.KingdomData own,
                                WorldMorphState.KingdomData target, WorldMorphState.SettlementData destination,
                                String type, String reason) {
        WorldMorphState.SettlementData origin = capital(state, own.id());
        if (origin == null) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.center().getX(), origin.center().getZ());
        UUID riderId = UUID.randomUUID();
        UUID horseId = UUID.randomUUID();
        Entity rider = create(level, "villager", riderId, origin.center().getX() + .5, y, origin.center().getZ() + .5,
                "Royal Messenger");
        Entity horse = create(level, "horse", horseId, origin.center().getX() + 1.5, y, origin.center().getZ() + .5, "Royal Messenger Horse");
        if (rider == null || horse == null) return;
        rider.startRiding(horse, true);
        npcs.create(riderId, "Royal Messenger").setKingdom(own.id());
        long travel = Math.max(MIN_ASSIGNMENT, roads.travelTicks(origin.center(), destination.center(), true));
        messengers.put(riderId, new Messenger(riderId, horseId, own.id(), target.id(),
                destination.center(), origin.center(), state.getSimulationTick() + travel, type, reason));
    }

    private void deliver(ServerLevel level, WorldMorphState state, Entity rider, Messenger m) {
        WorldMorphState.KingdomData target = state.kingdoms().get(m.targetKingdom());
        if (target == null) return;
        rider.setCustomName(Component.literal("Royal Messenger"));
        rider.setCustomNameVisible(true);
        if (target.leader() != null) {
            ServerPlayer king = level.getServer().getPlayerList().getPlayer(target.leader());
            if (king != null) king.displayClientMessage(Component.literal(
                    "A royal messenger arrives: " + m.type() + " — " + m.reason()), false);
        }
        state.history("LETTER_DELIVERED", "A messenger delivered a " + m.type() + " letter to " + target.name());
    }

    private void finishReturn(ServerLevel level, WorldMorphState state, Entity rider, Entity horse, Messenger m) {
        rider.discard();
        horse.discard();
        pendingOrders.remove(m.kingdom());
        state.history("MESSENGER_RETURNED", "The royal messenger returned home.");
    }

    private void moveMounted(ServerLevel level, Entity rider, Entity horse, BlockPos target, double speed) {
        double dx = target.getX() + .5 - rider.getX();
        double dz = target.getZ() + .5 - rider.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.01) return;
        rider.setPos(rider.getX() + dx / distance * speed, rider.getY(), rider.getZ() + dz / distance * speed);
        horse.setPos(rider.getX(), rider.getY(), rider.getZ());
    }

    private static Entity create(ServerLevel level, String typeName, UUID id, double x, double y, double z, String name) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.fromNamespaceAndPath("minecraft", typeName)).orElse(null);
        if (type == null) return null;
        Entity entity = type.value().create(level, EntitySpawnReason.COMMAND);
        if (entity == null) return null;
        entity.setUUID(id);
        entity.setPos(x, y, z);
        entity.setCustomName(Component.literal(name));
        entity.setCustomNameVisible(true);
        if (!level.addFreshEntity(entity)) return null;
        return entity;
    }

    private static WorldMorphState.SettlementData capital(WorldMorphState state, UUID kingdom) {
        return state.settlements().values().stream()
                .filter(s -> kingdom.equals(s.kingdomId()))
                .max(Comparator.comparingInt(WorldMorphState.SettlementData::population))
                .orElse(null);
    }

    private static int population(WorldMorphState state, UUID kingdom) {
        return state.settlements().values().stream()
                .filter(s -> kingdom.equals(s.kingdomId()))
                .mapToInt(WorldMorphState.SettlementData::population).sum();
    }
}
