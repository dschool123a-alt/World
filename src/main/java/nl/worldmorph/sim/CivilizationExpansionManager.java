package nl.worldmorph.sim;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.npc.NpcManager;
import nl.worldmorph.npc.NpcProfile;

/**
 * Connects the abstract simulation to visible Minecraft-world behavior.
 *
 * This manager deliberately keeps the expensive work on slow simulation intervals.
 */
public final class CivilizationExpansionManager {
    private static final long FAST = 1200;
    private static final long DAILY = 24000;
    private static final int MAX_HOUSE_BUILD = 3;
    private static final int HOUSE_COST = 25;
    private static final int ROAD_COST = 10;
    private final Set<UUID> seededSettlements = new HashSet<>();
    private final Set<UUID> castleBuiltKingdoms = new HashSet<>();
    private final ConstructionProjectManager constructionProjects = new ConstructionProjectManager();

    public void tick(ServerLevel level, WorldMorphState state, NpcManager npcs,
                     FamilyManager families, RelationshipManager relationships,
                     MarriageManager marriages, JobManager jobs, ResourceManager resources,
                     TechnologyManager technology, TerritoryManager territory,
                     RoadNetwork roads, HousingManager housing) {
        constructionProjects.tick(level, state, housing);
        long tick = state.getSimulationTick();
        if (tick % FAST == 0) {
            for (WorldMorphState.SettlementData settlement : List.copyOf(state.settlements().values())) {
                visiblePopulation(level, settlement, state, npcs, housing);
                createSocialBonds(settlement, npcs, relationships);
                formFamilies(settlement, npcs, families, relationships, marriages, state);
                assignJobs(settlement, npcs, jobs, state);
                produceResources(settlement, npcs, resources);
                consumeFood(settlement, state, resources);
                advanceTechnology(settlement, state, technology);
                claimTerritory(settlement, state, territory);
                buildNeededHomes(level, settlement, npcs, housing, state);
                maintainRoads(settlement, state, roads);
                enforcePopulationCap(settlement, state);
            }
        }
        if (tick % DAILY == 0) {
            for (WorldMorphState.SettlementData settlement : List.copyOf(state.settlements().values())) {
                syncPhysicalCitizens(level, settlement, state, npcs);
                growKingdom(level, settlement, state, npcs, families, housing, territory, roads);
                runAdvancedCycle(level, state, settlement, npcs, resources, housing);
                removeDeadCitizens(level, settlement, npcs);
            }
        }
    }

    // 1: visible population is reconciled with the abstract population.
    private void visiblePopulation(ServerLevel level, WorldMorphState.SettlementData s, WorldMorphState state,
                                   NpcManager npcs, HousingManager housing) {
        long alive = citizens(npcs, s.id()).stream().filter(NpcProfile::alive).count();
        int target = Math.max(s.population(), (int) alive);
        if (target != s.population()) state.updateSettlement(s.withPopulation(target));
        HousingManager.Housing h = housing.get(s.id());
        int missing = Math.max(0, (int) alive - h.residents());
        if (missing > 0 && h.capacity() >= h.residents() + missing) housing.moveIn(s.id(), missing);
    }

    // 2-4: relationship growth creates actual social links.
    private void createSocialBonds(WorldMorphState.SettlementData s, NpcManager npcs,
                                   RelationshipManager relationships) {
        List<NpcProfile> adults = citizens(npcs, s.id()).stream()
                .filter(p -> p.alive() && p.age() >= 16).limit(32).toList();
        for (int i = 0; i < adults.size(); i++) {
            for (int j = i + 1; j < adults.size(); j++) {
                NpcProfile a = adults.get(i), b = adults.get(j);
                int current = relationships.score(a, b);
                if (current < 35) {
                    relationships.change(a, b, 1);
                    relationships.change(b, a, 1);
                }
            }
        }
    }

    // 5-7: compatible adults marry and their families are merged.
    private void formFamilies(WorldMorphState.SettlementData s, NpcManager npcs,
                              FamilyManager families, RelationshipManager relationships,
                              MarriageManager marriages, WorldMorphState state) {
        List<NpcProfile> adults = citizens(npcs, s.id()).stream()
                .filter(p -> p.alive() && p.age() >= 18 && p.age() <= 45)
                .toList();
        for (int i = 0; i < adults.size(); i++) {
            NpcProfile a = adults.get(i);
            if (marriages.spouse(a.id()) != null) continue;
            for (int j = i + 1; j < adults.size(); j++) {
                NpcProfile b = adults.get(j);
                if (marriages.spouse(b.id()) != null) continue;
                if (a.familyId() != null && a.familyId().equals(b.familyId())) continue;
                if (relationships.score(a, b) < 30 || relationships.score(b, a) < 30) continue;
                if (marriages.marry(a, b, state.getSimulationTick())) {
                    if (a.familyId() == null) families.found(a, s.id());
                    if (b.familyId() == null) families.found(b, s.id());
                    UUID mergedFamily = a.familyId();
                    families.merge(a.familyId(), b.familyId(), a.id());
                    if (mergedFamily != null) families.get(mergedFamily).ifPresent(f -> f.members().forEach(id -> { NpcProfile member = npcs.get(id); if (member != null) member.setFamily(mergedFamily); }));
                    state.history("FAMILY_FORMED", a.name() + " and " + b.name() + " formed a family in " + s.name());
                    break;
                }
            }
        }
    }

    // 8-10: children become useful citizens through age/job transitions.
    private void assignJobs(WorldMorphState.SettlementData s, NpcManager npcs, JobManager jobs,
                            WorldMorphState state) {
        for (NpcProfile p : citizens(npcs, s.id())) {
            if (!p.alive()) continue;
            if (p.age() < 6) jobs.assign(p, JobType.UNEMPLOYED);
            else if (p.age() < 16) jobs.assign(p, JobType.UNEMPLOYED);
            else if ("UNEMPLOYED".equals(p.job())) {
                JobType[] choices = {JobType.FARMER, JobType.BUILDER, JobType.MERCHANT, JobType.SOLDIER};
                int index = Math.floorMod(p.id().hashCode(), choices.length);
                jobs.assign(p, choices[index]);
                p.addMemory("BECAME_WORKER", null, state.getSimulationTick(), 3);
            }
        }
    }

    // 11-13: workers generate a small local economy.
    private void produceResources(WorldMorphState.SettlementData s, NpcManager npcs, ResourceManager resources) {
        long farmers = citizens(npcs, s.id()).stream().filter(p -> p.alive() && "FARMER".equals(p.job())).count();
        long builders = citizens(npcs, s.id()).stream().filter(p -> p.alive() && "BUILDER".equals(p.job())).count();
        long merchants = citizens(npcs, s.id()).stream().filter(p -> p.alive() && "MERCHANT".equals(p.job())).count();
        long soldiers = citizens(npcs, s.id()).stream().filter(p -> p.alive() && "SOLDIER".equals(p.job())).count();
        resources.add(s.id(), ResourceManager.Resource.GRAIN, farmers * 2);
        resources.add(s.id(), ResourceManager.Resource.WOOD, Math.max(0, builders));
        resources.add(s.id(), ResourceManager.Resource.GOLD, Math.max(0, merchants));
        resources.add(s.id(), ResourceManager.Resource.IRON, Math.max(0, soldiers / 2));
        resources.add(s.id(), ResourceManager.Resource.FOOD, farmers);
        resources.add(s.id(), ResourceManager.Resource.TOOLS, Math.max(0, builders / 2));
    }

    // 14: food demand affects stability instead of allowing impossible infinite growth.
    private void consumeFood(WorldMorphState.SettlementData s, WorldMorphState state, ResourceManager resources) {
        long demand = Math.max(1, s.population());
        long available = resources.get(s.id(), ResourceManager.Resource.FOOD)
                + resources.get(s.id(), ResourceManager.Resource.GRAIN);
        if (available >= demand) {
            resources.consume(s.id(), ResourceManager.Resource.FOOD, Math.min(demand, resources.get(s.id(), ResourceManager.Resource.FOOD)));
            long rest = demand - Math.min(demand, resources.get(s.id(), ResourceManager.Resource.FOOD));
            if (rest > 0) resources.consume(s.id(), ResourceManager.Resource.GRAIN, rest);
        } else {
            state.kingdoms().values().stream().filter(k -> k.id().equals(s.kingdomId())).findFirst()
                    .ifPresent(k -> state.updateKingdom(k.withStability(k.stability() - 2)));
            state.history("FOOD_SHORTAGE", s.name() + " is short on food");
        }
    }

    // 15-16: technology unlocks follow settlement size.
    private void advanceTechnology(WorldMorphState.SettlementData s, WorldMorphState state,
                                   TechnologyManager technology) {
        int p = s.population();
        TechnologyManager.Tech[] techs = TechnologyManager.Tech.values();
        int amount = Math.min(techs.length, 1 + p / 15);
        for (int i = 0; i < amount; i++) technology.unlock(s.kingdomId(), techs[i]);
    }

    // 17: every settlement gets a territory claim that expands with population.
    private void claimTerritory(WorldMorphState.SettlementData s, WorldMorphState state, TerritoryManager territory) {
        int radius = Math.min(128, 12 + s.population() / 2);
        territory.claim(s.kingdomId(), s.center(), radius, state.getSimulationTick());
    }

    // 18-20: homes are queued and then physically placed one block at a time by villagers.
    private void buildNeededHomes(ServerLevel level, WorldMorphState.SettlementData s, NpcManager npcs,
                                  HousingManager housing, WorldMorphState state) {
        int citizens = (int) citizens(npcs, s.id()).stream().filter(NpcProfile::alive).count();
        int desired = Math.max(1, (int) Math.ceil(citizens / 4.0));
        int missing = Math.min(MAX_HOUSE_BUILD, Math.max(0,
                desired - housing.get(s.id()).houses() - constructionProjects.pendingHouses(s.id())));
        for (int i = 0; i < missing; i++) {
            BlockPos pos = findBuildPos(level, s.center(),
                    housing.get(s.id()).houses() + constructionProjects.pendingHouses(s.id()) + i + 1);
            if (pos == null) continue;
            if (!state.spendTreasury(s.kingdomId(), HOUSE_COST, "house construction")) continue;
            int kingdomPopulation = state.settlements().values().stream()
                    .filter(x -> x.kingdomId().equals(s.kingdomId()))
                    .mapToInt(WorldMorphState.SettlementData::population).sum();
            int houseTier = kingdomPopulation >= 50 ? 2 : kingdomPopulation >= 20 ? 1 : 0;
            constructionProjects.requestHouse(s, pos, houseTier);
        }
    }

    // 21: settlements get simple physical roads.
    private void maintainRoads(WorldMorphState.SettlementData s, WorldMorphState state, RoadNetwork roads) {
        if (roads.inSettlement(s.id()).isEmpty()) {
            BlockPos a = s.center();
            BlockPos b = s.center().offset(8, 0, 0);
            if (!state.spendTreasury(s.kingdomId(), ROAD_COST, "road construction")) return;
            roads.build(s.id(), a, b, 50);
            state.history("ROAD_STARTED", s.name() + " started a local road");
        }
    }

    // 22: stored population never becomes smaller than the living citizen count.
    private void enforcePopulationCap(WorldMorphState.SettlementData s, WorldMorphState state) {
        long living = state.settlements().values().stream()
                .filter(x -> x.id().equals(s.id())).mapToInt(WorldMorphState.SettlementData::population).sum();
        if (living < 0) state.updateSettlement(s.withPopulation(0));
    }

    // 23-27: spawn the abstract citizens as real villagers and keep names synchronized.
    private void syncPhysicalCitizens(ServerLevel level, WorldMorphState.SettlementData s,
                                      WorldMorphState state, NpcManager npcs) {
        for (NpcProfile p : citizens(npcs, s.id())) {
            if (!p.alive()) continue;
            Entity existing = level.getEntity(p.id());
            if (existing != null && existing.isAlive()) {
                existing.setCustomName(Component.literal(p.name()));
                existing.setCustomNameVisible(true);
                continue;
            }
            BlockPos pos = spawnPos(level, s.center(), p.id());
            var type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.fromNamespaceAndPath("minecraft", "villager")).orElseThrow();
            Entity villager = type.value().create(level, EntitySpawnReason.COMMAND);
            if (villager == null) continue;
            villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            villager.setUUID(p.id());
            villager.setCustomName(Component.literal(p.name()));
            villager.setCustomNameVisible(true);
            level.addFreshEntity(villager);
            p.addMemory("SPAWNED_IN_WORLD", null, state.getSimulationTick(), 2);
        }
    }

    // 28-30: dead abstract citizens stop existing physically.
    private void removeDeadCitizens(ServerLevel level, WorldMorphState.SettlementData s, NpcManager npcs) {
        for (NpcProfile p : citizens(npcs, s.id())) {
            if (p.alive()) continue;
            Entity e = level.getEntity(p.id());
            if (e != null) e.discard();
        }
    }

    // 31-38: new settlements are founded once a population has enough surplus.
    private void growKingdom(ServerLevel level, WorldMorphState.SettlementData s, WorldMorphState state,
                             NpcManager npcs, FamilyManager families, HousingManager housing,
                             TerritoryManager territory, RoadNetwork roads) {
        if (s.population() < 24 || seededSettlements.contains(s.id())) return;
        List<WorldMorphState.SettlementData> nearby = state.settlements().values().stream()
                .filter(other -> other.id().equals(s.id())
                        || !other.kingdomId().equals(s.kingdomId())
                        || distance(s.center(), other.center()) < 80)
                .toList();
        if (nearby.stream().anyMatch(other -> !other.id().equals(s.id())
                && other.kingdomId().equals(s.kingdomId())
                && distance(s.center(), other.center()) < 80)) return;

        BlockPos target = s.center().offset(48, 0, 48);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        target = new BlockPos(target.getX(), y, target.getZ());
        WorldMorphState.SettlementData child = state.createSettlement(s.name() + " Outpost", target, s.kingdomId());
        seededSettlements.add(s.id());
        territory.claim(s.kingdomId(), target, 24, state.getSimulationTick());
        roads.build(s.id(), s.center(), target, 35);
        state.history("SETTLEMENT_EXPANDED", s.name() + " founded " + child.name());

        List<NpcProfile> migrants = citizens(npcs, s.id()).stream()
                .filter(p -> p.alive() && p.age() >= 16 && p.ambition() > 40).limit(2).toList();
        for (NpcProfile migrant : migrants) {
            migrant.setSettlement(child.id());
            migrant.setFamily(null);
            families.found(migrant, child.id());
        }
        state.updateSettlement(child.withPopulation(Math.max(2, migrants.size())));
        housing.addHouse(child.id(), 4);
        int kingdomPopulation = state.settlements().values().stream()
                .filter(x -> x.kingdomId().equals(s.kingdomId()))
                .mapToInt(WorldMorphState.SettlementData::population).sum();
        buildHouse(level, target, kingdomPopulation >= 50 ? 2 : kingdomPopulation >= 20 ? 1 : 0);
    }

    // 39-50: lightweight long-term systems keep mature settlements from becoming static.
    private void runAdvancedCycle(ServerLevel level, WorldMorphState s, WorldMorphState.SettlementData settlement,
                                  NpcManager npcs, ResourceManager resources, HousingManager housing) {
        long day = s.getSimulationTick() / DAILY;
        int season = (int) ((day / 8) % 4);
        seasonalProduction(settlement, resources, season);
        treasuryIncome(s, settlement);
        payWages(settlement, npcs, resources);
        education(settlement, npcs, s);
        specialistJobs(settlement, npcs, s);
        if (day % 7 == 0) holdFestival(settlement, s);
        if (day % 5 == 0) caravan(settlement, s, resources);
        if (day % 12 == 0) immigration(settlement, s, npcs, housing);
        if (day % 20 == 0) migration(settlement, s, npcs);
        if (day % 30 == 0) monument(level, settlement, s);
        constructionProjects.requestCastleIfReady(level, s, settlement);
    }

    // 39: seasons change production without changing the core deterministic simulation.
    private void seasonalProduction(WorldMorphState.SettlementData s, ResourceManager r, int season) {
        if (season == 0) r.add(s.id(), ResourceManager.Resource.GRAIN, Math.max(1, s.population() / 4));
        if (season == 1) r.add(s.id(), ResourceManager.Resource.WOOD, Math.max(1, s.population() / 5));
        if (season == 2) r.add(s.id(), ResourceManager.Resource.FOOD, Math.max(1, s.population() / 4));
        if (season == 3) r.add(s.id(), ResourceManager.Resource.IRON, Math.max(1, s.population() / 10));
    }

    // 40: population now contributes predictable tax income.
    private void treasuryIncome(WorldMorphState state, WorldMorphState.SettlementData s) {
        state.kingdoms().values().stream().filter(k -> k.id().equals(s.kingdomId())).findFirst()
                .ifPresent(k -> state.updateKingdom(k.withTreasury(k.treasury() + Math.max(1, s.population() / 10))));
    }

    // 41: workers receive simulated wages instead of being purely decorative jobs.
    private void payWages(WorldMorphState.SettlementData s, NpcManager npcs, ResourceManager resources) {
        long workers = citizens(npcs, s.id()).stream().filter(p -> p.alive() && p.age() >= 16
                && !"UNEMPLOYED".equals(p.job())).count();
        long wage = Math.min(resources.get(s.id(), ResourceManager.Resource.GOLD), workers);
        if (wage > 0) {
            resources.consume(s.id(), ResourceManager.Resource.GOLD, wage);
            int paid = 0;
            for (NpcProfile p : citizens(npcs, s.id())) {
                if (paid >= wage) break;
                if (p.alive() && p.age() >= 16 && !"UNEMPLOYED".equals(p.job())) { p.changeMoney(1); paid++; }
            }
        }
    }

    // 42: children get an education memory and a small ambition boost.
    private void education(WorldMorphState.SettlementData s, NpcManager npcs, WorldMorphState state) {
        for (NpcProfile p : citizens(npcs, s.id())) {
            if (p.alive() && p.age() >= 6 && p.age() < 16 && p.memories().stream().noneMatch(m -> "SCHOOL".equals(m.type()))) {
                p.setAmbition(Math.min(100, p.ambition() + 2));
                p.addMemory("SCHOOL", null, state.getSimulationTick(), 2);
            }
        }
    }

    // 43: technology and age steer citizens toward useful specialist jobs.
    private void specialistJobs(WorldMorphState.SettlementData s, NpcManager npcs, WorldMorphState state) {
        boolean iron = state.kingdoms().containsKey(s.kingdomId()) && s.population() >= 15;
        for (NpcProfile p : citizens(npcs, s.id())) {
            if (!p.alive() || p.age() < 18) continue;
            if (iron && p.ambition() >= 80) p.setJob("BLACKSMITH");
            else if (p.ambition() <= 15 && p.age() >= 25) p.setJob("MINER");
        }
    }

    // 44: festivals improve stability and leave a visible historical record.
    private void holdFestival(WorldMorphState.SettlementData s, WorldMorphState state) {
        state.kingdoms().values().stream().filter(k -> k.id().equals(s.kingdomId())).findFirst()
                .ifPresent(k -> state.updateKingdom(k.withStability(k.stability() + 1)));
        state.history("LOCAL_FESTIVAL", s.name() + " held a local festival");
    }

    // 45: caravans move abstract gold between settlements in the same kingdom.
    private void caravan(WorldMorphState.SettlementData origin, WorldMorphState state, ResourceManager resources) {
        var destination = state.settlements().values().stream()
                .filter(x -> !x.id().equals(origin.id()) && x.kingdomId().equals(origin.kingdomId()))
                .findFirst().orElse(null);
        if (destination == null) return;
        long amount = Math.min(3, resources.get(origin.id(), ResourceManager.Resource.GOLD));
        if (amount > 0) {
            resources.consume(origin.id(), ResourceManager.Resource.GOLD, amount);
            resources.add(destination.id(), ResourceManager.Resource.GOLD, amount);
            state.history("CARAVAN_ARRIVED", origin.name() + " sent a trade caravan to " + destination.name());
        }
    }

    // 46: spare housing and food can attract a new resident.
    private void immigration(WorldMorphState.SettlementData s, WorldMorphState state, NpcManager npcs,
                             HousingManager housing) {
        HousingManager.Housing h = housing.get(s.id());
        if (h.capacity() <= h.residents() || s.population() >= h.capacity()) return;
        if (npcs.profiles().values().stream().filter(p -> s.id().equals(p.settlementId()) && p.alive()).count() >= s.population()) return;
        NpcProfile newcomer = npcs.createForSettlement("Settler_" + (npcs.size() + 1), s.id(), s.kingdomId(), state.getSimulationTick());
        newcomer.setAge(18 + Math.floorMod(newcomer.id().hashCode(), 30));
        newcomer.setLoyalty(55);
        housing.moveIn(s.id(), 1);
        state.updateSettlement(s.withPopulation(s.population() + 1));
        state.history("IMMIGRANT_ARRIVED", newcomer.name() + " joined " + s.name());
    }

    // 47: ambitious adults may relocate when a kingdom has multiple settlements.
    private void migration(WorldMorphState.SettlementData s, WorldMorphState state, NpcManager npcs) {
        var target = state.settlements().values().stream()
                .filter(x -> !x.id().equals(s.id()) && x.kingdomId().equals(s.kingdomId()) && x.population() < s.population())
                .findFirst().orElse(null);
        if (target == null) return;
        var migrant = citizens(npcs, s.id()).stream().filter(p -> p.alive() && p.age() >= 18 && p.ambition() >= 70).findFirst().orElse(null);
        if (migrant == null) return;
        migrant.setSettlement(target.id());
        migrant.setFamily(null);
        state.updateSettlement(s.withPopulation(Math.max(0, s.population() - 1)));
        state.updateSettlement(target.withPopulation(target.population() + 1));
        state.history("MIGRATION", migrant.name() + " moved from " + s.name() + " to " + target.name());
    }

    // 48-50: mature settlements queue a monument; villagers place every block.
    private void monument(ServerLevel level, WorldMorphState.SettlementData s, WorldMorphState state) {
        constructionProjects.requestMonument(level, s);
    }

    private BlockPos findBuildPos(ServerLevel level, BlockPos center, int index) {
        int radius = 6 + index * 4;
        for (int tries = 0; tries < 12; tries++) {
            int x = center.getX() + ((tries * 7 + radius) % (radius * 2 + 1)) - radius;
            int z = center.getZ() + ((tries * 11 + radius / 2) % (radius * 2 + 1)) - radius;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) return p;
        }
        return null;
    }

    private BlockPos spawnPos(ServerLevel level, BlockPos center, UUID id) {
        int hash = id.hashCode();
        int dx = Math.floorMod(hash, 9) - 4;
        int dz = Math.floorMod(hash / 9, 9) - 4;
        int x = center.getX() + dx, z = center.getZ() + dz;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private List<NpcProfile> citizens(NpcManager npcs, UUID settlement) {
        return npcs.profiles().values().stream().filter(p -> settlement.equals(p.settlementId())).toList();
    }

    private static int distance(BlockPos a, BlockPos b) {
        return (int) Math.sqrt(a.distSqr(b));
    }
}
