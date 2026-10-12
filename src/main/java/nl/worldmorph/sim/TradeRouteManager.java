package nl.worldmorph.sim;

import java.util.*;
import nl.worldmorph.data.WorldMorphState;

/**
 * Scheduled settlement trade. Goods leave the origin only when they exist in its stockpile,
 * then arrive after a road-adjusted travel delay.
 */
public final class TradeRouteManager {
    public record Route(UUID id, UUID origin, UUID destination, String good,
                        int quantity, int intervalTicks, boolean active) {}
    private record Shipment(UUID routeId, UUID destination, ResourceManager.Resource good,
                            int quantity, long arrivalTick, String originName, String destinationName) {}

    private final Map<UUID, Route> routes = new LinkedHashMap<>();
    private final Map<UUID, Long> nextDeparture = new HashMap<>();
    private final List<Shipment> shipments = new ArrayList<>();

    public Route create(UUID origin, UUID destination, String good, int quantity, int intervalTicks) {
        if (origin == null || destination == null || origin.equals(destination)
                || quantity < 1 || intervalTicks < 20 || good == null) return null;
        ResourceManager.Resource resource;
        try {
            resource = ResourceManager.Resource.valueOf(good.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
        Route route = new Route(UUID.randomUUID(), origin, destination, resource.name(),
                quantity, intervalTicks, true);
        routes.put(route.id(), route);
        return route;
    }

    public Route setActive(UUID id, boolean active) {
        Route route = routes.get(id);
        if (route == null) return null;
        Route updated = new Route(route.id(), route.origin(), route.destination(), route.good(),
                route.quantity(), route.intervalTicks(), active);
        routes.put(id, updated);
        return updated;
    }

    public List<Route> active() {
        return routes.values().stream().filter(Route::active).toList();
    }

    public List<Route> all() {
        return List.copyOf(routes.values());
    }

    public int inTransit() {
        return shipments.size();
    }

    public void tick(WorldMorphState state, ResourceManager resources, RoadNetwork roads) {
        long tick = state.getSimulationTick();

        for (Iterator<Shipment> it = shipments.iterator(); it.hasNext();) {
            Shipment shipment = it.next();
            if (tick < shipment.arrivalTick()) continue;
            resources.add(shipment.destination(), shipment.good(), shipment.quantity());
            state.history("TRADE_ROUTE_ARRIVED", shipment.quantity() + " " + shipment.good()
                    + " arrived at " + shipment.destinationName() + " from " + shipment.originName());
            it.remove();
        }

        for (Route route : active()) {
            WorldMorphState.SettlementData origin = state.settlements().get(route.origin());
            WorldMorphState.SettlementData destination = state.settlements().get(route.destination());
            if (origin == null || destination == null) continue;
            long departure = nextDeparture.getOrDefault(route.id(), tick);
            if (tick < departure) continue;

            ResourceManager.Resource good;
            try {
                good = ResourceManager.Resource.valueOf(route.good());
            } catch (IllegalArgumentException ex) {
                continue;
            }

            if (!resources.consume(route.origin(), good, route.quantity())) {
                nextDeparture.put(route.id(), tick + Math.min(route.intervalTicks(), 1200));
                continue;
            }
            int travelTicks = roads.travelTicks(origin.center(), destination.center(), false);
            shipments.add(new Shipment(route.id(), route.destination(), good, route.quantity(),
                    tick + travelTicks, origin.name(), destination.name()));
            nextDeparture.put(route.id(), tick + route.intervalTicks());
            state.history("TRADE_ROUTE_DEPARTED", route.quantity() + " " + good
                    + " departed " + origin.name() + " for " + destination.name());
        }
    }
}
