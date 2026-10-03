package nl.worldmorph.sim;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import nl.worldmorph.data.WorldMorphState;

/**
 * Runs infrequent, deterministic-by-world-tick settlement events.
 * One event per settlement per in-game day keeps the work bounded and makes
 * worlds reproducible when they are loaded with the same saved state.
 */
public final class WorldEventSimulator {
    private static final long EVENT_INTERVAL = 24_000L;
    private final WorldEventManager events;
    private final SettlementNeedsManager needs;
    private final EconomyManager economy;

    public WorldEventSimulator(WorldEventManager events, SettlementNeedsManager needs, EconomyManager economy) {
        this.events = events;
        this.needs = needs;
        this.economy = economy;
    }

    public void tick(WorldMorphState state) {
        long tick = state.getSimulationTick();
        if (tick == 0 || tick % EVENT_INTERVAL != 0 || state.settlements().isEmpty()) return;
        for (var settlement : List.copyOf(state.settlements().values())) {
            long seed = tick * 31L + settlement.id().getMostSignificantBits() + settlement.id().getLeastSignificantBits();
            Random random = new Random(seed);
            int roll = random.nextInt(100);
            WorldEventManager.Type type = roll < 20 ? WorldEventManager.Type.GOOD_HARVEST
                    : roll < 35 ? WorldEventManager.Type.POOR_HARVEST
                    : roll < 50 ? WorldEventManager.Type.MERCHANT_CARAVAN
                    : roll < 63 ? WorldEventManager.Type.IMMIGRATION
                    : roll < 72 ? WorldEventManager.Type.OUTBREAK
                    : roll < 82 ? WorldEventManager.Type.STORM
                    : roll < 92 ? WorldEventManager.Type.DISCOVERY
                    : WorldEventManager.Type.LOCAL_FESTIVAL;
            apply(state, settlement.id(), type, random);
        }
    }

    public WorldEventManager.Event apply(WorldMorphState state, UUID settlementId, WorldEventManager.Type type, Random random) {
        var settlement = state.settlements().get(settlementId);
        if (settlement == null) throw new IllegalArgumentException("Unknown settlement: " + settlementId);
        String detail;
        switch (type) {
            case GOOD_HARVEST -> {
                needs.change(settlementId, SettlementNeedsManager.Need.FOOD, 18);
                economy.trade("wheat", 180, 70);
                detail = settlement.name() + " had a good harvest; food stores improved.";
            }
            case POOR_HARVEST -> {
                needs.change(settlementId, SettlementNeedsManager.Need.FOOD, -18);
                economy.trade("wheat", 45, 140);
                changeStability(state, settlement.kingdomId(), -3);
                detail = settlement.name() + " had a poor harvest; food prices may rise.";
            }
            case MERCHANT_CARAVAN -> {
                needs.change(settlementId, SettlementNeedsManager.Need.TRADE, 15);
                economy.trade("tools", 70, 45);
                detail = "A merchant caravan visited " + settlement.name() + ".";
            }
            case IMMIGRATION -> {
                var n = needs.get(settlementId);
                int average = (n.food() + n.housing() + n.safety() + n.jobs() + n.trade()) / 5;
                if (average >= 50 && settlement.population() < 5000) {
                    state.updateSettlement(settlement.withPopulation(settlement.population() + 1));
                    needs.change(settlementId, SettlementNeedsManager.Need.HOUSING, -2);
                    detail = "A new family arrived in " + settlement.name() + ".";
                } else {
                    detail = "Travellers considered settling in " + settlement.name() + " but conditions were not ready.";
                }
            }
            case OUTBREAK -> {
                if (settlement.population() > 1) state.updateSettlement(settlement.withPopulation(settlement.population() - 1));
                needs.change(settlementId, SettlementNeedsManager.Need.SAFETY, -8);
                needs.change(settlementId, SettlementNeedsManager.Need.FOOD, -5);
                detail = "An illness outbreak strained " + settlement.name() + "; residents need support.";
            }
            case STORM -> {
                needs.change(settlementId, SettlementNeedsManager.Need.HOUSING, -10);
                needs.change(settlementId, SettlementNeedsManager.Need.SAFETY, -5);
                detail = "A severe storm damaged homes around " + settlement.name() + ".";
            }
            case DISCOVERY -> {
                changeStability(state, settlement.kingdomId(), 2);
                detail = "Residents of " + settlement.name() + " made a useful discovery.";
            }
            case LOCAL_FESTIVAL -> {
                needs.change(settlementId, SettlementNeedsManager.Need.TRADE, 5);
                changeStability(state, settlement.kingdomId(), 2);
                detail = "A local festival brought people together in " + settlement.name() + ".";
            }
            default -> throw new IllegalArgumentException("Unsupported event: " + type);
        }
        return events.emit(state, type, settlementId, detail);
    }

    private static void changeStability(WorldMorphState state, UUID kingdomId, int delta) {
        if (kingdomId == null) return;
        var kingdom = state.kingdoms().get(kingdomId);
        if (kingdom != null) state.updateKingdom(kingdom.withStability(kingdom.stability() + delta));
    }
}
