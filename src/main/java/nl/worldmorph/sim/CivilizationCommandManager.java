package nl.worldmorph.sim;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.data.WorldMorphStateAccess;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public final class CivilizationCommandManager {
    private CivilizationCommandManager() {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root, WorldMorphSimulation sim) {
        root.then(kingdomCommands());
        root.then(settlementCommands());
        root.then(economyCommands());
        root.then(populationCommands(sim));
        root.then(worldCommands(sim));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> kingdomCommands() {
        var root = Commands.literal("kingdominfo");
        root.then(read("info", "Kingdoms", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(
                k.name() + " | government=" + k.government() + " | treasury=" + k.treasury() +
                " | stability=" + k.stability() + " | tax=" + k.taxRate()), false);
            return s.kingdoms().size();
        }));
        root.then(read("treasury", "Kingdom treasury", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + k.treasury() + " coins"), false);
            return s.kingdoms().size();
        }));
        root.then(read("stability", "Kingdom stability", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + k.stability() + "/100"), false);
            return s.kingdoms().size();
        }));
        root.then(read("citizens", "Kingdom citizens", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + population(s, k.id()) + " citizens"), false);
            return s.kingdoms().size();
        }));
        root.then(read("settlements", "Kingdom settlements", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + settlementCount(s, k.id()) + " settlements"), false);
            return s.kingdoms().size();
        }));
        root.then(read("budget", "Kingdom budget", (s, c) -> {
            for (var k : s.kingdoms().values()) {
                int pop = population(s, k.id());
                int estimatedIncome = Math.max(0, pop * k.taxRate());
                c.sendSuccess(() -> Component.literal(k.name() + " | treasury=" + k.treasury() + " | estimated tax income=" + estimatedIncome + "/day"), false);
            }
            return s.kingdoms().size();
        }));
        root.then(read("government", "Kingdom governments", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + k.government()), false);
            return s.kingdoms().size();
        }));
        root.then(read("leader", "Kingdom leaders", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + k.leader()), false);
            return s.kingdoms().size();
        }));
        root.then(read("tax", "Kingdom tax rates", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + k.taxRate() + " coins/citizen"), false);
            return s.kingdoms().size();
        }));
        root.then(read("report", "Kingdom report", (s, c) -> {
            for (var k : s.kingdoms().values()) {
                int pop = population(s, k.id());
                c.sendSuccess(() -> Component.literal(
                    k.name() + " | " + pop + " citizens | " + settlementCount(s, k.id()) +
                    " settlements | " + k.treasury() + " treasury | stability " + k.stability() +
                    " | " + k.government()), false);
            }
            return s.kingdoms().size();
        }));
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> settlementCommands() {
        var root = Commands.literal("settlementinfo");
        root.then(read("count", "Settlement count", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Settlements: " + s.settlements().size()), false); return 1;
        }));
        root.then(read("population", "Settlement population", (s, c) -> {
            for (var x : s.settlements().values()) c.sendSuccess(() -> Component.literal(x.name() + ": " + x.population()), false);
            return s.settlements().size();
        }));
        root.then(read("types", "Settlement types", (s, c) -> {
            Map<String, Long> counts = s.settlements().values().stream().collect(java.util.stream.Collectors.groupingBy(WorldMorphState.SettlementData::type, java.util.stream.Collectors.counting()));
            counts.forEach((type, count) -> c.sendSuccess(() -> Component.literal(type + ": " + count), false));
            return Math.max(1, counts.size());
        }));
        root.then(read("largest", "Largest settlement", (s, c) -> {
            var x = s.settlements().values().stream().max(Comparator.comparingInt(WorldMorphState.SettlementData::population)).orElse(null);
            c.sendSuccess(() -> Component.literal(x == null ? "No settlements." : x.name() + ": " + x.population()), false); return 1;
        }));
        root.then(read("smallest", "Smallest settlement", (s, c) -> {
            var x = s.settlements().values().stream().min(Comparator.comparingInt(WorldMorphState.SettlementData::population)).orElse(null);
            c.sendSuccess(() -> Component.literal(x == null ? "No settlements." : x.name() + ": " + x.population()), false); return 1;
        }));
        root.then(read("locations", "Settlement locations", (s, c) -> {
            for (var x : s.settlements().values()) c.sendSuccess(() -> Component.literal(
                x.name() + ": " + x.center().getX() + ", " + x.center().getY() + ", " + x.center().getZ()), false);
            return s.settlements().size();
        }));
        root.then(read("kingdoms", "Settlement ownership", (s, c) -> {
            for (var x : s.settlements().values()) {
                var k = s.kingdoms().get(x.kingdomId());
                c.sendSuccess(() -> Component.literal(x.name() + ": " + (k == null ? "unclaimed" : k.name())), false);
            }
            return s.settlements().size();
        }));
        root.then(read("cities", "Cities", (s, c) -> {
            long n = s.settlements().values().stream().filter(x -> "CITY".equals(x.type())).count();
            c.sendSuccess(() -> Component.literal("Cities: " + n), false); return 1;
        }));
        root.then(read("towns", "Towns", (s, c) -> {
            long n = s.settlements().values().stream().filter(x -> "TOWN".equals(x.type())).count();
            c.sendSuccess(() -> Component.literal("Towns: " + n), false); return 1;
        }));
        root.then(read("report", "Settlement report", (s, c) -> {
            for (var x : s.settlements().values()) c.sendSuccess(() -> Component.literal(
                x.name() + " | " + x.type() + " | pop=" + x.population() + " | kingdom=" + x.kingdomId()), false);
            return s.settlements().size();
        }));
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> economyCommands() {
        var root = Commands.literal("economyinfo");
        root.then(read("treasury", "Combined treasury", (s, c) -> {
            long total = s.kingdoms().values().stream().mapToLong(WorldMorphState.KingdomData::treasury).sum();
            c.sendSuccess(() -> Component.literal("Combined kingdom treasury: " + total), false); return 1;
        }));
        root.then(read("income", "Estimated income", (s, c) -> {
            long income = s.kingdoms().values().stream().mapToLong(k -> (long) population(s, k.id()) * k.taxRate()).sum();
            c.sendSuccess(() -> Component.literal("Estimated daily tax income: " + income), false); return 1;
        }));
        root.then(read("taxes", "Tax overview", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + k.taxRate() + " coins/citizen"), false);
            return s.kingdoms().size();
        }));
        root.then(read("expenses", "Recorded treasury expenses", (s, c) -> {
            long n = s.history().stream().filter(e -> "TREASURY_SPEND".equals(e.type())).count();
            c.sendSuccess(() -> Component.literal("Recorded treasury spend events: " + n), false); return 1;
        }));
        root.then(read("wealth", "Kingdom wealth", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + k.treasury()), false);
            return s.kingdoms().size();
        }));
        root.then(read("market", "Market prices", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Use /worldmorph economy list for current simulated market prices."), false); return 1;
        }));
        root.then(read("prices", "Price system", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Market price system is active; see /worldmorph economy list."), false); return 1;
        }));
        root.then(read("report", "Economy report", (s, c) -> {
            long treasury = s.kingdoms().values().stream().mapToLong(WorldMorphState.KingdomData::treasury).sum();
            long income = s.kingdoms().values().stream().mapToLong(k -> (long) population(s, k.id()) * k.taxRate()).sum();
            long spending = s.history().stream().filter(e -> "TREASURY_SPEND".equals(e.type())).count();
            c.sendSuccess(() -> Component.literal("Economy | treasury=" + treasury + " | estimated daily tax income=" + income + " | spend events=" + spending), false); return 1;
        }));
        root.then(read("balance", "Economy balance", (s, c) -> {
            long treasury = s.kingdoms().values().stream().mapToLong(WorldMorphState.KingdomData::treasury).sum();
            long income = s.kingdoms().values().stream().mapToLong(k -> (long) population(s, k.id()) * k.taxRate()).sum();
            c.sendSuccess(() -> Component.literal("Treasury=" + treasury + " | estimated income=" + income), false); return 1;
        }));
        root.then(read("activity", "Economy activity", (s, c) -> {
            long spend = s.history().stream().filter(e -> "TREASURY_SPEND".equals(e.type())).count();
            long founded = s.history().stream().filter(e -> "SETTLEMENT_FOUNDED".equals(e.type())).count();
            c.sendSuccess(() -> Component.literal("Economic activity | treasury spend events=" + spend + " | settlements founded=" + founded), false); return 1;
        }));
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> populationCommands(WorldMorphSimulation sim) {
        var root = Commands.literal("populationinfo");
        root.then(read("total", "Total population", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Stored population: " + totalPopulation(s)), false); return 1;
        }));
        root.then(read("kingdoms", "Population by kingdom", (s, c) -> {
            for (var k : s.kingdoms().values()) c.sendSuccess(() -> Component.literal(k.name() + ": " + population(s, k.id())), false);
            return s.kingdoms().size();
        }));
        root.then(read("settlements", "Population by settlement", (s, c) -> {
            for (var x : s.settlements().values()) c.sendSuccess(() -> Component.literal(x.name() + ": " + x.population()), false);
            return s.settlements().size();
        }));
        root.then(read("npcs", "NPC population", (s, c) -> {
            c.sendSuccess(() -> Component.literal("NPC profiles: " + sim.npcs().size()), false); return 1;
        }));
        root.then(read("alive", "Living NPCs", (s, c) -> {
            long n = sim.npcs().profiles().values().stream().filter(npc -> npc.alive()).count();
            c.sendSuccess(() -> Component.literal("Living NPC profiles: " + n), false); return 1;
        }));
        root.then(read("dead", "Dead NPCs", (s, c) -> {
            long n = sim.npcs().profiles().values().stream().filter(npc -> !npc.alive()).count();
            c.sendSuccess(() -> Component.literal("Dead NPC profiles: " + n), false); return 1;
        }));
        root.then(read("jobs", "NPC jobs", (s, c) -> {
            Map<String, Long> jobs = sim.npcs().profiles().values().stream().filter(npc -> npc.alive()).collect(java.util.stream.Collectors.groupingBy(npc -> npc.job().name(), java.util.stream.Collectors.counting()));
            jobs.forEach((job, count) -> c.sendSuccess(() -> Component.literal(job + ": " + count), false));
            return Math.max(1, jobs.size());
        }));
        root.then(read("families", "Population families", (s, c) -> {
            long n = s.history().stream().filter(e -> "FAMILY_FOUNDED".equals(e.type())).count();
            c.sendSuccess(() -> Component.literal("Recorded family foundations: " + n), false); return 1;
        }));
        root.then(read("births", "Birth history", (s, c) -> {
            long n = s.history().stream().filter(e -> "CHILD_BORN".equals(e.type())).count();
            c.sendSuccess(() -> Component.literal("Recorded births: " + n), false); return 1;
        }));
        root.then(read("report", "Population report", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Population | stored=" + totalPopulation(s) + " | NPC profiles=" + sim.npcs().size() +
                " | settlements=" + s.settlements().size() + " | kingdoms=" + s.kingdoms().size()), false); return 1;
        }));
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> worldCommands(WorldMorphSimulation sim) {
        var root = Commands.literal("worldinfo");
        root.then(read("tick", "Simulation tick", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Simulation tick: " + s.getSimulationTick()), false); return 1;
        }));
        root.then(read("history", "History size", (s, c) -> {
            c.sendSuccess(() -> Component.literal("History events stored: " + s.history().size()), false); return 1;
        }));
        root.then(read("events", "Recent events", (s, c) -> {
            long n = sim.events().recent().size();
            c.sendSuccess(() -> Component.literal("Session events available: " + n), false); return 1;
        }));
        root.then(read("roads", "Road network", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Roads registered: " + sim.roads().all().size()), false); return 1;
        }));
        root.then(read("roadsreport", "Road report", (s, c) -> {
            for (var road : sim.roads().all()) c.sendSuccess(() -> Component.literal(
                "Road " + road.id() + " | settlement=" + road.settlement() + " | quality=" + road.quality()), false);
            return Math.max(1, sim.roads().all().size());
        }));
        root.then(read("population", "World population", (s, c) -> {
            c.sendSuccess(() -> Component.literal("World population: " + totalPopulation(s)), false); return 1;
        }));
        root.then(read("kingdoms", "World kingdoms", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Kingdoms: " + s.kingdoms().size()), false); return 1;
        }));
        root.then(read("settlements", "World settlements", (s, c) -> {
            c.sendSuccess(() -> Component.literal("Settlements: " + s.settlements().size()), false); return 1;
        }));
        root.then(read("report", "World report", (s, c) -> {
            c.sendSuccess(() -> Component.literal(
                "WorldMorph | tick=" + s.getSimulationTick() + " | kingdoms=" + s.kingdoms().size() +
                " | settlements=" + s.settlements().size() + " | population=" + totalPopulation(s) +
                " | NPCs=" + sim.npcs().size() + " | roads=" + sim.roads().all().size() +
                " | history=" + s.history().size()), false); return 1;
        }));
        root.then(read("historytypes", "History types", (s, c) -> {
            Map<String, Long> types = s.history().stream().collect(java.util.stream.Collectors.groupingBy(WorldMorphState.HistoryEvent::type, java.util.stream.Collectors.counting()));
            types.forEach((type, count) -> c.sendSuccess(() -> Component.literal(type + ": " + count), false));
            return Math.max(1, types.size());
        }));
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> read(String name, String ignored, Reader reader) {
        return Commands.literal(name).executes(c -> {
            try {
                return reader.read(WorldMorphStateAccess.get(c.getSource().getLevel()), c.getSource());
            } catch (Exception ex) {
                c.getSource().sendFailure(Component.literal("Command failed: " + ex.getMessage()));
                return 0;
            }
        });
    }

    private interface Reader {
        int read(WorldMorphState state, CommandSourceStack source);
    }

    private static int population(WorldMorphState s, UUID kingdomId) {
        return s.settlements().values().stream().filter(x -> x.kingdomId().equals(kingdomId)).mapToInt(WorldMorphState.SettlementData::population).sum();
    }

    private static int settlementCount(WorldMorphState s, UUID kingdomId) {
        return (int) s.settlements().values().stream().filter(x -> x.kingdomId().equals(kingdomId)).count();
    }

    private static int totalPopulation(WorldMorphState s) {
        return s.settlements().values().stream().mapToInt(WorldMorphState.SettlementData::population).sum();
    }
}
