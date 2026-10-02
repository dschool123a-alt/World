package nl.worldmorph.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldMorphState extends SavedData {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<BlockPos> POS_CODEC = BlockPos.CODEC;

    public static final Codec<KingdomData> KINGDOM_CODEC = RecordCodecBuilder.create(i -> i.group(
        UUID_CODEC.fieldOf("id").forGetter(KingdomData::id),
        Codec.STRING.fieldOf("name").forGetter(KingdomData::name),
        UUID_CODEC.fieldOf("leader").forGetter(KingdomData::leader),
        Codec.STRING.fieldOf("government").forGetter(KingdomData::government),
        Codec.INT.fieldOf("treasury").forGetter(KingdomData::treasury),
        Codec.INT.fieldOf("stability").forGetter(KingdomData::stability),
        Codec.INT.fieldOf("taxRate").forGetter(KingdomData::taxRate)
    ).apply(i, KingdomData::new));

    public static final Codec<SettlementData> SETTLEMENT_CODEC = RecordCodecBuilder.create(i -> i.group(
        UUID_CODEC.fieldOf("id").forGetter(SettlementData::id),
        Codec.STRING.fieldOf("name").forGetter(SettlementData::name),
        POS_CODEC.fieldOf("center").forGetter(SettlementData::center),
        UUID_CODEC.fieldOf("kingdomId").forGetter(SettlementData::kingdomId),
        Codec.STRING.fieldOf("type").forGetter(SettlementData::type),
        Codec.INT.fieldOf("population").forGetter(SettlementData::population)
    ).apply(i, SettlementData::new));

    private static final Codec<WorldMorphState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.LONG.fieldOf("simulationTick").forGetter(WorldMorphState::getSimulationTick),
        KINGDOM_CODEC.listOf().fieldOf("kingdoms").forGetter(s -> new ArrayList<>(s.kingdoms.values())),
        SETTLEMENT_CODEC.listOf().fieldOf("settlements").forGetter(s -> new ArrayList<>(s.settlements.values())),
        HistoryEvent.CODEC.listOf().fieldOf("history").forGetter(s -> s.history)
    ).apply(i, WorldMorphState::fromData));

    public static final SavedDataType<WorldMorphState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("worldmorph", "state"),
        WorldMorphState::new,
        CODEC,
        null
    );

    private final Map<UUID, KingdomData> kingdoms = new LinkedHashMap<>();
    private final Map<UUID, SettlementData> settlements = new LinkedHashMap<>();
    private final List<HistoryEvent> history = new ArrayList<>();
    private long simulationTick;

    public WorldMorphState() {}

    private static WorldMorphState fromData(long tick, List<KingdomData> kingdoms, List<SettlementData> settlements, List<HistoryEvent> history) {
        WorldMorphState state = new WorldMorphState();
        state.simulationTick = tick;
        kingdoms.forEach(k -> state.kingdoms.put(k.id(), k));
        settlements.forEach(s -> state.settlements.put(s.id(), s));
        state.history.addAll(history);
        return state;
    }

    public void tick() {
        simulationTick++;
        if (simulationTick % 20 == 0) setDirty();
    }

    public long getSimulationTick() { return simulationTick; }
    public Map<UUID, KingdomData> kingdoms() { return kingdoms; }
    public Map<UUID, SettlementData> settlements() { return settlements; }
    public List<HistoryEvent> history() { return List.copyOf(history); }

    public KingdomData createKingdom(String name, UUID leader) {
        KingdomData kingdom = new KingdomData(UUID.randomUUID(), name, leader, "MONARCHY", 100, 75, 10);
        kingdoms.put(kingdom.id(), kingdom);
        history("KINGDOM_FOUNDED", name);
        setDirty();
        return kingdom;
    }

    public SettlementData createSettlement(String name, BlockPos center, UUID kingdomId) {
        SettlementData settlement = new SettlementData(UUID.randomUUID(), name, center, kingdomId, "VILLAGE", 1);
        settlements.put(settlement.id(), settlement);
        history("SETTLEMENT_FOUNDED", name);
        setDirty();
        return settlement;
    }

    public void history(String type, String description) {
        history.add(new HistoryEvent(simulationTick, type, description));
        if (history.size() > 5000) history.removeFirst();
        setDirty();
    }

    public record KingdomData(UUID id, String name, UUID leader, String government, int treasury, int stability, int taxRate) {
        public KingdomData withTreasury(int value) { return new KingdomData(id, name, leader, government, value, stability, taxRate); }
        public KingdomData withStability(int value) { return new KingdomData(id, name, leader, government, value < 0 ? 0 : Math.min(100, value), taxRate); }
    }

    public record SettlementData(UUID id, String name, BlockPos center, UUID kingdomId, String type, int population) {
        public SettlementData grow() { return new SettlementData(id, name, center, kingdomId, population >= 1000 ? "CITY" : population >= 100 ? "TOWN" : type, population + 1); }
    }

    public record HistoryEvent(long tick, String type, String description) {
        public static final Codec<HistoryEvent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("tick").forGetter(HistoryEvent::tick),
            Codec.STRING.fieldOf("type").forGetter(HistoryEvent::type),
            Codec.STRING.fieldOf("description").forGetter(HistoryEvent::description)
        ).apply(i, HistoryEvent::new));
    }
}
