package nl.worldmorph.sim;

import java.util.*;
import nl.worldmorph.data.WorldMorphState;

/**
 * Large collection of deterministic civilization rules.
 * The rules are intentionally lightweight so they can run on long-running servers.
 */
public final class Civilization100Manager {
    private static final long DAILY = 24000L;
    private static final long WEEK = DAILY * 7L;
    private final Map<UUID, Integer> localTreasuries = new HashMap<>();
    private final Map<UUID, Integer> prosperity = new HashMap<>();
    private final Map<UUID, Integer> legitimacy = new HashMap<>();

    public void tick(WorldMorphState state) {
        long tick = state.getSimulationTick();
        if (tick % DAILY != 0) return;

        for (WorldMorphState.KingdomData kingdom : List.copyOf(state.kingdoms().values())) {
            Metrics m = new Metrics();
            List<WorldMorphState.SettlementData> settlements = state.settlements().values().stream()
                    .filter(s -> s.kingdomId().equals(kingdom.id())).toList();

            int population = settlements.stream().mapToInt(WorldMorphState.SettlementData::population).sum();
            int cities = (int) settlements.stream().filter(s -> "CITY".equals(s.type())).count();
            int towns = (int) settlements.stream().filter(s -> "TOWN".equals(s.type())).count();

            m.population = population;
            m.settlements = settlements.size();
            m.cities = cities;
            m.towns = towns;
            m.stability = kingdom.stability();
            m.treasury = kingdom.treasury();
            m.tax = kingdom.taxRate();
            m.prosperity = prosperity.getOrDefault(kingdom.id(), 50);
            m.legitimacy = legitimacy.getOrDefault(kingdom.id(), 60);
            m.government = kingdom.government();

            for (Rule rule : Rule.values()) apply(rule, m);

            int newTreasury = clamp(m.treasury + m.treasuryDelta, 0, Integer.MAX_VALUE);
            int newStability = clamp(m.stability + m.stabilityDelta, 0, 100);
            int newProsperity = clamp(m.prosperity + m.prosperityDelta, 0, 100);
            int newLegitimacy = clamp(m.legitimacy + m.legitimacyDelta, 0, 100);

            if (newTreasury != kingdom.treasury() || newStability != kingdom.stability()) {
                state.updateKingdom(kingdom.withTreasury(newTreasury).withStability(newStability));
            }
            prosperity.put(kingdom.id(), newProsperity);
            legitimacy.put(kingdom.id(), newLegitimacy);

            for (WorldMorphState.SettlementData s : settlements) {
                int change = settlementGrowth(s, m);
                if (change != 0) {
                    state.updateSettlement(s.withPopulation(s.population() + change));
                }
                if (tick % WEEK == 0) weeklySettlementReport(state, s, m);
            }

            if (m.populationDelta != 0 && !settlements.isEmpty()) {
                WorldMorphState.SettlementData target = settlements.get(0);
                state.updateSettlement(target.withPopulation(target.population() + m.populationDelta));
            }

            localTreasuries.put(kingdom.id(),
                    localTreasuries.getOrDefault(kingdom.id(), 0) + Math.max(0, m.localTax));
            if (tick % WEEK == 0) weeklyKingdomReport(state, kingdom, m, newTreasury, newStability);
        }
    }

    private int settlementGrowth(WorldMorphState.SettlementData s, Metrics m) {
        if (s.population() <= 0) return 0;
        int score = m.stability + m.prosperity;
        if (score >= 145 && s.population() < 1000) return 1;
        if (score <= 45 && s.population() > 2) return -1;
        return 0;
    }

    private void weeklySettlementReport(WorldMorphState state, WorldMorphState.SettlementData s, Metrics m) {
        state.history("SETTLEMENT_REPORT",
                s.name() + " | population " + s.population() +
                " | prosperity " + m.prosperity + " | stability " + m.stability);
    }

    private void weeklyKingdomReport(WorldMorphState state, WorldMorphState.KingdomData k,
                                     Metrics m, int treasury, int stability) {
        state.history("KINGDOM_REPORT",
                k.name() + " | population " + m.population +
                " | settlements " + m.settlements +
                " | treasury " + treasury +
                " | stability " + stability +
                " | prosperity " + m.prosperity +
                " | legitimacy " + m.legitimacy);
    }

    private void apply(Rule r, Metrics m) {
        switch (r) {
            case TAX_BASE -> { int income = Math.max(1, m.population * Math.max(0, m.tax) / 100); m.treasuryDelta += income; m.localTax += income / 4; }
            case TAX_PRESSURE -> { if (m.tax > 30) m.stabilityDelta -= (m.tax - 30) / 5; }
            case LOW_TAX_PROSPERITY -> { if (m.tax <= 15 && m.population > 0) m.prosperityDelta += 1; }
            case POPULATION_TAX -> { m.treasuryDelta += m.population / 20; }
            case SETTLEMENT_TAX -> { m.treasuryDelta += m.settlements; }
            case CITY_REVENUE -> { m.treasuryDelta += m.cities * 5; m.prosperityDelta += m.cities > 0 ? 1 : 0; }
            case TOWN_REVENUE -> { m.treasuryDelta += m.towns * 2; }
            case TRADE_REVENUE -> { m.treasuryDelta += Math.max(0, m.settlements - 1); }
            case MARKET_ACTIVITY -> { m.prosperityDelta += m.settlements > 1 ? 1 : 0; }
            case ROAD_COMMERCE -> { m.prosperityDelta += m.settlements > 2 ? 1 : 0; }

            case FOOD_SECURITY -> { if (m.population > 0 && m.prosperity < 25) m.stabilityDelta -= 1; }
            case FOOD_SURPLUS -> { if (m.prosperity > 70) m.stabilityDelta += 1; }
            case HOUSING_PRESSURE -> { if (m.population > m.settlements * 40) m.stabilityDelta -= 1; }
            case URBAN_DENSITY -> { if (m.cities > 0) m.prosperityDelta += 1; }
            case RURAL_SUPPORT -> { if (m.cities == 0 && m.settlements > 0) m.stabilityDelta += 1; }
            case EMPLOYMENT -> { if (m.population > 10) m.prosperityDelta += 1; }
            case UNEMPLOYMENT -> { if (m.population > 0 && m.settlements == 1 && m.population > 60) m.stabilityDelta -= 1; }
            case WAGES -> { if (m.prosperity > 55) m.stabilityDelta += 1; }
            case INFLATION -> { if (m.treasury > m.population * 20) m.prosperityDelta -= 1; }
            case PRICE_STABILITY -> { if (m.treasury >= m.population) m.prosperityDelta += 1; }

            case FARMING -> { m.prosperityDelta += m.population > 8 ? 1 : 0; }
            case MINING -> { m.treasuryDelta += m.population / 30; }
            case FORESTRY -> { m.prosperityDelta += m.settlements > 0 ? 1 : 0; }
            case CRAFTING -> { m.treasuryDelta += m.towns; }
            case BLACKSMITHS -> { m.prosperityDelta += m.cities > 0 ? 1 : 0; }
            case MERCHANTS -> { m.treasuryDelta += m.settlements / 2; }
            case BUILDERS -> { if (m.population > 20) m.prosperityDelta += 1; }
            case SCHOLARS -> { if (m.population >= 50) m.legitimacyDelta += 1; }
            case SOLDIERS -> { if (m.population >= 25) m.stabilityDelta += 1; }
            case ENGINEERS -> { if (m.population >= 100) m.prosperityDelta += 1; }

            case EDUCATION -> { if (m.population >= 20) m.legitimacyDelta += 1; }
            case LITERACY -> { if (m.population >= 50) m.prosperityDelta += 1; }
            case APPRENTICES -> { if (m.towns > 0) m.prosperityDelta += 1; }
            case UNIVERSITIES -> { if (m.cities > 0) m.legitimacyDelta += 1; }
            case INVENTION -> { if (m.population >= 100) m.prosperityDelta += 1; }
            case TECHNOLOGY_SPILLOVER -> { if (m.cities > 1) m.prosperityDelta += 1; }
            case ADMINISTRATION -> { if (m.population >= 100) m.treasuryDelta += 2; }
            case RECORD_KEEPING -> { if (m.population >= 50) m.legitimacyDelta += 1; }
            case CALENDAR -> { if (m.settlements > 0) m.prosperityDelta += 1; }
            case ENGINEERING -> { if (m.population >= 150) m.prosperityDelta += 1; }

            case FESTIVALS -> { if (m.prosperity > 40) m.stabilityDelta += 1; }
            case HOLIDAYS -> { if (m.legitimacy > 50) m.stabilityDelta += 1; }
            case CULTURE -> { if (m.population >= 20) m.legitimacyDelta += 1; }
            case ARTISANS -> { if (m.towns > 0) m.prosperityDelta += 1; }
            case MONUMENTS -> { if (m.cities > 0) m.legitimacyDelta += 1; }
            case DYNASTY -> { if ("MONARCHY".equalsIgnoreCase(m.government)) m.legitimacyDelta += 1; }
            case COUNCIL -> { if (!"MONARCHY".equalsIgnoreCase(m.government)) m.legitimacyDelta += 1; }
            case LOCAL_ELITES -> { if (m.towns > 1) m.stabilityDelta += 1; }
            case CIVIC_PRIDE -> { if (m.legitimacy > 60) m.stabilityDelta += 1; }
            case FESTIVAL_COMMERCE -> { if (m.settlements > 1) m.treasuryDelta += 1; }

            case BORDER_SECURITY -> { if (m.settlements > 1) m.stabilityDelta += 1; }
            case FRONTIER_COST -> { if (m.settlements > 3) m.treasuryDelta -= m.settlements / 2; }
            case OUTPOSTS -> { if (m.population >= 50) m.prosperityDelta += 1; }
            case TERRITORY_VALUE -> { if (m.settlements > 0) m.treasuryDelta += m.settlements / 2; }
            case BORDER_TRADE -> { if (m.settlements >= 2) m.treasuryDelta += 1; }
            case BORDER_DISPUTES -> { if (m.settlements >= 5 && m.stability < 60) m.stabilityDelta -= 1; }
            case DIPLOMACY -> { if (m.settlements >= 2) m.legitimacyDelta += 1; }
            case ALLIANCES -> { if (m.settlements >= 3) m.prosperityDelta += 1; }
            case EMBASSIES -> { if (m.cities > 0) m.treasuryDelta -= 1; }
            case TRIBUTE -> { if (m.legitimacy >= 80) m.treasuryDelta += 1; }

            case ARMY_UPKEEP -> { if (m.population >= 25) m.treasuryDelta -= Math.max(1, m.population / 100); }
            case MILITARY_READINESS -> { if (m.population >= 25) m.stabilityDelta += 1; }
            case TRAINING_COST -> { if (m.population >= 50) m.treasuryDelta -= 1; }
            case GARRISONS -> { if (m.cities > 0) m.treasuryDelta -= m.cities; }
            case FORTIFICATIONS -> { if (m.cities > 0) m.stabilityDelta += 1; }
            case SIEGE_PREPARATION -> { if (m.population >= 100) m.treasuryDelta -= 1; }
            case MOBILIZATION -> { if (m.stability < 40) m.treasuryDelta -= 1; }
            case VETERANS -> { if (m.population >= 100) m.legitimacyDelta += 1; }
            case COMMAND_STRUCTURE -> { if (m.population >= 150) m.stabilityDelta += 1; }
            case WAR_FATIGUE -> { if (m.stability < 35) m.prosperityDelta -= 2; }

            case MIGRATION -> { if (m.prosperity > 65) m.prosperityDelta += 1; }
            case IMMIGRATION -> { if (m.prosperity > 75) m.populationDelta += 1; }
            case EMIGRATION -> { if (m.prosperity < 20 && m.population > 50) m.populationDelta -= 1; }
            case FAMILY_GROWTH -> { if (m.prosperity > 55) m.populationDelta += 1; }
            case CHILD_SURVIVAL -> { if (m.stability > 70) m.populationDelta += 1; }
            case ELDER_SUPPORT -> { if (m.legitimacy > 50) m.stabilityDelta += 1; }
            case HOUSING_EXPANSION -> { if (m.population > m.settlements * 30) m.treasuryDelta -= 1; }
            case NEW_SETTLEMENT_PRESSURE -> { if (m.population > 80 && m.settlements == 1) m.prosperityDelta += 1; }
            case CITY_ATTRACTION -> { if (m.cities > 0) m.populationDelta += 1; }
            case FRONTIER_MIGRATION -> { if (m.settlements > 2 && m.prosperity > 60) m.populationDelta += 1; }

            case CRIME_PREVENTION -> { if (m.stability > 60) m.legitimacyDelta += 1; }
            case LAW_ENFORCEMENT -> { if (m.population >= 30) m.stabilityDelta += 1; }
            case COURTS -> { if (m.population >= 80) m.legitimacyDelta += 1; }
            case PRISONS -> { if (m.population >= 150) m.treasuryDelta -= 1; }
            case TAX_COLLECTORS -> { if (m.population >= 50) m.treasuryDelta += 1; }
            case CORRUPTION -> { if (m.tax > 40) m.treasuryDelta -= 1; }
            case AUDITS -> { if (m.legitimacy >= 70) m.treasuryDelta += 1; }
            case PUBLIC_SERVICES -> { if (m.treasury > 100) { m.treasuryDelta -= 1; m.stabilityDelta += 1; } }
            case GRANARIES -> { if (m.population >= 50) m.stabilityDelta += 1; }
            case WAREHOUSES -> { if (m.towns > 0) m.prosperityDelta += 1; }

            case ROAD_MAINTENANCE -> { if (m.settlements > 1) m.treasuryDelta -= 1; }
            case BRIDGES -> { if (m.settlements > 2) m.prosperityDelta += 1; }
            case CARAVANS -> { if (m.settlements > 1) m.treasuryDelta += 1; }
            case ROAD_TOLLS -> { if (m.settlements > 2) m.treasuryDelta += 1; }
            case PORTS -> { if (m.cities > 1) m.prosperityDelta += 1; }
            case MARKET_ROUTES -> { if (m.settlements > 2) m.prosperityDelta += 1; }
            case TRAVEL_NETWORK -> { if (m.settlements > 1) m.prosperityDelta += 1; }
            case MESSENGER_SERVICE -> { if (m.population >= 100) m.treasuryDelta -= 1; }
            case POSTAL_NETWORK -> { if (m.population >= 200) m.legitimacyDelta += 1; }
            case ROAD_CONGESTION -> { if (m.settlements >= 8) m.prosperityDelta -= 1; }

            case ROYAL_ASSISTANT -> { if (m.population >= 100) m.legitimacyDelta += 1; }
            case ROYAL_COURT -> { if (m.population >= 200) m.treasuryDelta -= 1; }
            case COUNCIL_MEETINGS -> { if (m.population >= 100) m.stabilityDelta += 1; }
            case ROYAL_ORDERS -> { if (m.population >= 100) m.legitimacyDelta += 1; }
            case MESSENGER_ETA -> { if (m.population >= 100) m.prosperityDelta += 1; }
            case DIPLOMATIC_LETTERS -> { if (m.settlements >= 2) m.legitimacyDelta += 1; }
            case SUCCESSION -> { if (m.population >= 100) m.legitimacyDelta += 1; }
            case INHERITANCE -> { if (m.population >= 50) m.prosperityDelta += 1; }
            case CENSUS -> { if (m.population > 0) m.legitimacyDelta += 1; }
            case YEARLY_BUDGET -> { if (m.population >= 25) m.treasuryDelta += 1; }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Metrics {
        int population, settlements, cities, towns, stability, treasury, tax, prosperity, legitimacy;
        int treasuryDelta, stabilityDelta, prosperityDelta, legitimacyDelta, populationDelta, localTax;
        String government = "MONARCHY";
    }

    private enum Rule {
        TAX_BASE, TAX_PRESSURE, LOW_TAX_PROSPERITY, POPULATION_TAX, SETTLEMENT_TAX, CITY_REVENUE, TOWN_REVENUE, TRADE_REVENUE, MARKET_ACTIVITY, ROAD_COMMERCE,
        FOOD_SECURITY, FOOD_SURPLUS, HOUSING_PRESSURE, URBAN_DENSITY, RURAL_SUPPORT, EMPLOYMENT, UNEMPLOYMENT, WAGES, INFLATION, PRICE_STABILITY,
        FARMING, MINING, FORESTRY, CRAFTING, BLACKSMITHS, MERCHANTS, BUILDERS, SCHOLARS, SOLDIERS, ENGINEERS,
        EDUCATION, LITERACY, APPRENTICES, UNIVERSITIES, INVENTION, TECHNOLOGY_SPILLOVER, ADMINISTRATION, RECORD_KEEPING, CALENDAR, ENGINEERING,
        FESTIVALS, HOLIDAYS, CULTURE, ARTISANS, MONUMENTS, DYNASTY, COUNCIL, LOCAL_ELITES, CIVIC_PRIDE, FESTIVAL_COMMERCE,
        BORDER_SECURITY, FRONTIER_COST, OUTPOSTS, TERRITORY_VALUE, BORDER_TRADE, BORDER_DISPUTES, DIPLOMACY, ALLIANCES, EMBASSIES, TRIBUTE,
        ARMY_UPKEEP, MILITARY_READINESS, TRAINING_COST, GARRISONS, FORTIFICATIONS, SIEGE_PREPARATION, MOBILIZATION, VETERANS, COMMAND_STRUCTURE, WAR_FATIGUE,
        MIGRATION, IMMIGRATION, EMIGRATION, FAMILY_GROWTH, CHILD_SURVIVAL, ELDER_SUPPORT, HOUSING_EXPANSION, NEW_SETTLEMENT_PRESSURE, CITY_ATTRACTION, FRONTIER_MIGRATION,
        CRIME_PREVENTION, LAW_ENFORCEMENT, COURTS, PRISONS, TAX_COLLECTORS, CORRUPTION, AUDITS, PUBLIC_SERVICES, GRANARIES, WAREHOUSES,
        ROAD_MAINTENANCE, BRIDGES, CARAVANS, ROAD_TOLLS, PORTS, MARKET_ROUTES, TRAVEL_NETWORK, MESSENGER_SERVICE, POSTAL_NETWORK, ROAD_CONGESTION,
        ROYAL_ASSISTANT, ROYAL_COURT, COUNCIL_MEETINGS, ROYAL_ORDERS, MESSENGER_ETA, DIPLOMATIC_LETTERS, SUCCESSION, INHERITANCE, CENSUS, YEARLY_BUDGET
    }
}
