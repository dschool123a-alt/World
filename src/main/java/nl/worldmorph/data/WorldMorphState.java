package nl.worldmorph.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldMorphState extends SavedData {
    public static final SavedDataType<WorldMorphState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("worldmorph", "state"),
        WorldMorphState::new,
        Codec.LONG.xmap(WorldMorphState::fromTick, WorldMorphState::getSimulationTick),
        null
    );

    private final Map<UUID, KingdomData> kingdoms = new LinkedHashMap<>();
    private final Map<UUID, SettlementData> settlements = new LinkedHashMap<>();
    private long simulationTick;

    public WorldMorphState() {
    }

    private static WorldMorphState fromTick(long tick) {
        WorldMorphState state = new WorldMorphState();
        state.simulationTick = tick;
        return state;
    }

    public void tick() {
        simulationTick++;
        if (simulationTick % 20 == 0) {
            setDirty();
        }
    }

    public long getSimulationTick() {
        return simulationTick;
    }

    public Map<UUID, KingdomData> kingdoms() {
        return kingdoms;
    }

    public Map<UUID, SettlementData> settlements() {
        return settlements;
    }

    public KingdomData createKingdom(String name, UUID leader) {
        KingdomData kingdom = new KingdomData(UUID.randomUUID(), name, leader);
        kingdoms.put(kingdom.id(), kingdom);
        setDirty();
        return kingdom;
    }

    public SettlementData createSettlement(String name, BlockPos center, UUID kingdomId) {
        SettlementData settlement = new SettlementData(UUID.randomUUID(), name, center, kingdomId);
        settlements.put(settlement.id(), settlement);
        setDirty();
        return settlement;
    }

    public record KingdomData(UUID id, String name, UUID leader) {}
    public record SettlementData(UUID id, String name, BlockPos center, UUID kingdomId) {}
}
