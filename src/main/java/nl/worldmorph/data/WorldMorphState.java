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
        state.simulationTick = tag.getLongOr("SimulationTick", 0L);

        tag.getCompound("Kingdoms").ifPresent(kingdomsTag -> {
            for (String key : kingdomsTag.getAllKeys()) {
                kingdomsTag.getCompound(key).ifPresent(data -> {
                    try {
                        UUID id = UUID.fromString(key);
                        UUID leader = UUID.fromString(data.getStringOr("Leader", new UUID(0L, 0L).toString()));
                        String name = data.getStringOr("Name", "Unnamed Kingdom");
                        state.kingdoms.put(id, new KingdomData(id, name, leader));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        });

        tag.getCompound("Settlements").ifPresent(settlementsTag -> {
            for (String key : settlementsTag.getAllKeys()) {
                settlementsTag.getCompound(key).ifPresent(data -> {
                    try {
                        UUID id = UUID.fromString(key);
                        UUID kingdomId = UUID.fromString(data.getStringOr("Kingdom", new UUID(0L, 0L).toString()));
                        String name = data.getStringOr("Name", "Unnamed Settlement");
                        int x = data.getIntOr("X", 0);
                        int y = data.getIntOr("Y", 0);
                        int z = data.getIntOr("Z", 0);
                        state.settlements.put(id, new SettlementData(id, name, new BlockPos(x, y, z), kingdomId));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        });

        return state;
    }

    public static WorldMorphState create() {
        return new WorldMorphState();
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

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("SimulationTick", simulationTick);

        CompoundTag kingdomsTag = new CompoundTag();
        for (KingdomData kingdom : kingdoms.values()) {
            CompoundTag data = new CompoundTag();
            data.putString("Name", kingdom.name());
            data.putString("Leader", kingdom.leader().toString());
            kingdomsTag.put(kingdom.id().toString(), data);
        }
        tag.put("Kingdoms", kingdomsTag);

        CompoundTag settlementsTag = new CompoundTag();
        for (SettlementData settlement : settlements.values()) {
            CompoundTag data = new CompoundTag();
            data.putString("Name", settlement.name());
            data.putString("Kingdom", settlement.kingdomId().toString());
            data.putInt("X", settlement.center().getX());
            data.putInt("Y", settlement.center().getY());
            data.putInt("Z", settlement.center().getZ());
            settlementsTag.put(settlement.id().toString(), data);
        }
        tag.put("Settlements", settlementsTag);

        return tag;
    }

    public record KingdomData(UUID id, String name, UUID leader) {}
    public record SettlementData(UUID id, String name, BlockPos center, UUID kingdomId) {}
}
