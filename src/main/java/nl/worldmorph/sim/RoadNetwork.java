package nl.worldmorph.sim;

import java.util.*;
import net.minecraft.core.BlockPos;

/**
 * Lightweight civilization road graph. Roads are persistent in-memory simulation links;
 * physical road blocks can be built by the civilization expansion layer.
 */
public final class RoadNetwork {
    public record Road(UUID id, UUID settlement, BlockPos start, BlockPos end, int quality) {}
    private final Map<UUID, Road> roads = new LinkedHashMap<>();

    public Road build(UUID settlement, BlockPos start, BlockPos end, int quality) {
        Road r = new Road(UUID.randomUUID(), settlement, start, end, Math.max(1, Math.min(100, quality)));
        roads.put(r.id(), r);
        return r;
    }

    public List<Road> inSettlement(UUID settlement) {
        return roads.values().stream().filter(r -> r.settlement().equals(settlement)).toList();
    }

    public List<Road> all() {
        return List.copyOf(roads.values());
    }

    public boolean hasConnection(BlockPos a, BlockPos b) {
        return roads.values().stream().anyMatch(r ->
                (near(r.start(), a) && near(r.end(), b)) ||
                (near(r.start(), b) && near(r.end(), a)));
    }

    /** Lower is faster. Road quality reduces travel cost; no road remains expensive. */
    public int travelCost(BlockPos a, BlockPos b) {
        int direct = Math.max(1, (int) Math.ceil(Math.sqrt(a.distSqr(b))));
        Road best = roads.values().stream()
                .filter(r -> near(r.start(), a) || near(r.end(), a))
                .min(Comparator.comparingInt(r -> Math.abs(r.start().distManhattan(b)) + 100 - r.quality()))
                .orElse(null);
        if (best == null) return direct;
        int roadDistance = Math.min(
                best.start().distManhattan(a) + best.end().distManhattan(b),
                best.end().distManhattan(a) + best.start().distManhattan(b));
        int roadCost = Math.max(1, roadDistance - best.quality() * 2);
        return Math.min(direct, roadCost);
    }

    public int travelTicks(BlockPos a, BlockPos b, boolean mounted) {
        int cost = travelCost(a, b);
        return Math.max(20, (int) Math.round(cost * (mounted ? 0.65 : 1.0)));
    }

    private static boolean near(BlockPos a, BlockPos b) {
        return a.distSqr(b) <= 24 * 24;
    }
}
