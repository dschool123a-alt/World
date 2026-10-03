package nl.worldmorph.sim;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.worldmorph.npc.NpcManager;
import nl.worldmorph.npc.NpcProfile;
import nl.worldmorph.data.WorldMorphState;

public final class DayOneArrivalManager {
    private boolean arrived;
    private final Map<UUID, UUID> followers = new HashMap<>();
    private final Set<UUID> arrivalGroup = new LinkedHashSet<>();
    private final Map<UUID, Worker> workers = new HashMap<>();
    private BlockPos constructionCenter;
    private UUID settlementId;
    private int wood;
    private int stone;

    private enum Phase { FOLLOW, GATHER_WOOD, GATHER_STONE, BUILD }
    private static final class Worker {
        Phase phase = Phase.FOLLOW;
        BlockPos target;
        int buildIndex;
        int workCooldown;
    }

    public void tick(ServerLevel level, WorldMorphState state) {
        if (!arrived && state.getSimulationTick() <= 24000) spawnIfReady(level, state);
        for (var entry : new ArrayList<>(followers.entrySet())) {
            var entity = level.getEntity(entry.getKey());
            var player = level.getServer().getPlayerList().getPlayer(entry.getValue());
            if (entity instanceof Villager villager && player != null && villager.isAlive()) {
                double distance = villager.distanceTo(player);
                if (distance > 3.0D) villager.getNavigation().moveTo(player, 1.15D);
                else villager.getNavigation().stop();
            } else {
                followers.remove(entry.getKey());
            }
        }
        tickConstruction(level, state);
    }

    private void spawnIfReady(ServerLevel level, WorldMorphState state) {
        long time = level.getDayTime() % 24000L;
        if (time < 1000L || time > 12000L) return;
        ServerPlayer player = level.players().stream().filter(p -> !p.isSpectator()).findFirst().orElse(null);
        if (player == null || player.blockPosition().getY() < level.getSeaLevel() - 8) return;
        if (!level.canSeeSky(player.blockPosition())) return;

        var pos = player.blockPosition().offset(6, 0, 6);
        if (!level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(12)).isEmpty()) {
            arrived = true;
            return;
        }

        String[] names = {"Settler", "Builder", "Farmer"};
        for (int i = 0; i < 3; i++) {
            Villager villager = new Villager(level);
            villager.moveTo(pos.getX() + i * 1.5, pos.getY() + 1, pos.getZ(), 0, 0);
            villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
            villager.setCustomName(net.minecraft.network.chat.Component.literal(names[i]));
            villager.setCustomNameVisible(true);
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(villager);
            arrivalGroup.add(villager.getUUID());
        }
        state.history("DAY_ONE_ARRIVAL", "Three settlers arrived near the player on day one.");
        arrived = true;
    }

    private void tickConstruction(ServerLevel level, WorldMorphState state) {
        if (constructionCenter == null || workers.isEmpty()) return;
        if (wood >= 8 && stone >= 8) {
            for (Worker worker : workers.values()) worker.phase = Phase.BUILD;
        }
        for (var entry : new ArrayList<>(workers.entrySet())) {
            var entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Villager villager) || !villager.isAlive()) continue;
            Worker worker = entry.getValue();
            if (worker.workCooldown > 0) { worker.workCooldown--; continue; }
            if (worker.phase == Phase.GATHER_WOOD || worker.phase == Phase.GATHER_STONE) {
                if (worker.target == null || level.getBlockState(worker.target).isAir()) {
                    worker.target = findResource(level, villager.blockPosition(), worker.phase == Phase.GATHER_WOOD ? Blocks.OAK_LOG : Blocks.STONE);
                }
                if (worker.target == null) {
                    worker.workCooldown = 40;
                    continue;
                }
                if (villager.blockPosition().distSqr(worker.target) > 9) {
                    villager.getNavigation().moveTo(worker.target.getX()+0.5, worker.target.getY(), worker.target.getZ()+0.5, 1.0D);
                } else {
                    level.setBlock(worker.target, Blocks.AIR.defaultBlockState(), 3);
                    if (worker.phase == Phase.GATHER_WOOD) wood++; else stone++;
                    worker.target = null; worker.workCooldown = 10;
                    if (wood >= 8 && stone >= 8) {
                        for (Worker other : workers.values()) other.phase = Phase.BUILD;
                    }
                }
            } else if (worker.phase == Phase.BUILD) {
                BlockPos site = constructionBlock(worker.buildIndex);
                if (site == null) {
                    worker.phase = Phase.BUILD;
                    continue;
                }
                BlockPos workPos = new BlockPos(site.getX(), constructionCenter.getY() + 1, site.getZ());
                if (villager.blockPosition().distSqr(workPos) > 9) {
                    villager.getNavigation().moveTo(workPos.getX()+0.5, workPos.getY(), workPos.getZ()+0.5, 0.9D);
                } else {
                    net.minecraft.world.level.block.state.BlockState stateToPlace = constructionState(worker.buildIndex);
                    if (!stateToPlace.isAir()) level.setBlock(site, stateToPlace, 3);
                    worker.buildIndex += 3;
                    worker.workCooldown = 6;
                }
            }
        }
        if (workers.values().stream().allMatch(w -> w.phase == Phase.BUILD) && workers.values().stream().allMatch(w -> w.buildIndex >= 90)) {
            state.history("DAY_ONE_HOUSES_BUILT", "The first settlers gathered local materials and built their first homes.");
            workers.clear();
            constructionCenter = null;
        }
    }

    private BlockPos findResource(ServerLevel level, BlockPos origin, net.minecraft.world.level.block.Block block) {
        for (int r = 2; r <= 12; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                if (block == Blocks.STONE) {
                    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    BlockPos surface = new BlockPos(x, surfaceY, z);
                    if (level.getBlockState(surface).is(Blocks.STONE)
                            && level.getBlockState(surface.above()).isAir()) return surface;
                } else {
                    BlockPos p = new BlockPos(x, origin.getY(), z);
                    for (int y = -1; y <= 5; y++) {
                        BlockPos target = p.above(y);
                        if (level.getBlockState(target).is(block)
                                && level.getBlockState(target.above()).isAir()) return target;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos constructionBlock(int index) {
        if (constructionCenter == null || index >= 90) return null;
        if (index < 25) return constructionCenter.offset(index % 5, 0, index / 5);
        if (index < 45) return wallPosition(index - 25, 1);
        if (index < 65) return wallPosition(index - 45, 2);
        int roof = index - 65;
        int x = roof % 5, z = roof / 5;
        int inset = Math.min(Math.min(x, z), Math.min(4 - x, 4 - z));
        return constructionCenter.offset(x, 3 + inset, z);
    }

    private BlockPos wallPosition(int index, int y) {
        int side = index / 5, along = index % 5;
        return switch (side) {
            case 0 -> constructionCenter.offset(along, y, 0);
            case 1 -> constructionCenter.offset(4, y, along);
            case 2 -> constructionCenter.offset(4 - along, y, 4);
            default -> constructionCenter.offset(0, y, 4 - along);
        };
    }

    private net.minecraft.world.level.block.state.BlockState constructionState(int index) {
        if (index < 25) return Blocks.COBBLESTONE.defaultBlockState();
        if (index < 65) {
            int local = index < 45 ? index - 25 : index - 45;
            int side = local / 5, along = local % 5;
            boolean corner = along == 0 || along == 4;
            if (side == 0 && along == 2 && index < 45) return Blocks.AIR.defaultBlockState();
            if (index >= 45 && ((side == 0 || side == 2) && (along == 1 || along == 3)
                    || (side == 1 || side == 3) && (along == 1 || along == 3))) return Blocks.GLASS_PANE.defaultBlockState();
            return corner ? Blocks.OAK_LOG.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
        }
        return Blocks.SPRUCE_STAIRS.defaultBlockState();
    }

    public boolean follow(ServerLevel level, ServerPlayer player, UUID npcId) {
        if (!arrivalGroup.contains(npcId)) return false;
        var entity = level.getEntity(npcId);
        if (!(entity instanceof Villager villager) || !villager.isAlive()) return false;
        for (UUID id : arrivalGroup) {
            var groupEntity = level.getEntity(id);
            if (groupEntity instanceof Villager v && v.isAlive()) {
                followers.put(id, player.getUUID());
                v.getNavigation().moveTo(player, 1.15D);
            }
        }
        return true;
    }

    public boolean wait(UUID npcId) {
        return followers.remove(npcId) != null;
    }

    public boolean build(ServerLevel level, WorldMorphState state, NpcManager npcs, ServerPlayer player, UUID npcId) {
        var entity = level.getEntity(npcId);
        if (!(entity instanceof Villager villager) || !villager.isAlive()) return false;
        if (!arrivalGroup.contains(npcId)) return false;
        followers.keySet().removeIf(arrivalGroup::contains);
        var kingdom = state.kingdoms().values().stream()
            .filter(k -> k.leader().equals(player.getUUID()))
            .findFirst().orElseGet(() -> state.createKingdom(player.getName().getString() + "'s Realm", player.getUUID()));
        var center = player.blockPosition();
        var existing = state.settlements().values().stream()
            .filter(s -> s.center().distSqr(center) < 64 * 64)
            .findFirst();
        if (existing.isPresent()) return true;
        var settlement = state.createSettlement("New Settlement", center, kingdom.id());
        state.updateSettlement(settlement.withPopulation(3));
        settlementId = settlement.id();
        BlockPos buildXZ = center.offset(8, 0, 8);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, buildXZ.getX(), buildXZ.getZ());
        constructionCenter = new BlockPos(buildXZ.getX(), surfaceY, buildXZ.getZ());
        wood = 0;
        stone = 0;
        int workerIndex = 0;
        for (UUID id : arrivalGroup) {
            var found = level.getEntity(id);
            if (found instanceof Villager v && v.isAlive()) {
                String name = v.getCustomName() == null ? "Settler" : v.getCustomName().getString();
                NpcProfile profile = npcs.create(v.getUUID(), name);
                profile.setSettlement(settlement.id());
                profile.setKingdom(kingdom.id());
                profile.addMemory("SETTLEMENT_FOUNDER", null, state.getSimulationTick(), 8);
                Worker worker = new Worker();
                worker.phase = workerIndex == 0 ? Phase.GATHER_WOOD : Phase.GATHER_STONE;
                worker.buildIndex = workerIndex;
                workers.put(id, worker);
                workerIndex++;
            }
        }
        state.history("DAY_ONE_SETTLEMENT", "The first settlers founded New Settlement and began gathering materials.");
        return true;
    }
}
