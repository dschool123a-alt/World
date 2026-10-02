package nl.worldmorph.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class WorldMorphState extends SavedData {
    public static final String DATA_ID = "worldmorph_state";

    private final Map<UUID, KingdomData> kingdoms = new LinkedHashMap<>();
    private final Map<UUID, SettlementData> settlements = new LinkedHashMap<>();
    private long simulationTick;

    public static WorldMorphState load(CompoundTag tag) {
        WorldMorphState state = new WorldMorphState();
        state.simulationTick = tag.getLong("SimulationTick").orElse(0L);
        return state;
    }

    public static WorldMorphState create() {
        return new WorldMorphState();
    }

    public void tick() {
        simulationTick++;
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

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("SimulationTick", simulationTick);
        tag.putInt("KingdomCount", kingdoms.size());
        tag.putInt("SettlementCount", settlements.size());
        return tag;
    }

    public record KingdomData(UUID id, String name, UUID leader) {}
    public record SettlementData(UUID id, String name, BlockPos center, UUID kingdomId) {}
}
