package nl.worldmorph.sim;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import nl.worldmorph.data.WorldMorphState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldEconomicCycleTest {
    @Test void taxCollectionAddsIncomeBasedOnSettlementPopulation() {
        WorldMorphState state = new WorldMorphState();
        var kingdom = state.createKingdom("Taxland", UUID.randomUUID());
        state.createSettlement("Town", new BlockPos(0, 64, 0), kingdom.id());
        state.createSettlement("Village", new BlockPos(40, 64, 40), kingdom.id());
        var taxes = new TaxManager();
        int before = state.kingdoms().get(kingdom.id()).treasury();
        int income = taxes.collect(state, kingdom.id(), 2);
        assertEquals(20, income);
        assertEquals(before + 20, state.kingdoms().get(kingdom.id()).treasury());
    }

    @Test void highTaxReducesStabilityButNeverBelowZero() {
        WorldMorphState state = new WorldMorphState();
        var kingdom = state.createKingdom("HighTax", UUID.randomUUID());
        new TaxManager().setRate(state, kingdom.id(), 75);
        for (int i = 0; i < 100; i++) new TaxManager().collect(state, kingdom.id(), 100);
        assertTrue(state.kingdoms().get(kingdom.id()).stability() >= 0);
        assertTrue(state.kingdoms().get(kingdom.id()).treasury() > kingdom.treasury());
    }

    @Test void taxRateCannotExceedConfiguredBounds() {
        WorldMorphState state = new WorldMorphState();
        var kingdom = state.createKingdom("Bounded", UUID.randomUUID());
        TaxManager taxes = new TaxManager();
        assertEquals(75, taxes.setRate(state, kingdom.id(), 999));
        assertEquals(0, taxes.setRate(state, kingdom.id(), -20));
    }
}
