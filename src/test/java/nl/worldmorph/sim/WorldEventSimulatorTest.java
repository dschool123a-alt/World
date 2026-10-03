package nl.worldmorph.sim;

import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import nl.worldmorph.data.WorldMorphState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldEventSimulatorTest {
    @Test void goodHarvestImprovesFoodNeedAndRecordsHistory() {
        WorldMorphState state = new WorldMorphState();
        var kingdom = state.createKingdom("Testland", UUID.randomUUID());
        var settlement = state.createSettlement("Wheatford", new BlockPos(0, 64, 0), kingdom.id());
        var needs = new SettlementNeedsManager();
        var events = new WorldEventManager();
        var simulator = new WorldEventSimulator(events, needs, new EconomyManager());
        int before = needs.get(settlement.id()).food();

        var event = simulator.apply(state, settlement.id(), WorldEventManager.Type.GOOD_HARVEST, new Random(1));

        assertEquals(before + 18, needs.get(settlement.id()).food());
        assertEquals(WorldEventManager.Type.GOOD_HARVEST, event.type());
        assertFalse(state.history().isEmpty());
    }

    @Test void needsAreClampedWhenEventsRepeat() {
        WorldMorphState state = new WorldMorphState();
        var kingdom = state.createKingdom("Testland", UUID.randomUUID());
        var settlement = state.createSettlement("Rainford", new BlockPos(0, 64, 0), kingdom.id());
        var needs = new SettlementNeedsManager();
        var simulator = new WorldEventSimulator(new WorldEventManager(), needs, new EconomyManager());
        for (int i = 0; i < 20; i++) simulator.apply(state, settlement.id(), WorldEventManager.Type.STORM, new Random(i));
        assertTrue(needs.get(settlement.id()).housing() >= 0);
        assertTrue(needs.get(settlement.id()).safety() >= 0);
        assertTrue(needs.get(settlement.id()).housing() <= 100);
    }

    @Test void outbreakNeverMakesPopulationNegative() {
        WorldMorphState state = new WorldMorphState();
        var kingdom = state.createKingdom("Testland", UUID.randomUUID());
        var settlement = state.createSettlement("Smallford", new BlockPos(0, 64, 0), kingdom.id());
        var simulator = new WorldEventSimulator(new WorldEventManager(), new SettlementNeedsManager(), new EconomyManager());
        simulator.apply(state, settlement.id(), WorldEventManager.Type.OUTBREAK, new Random(2));
        assertTrue(state.settlements().get(settlement.id()).population() >= 0);
    }

    @Test void eventTickDoesNothingBeforeDailyBoundary() {
        WorldMorphState state = new WorldMorphState();
        var kingdom = state.createKingdom("Testland", UUID.randomUUID());
        state.createSettlement("Tickford", new BlockPos(0, 64, 0), kingdom.id());
        var events = new WorldEventManager();
        new WorldEventSimulator(events, new SettlementNeedsManager(), new EconomyManager()).tick(state);
        assertTrue(events.recent().isEmpty());
    }
}
