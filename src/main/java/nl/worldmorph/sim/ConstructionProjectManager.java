package nl.worldmorph.sim;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.worldmorph.data.WorldMorphState;

/**
 * Places every structure one block at a time while nearby villagers walk to the work site.
 * Projects are serialized through a queue so separate buildings do not overlap in progress.
 */
public final class ConstructionProjectManager {
    private enum Kind { HOUSE, BANK, CASTLE, MONUMENT }
    private record Placement(BlockPos pos, BlockState state) {}
    private static final class Project {
        final Kind kind;
        final UUID settlementId;
        final UUID kingdomId;
        final String name;
        final BlockPos base;
        final int castlePhase;
        final List<Placement> blocks;
        int index;
        int cooldown;
        Project(Kind kind, UUID settlementId, UUID kingdomId, String name, BlockPos base,
                int castlePhase, List<Placement> blocks) {
            this.kind = kind; this.settlementId = settlementId; this.kingdomId = kingdomId;
            this.name = name; this.base = base; this.castlePhase = castlePhase; this.blocks = blocks;
        }
    }

    private final Queue<Project> queue = new ArrayDeque<>();
    private final Map<UUID, Integer> pendingHouses = new HashMap<>();
    private final Map<UUID, Integer> completedCastlePhases = new HashMap<>();
    private final Set<String> queuedCastlePhases = new HashSet<>();
    private final Set<UUID> monumentSettlements = new HashSet<>();
    private final Set<UUID> bankSettlements = new HashSet<>();
    private static final int[] CASTLE_POPULATION = {100, 200, 350, 500, 750};
    private static final int[] CASTLE_COST = {250, 400, 650, 900, 1300};

    public int pendingHouses(UUID settlementId) {
        return pendingHouses.getOrDefault(settlementId, 0);
    }

    public void requestHouse(WorldMorphState.SettlementData settlement, BlockPos base, int tier) {
        pendingHouses.merge(settlement.id(), 1, Integer::sum);
        queue.add(new Project(Kind.HOUSE, settlement.id(), settlement.kingdomId(),
                settlement.name() + " house", base, 0, housePlan(base, tier)));
    }

    public void requestBankIfReady(ServerLevel level, WorldMorphState state, WorldMorphState.SettlementData settlement) {
        if (settlement.population() < 100 || bankSettlements.contains(settlement.id())) return;
        boolean existing = state.history().stream().anyMatch(e ->
                e.type().equals("BANK_BUILT") && e.description().contains(settlement.id().toString()));
        boolean queued = queue.stream().anyMatch(p -> p.kind == Kind.BANK && p.settlementId.equals(settlement.id()));
        if (existing || queued) { bankSettlements.add(settlement.id()); return; }
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                settlement.center().getX() + 8, settlement.center().getZ());
        BlockPos base = new BlockPos(settlement.center().getX() + 8, y, settlement.center().getZ());
        queue.add(new Project(Kind.BANK, settlement.id(), settlement.kingdomId(),
                settlement.name() + " bank", base, 0, bankPlan(base)));
        state.history("BANK_ORDERED", "Settlement " + settlement.id() + " ordered citizens to build a bank for tax administration.");
    }

    public void requestMonument(ServerLevel level, WorldMorphState state, WorldMorphState.SettlementData settlement) {
        if (settlement.population() < 12 || monumentSettlements.contains(settlement.id())
                || state.history().stream().anyMatch(e -> e.type().equals("MONUMENT_RAISED")
                && e.description().contains(settlement.id().toString()))) return;
        boolean queued = queue.stream().anyMatch(p -> p.kind == Kind.MONUMENT && p.settlementId.equals(settlement.id()));
        if (queued) return;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                settlement.center().getX(), settlement.center().getZ());
        BlockPos base = new BlockPos(settlement.center().getX(), y, settlement.center().getZ());
        List<Placement> blocks = new ArrayList<>();
        for (int h = 0; h < 3; h++) add(blocks, base.above(h), state(Blocks.STONE_BRICKS));
        add(blocks, base.above(3), state(Blocks.GOLD_BLOCK));
        queue.add(new Project(Kind.MONUMENT, settlement.id(), settlement.kingdomId(),
                settlement.name() + " monument", base, 0, blocks));
    }

    public void requestCastleIfReady(ServerLevel level, WorldMorphState state, WorldMorphState.SettlementData settlement) {
        UUID kingdomId = settlement.kingdomId();
        int population = state.settlements().values().stream()
                .filter(s -> s.kingdomId().equals(kingdomId))
                .mapToInt(WorldMorphState.SettlementData::population).sum();
        int completed = Math.max(completedCastlePhases.getOrDefault(kingdomId, 0),
                historyCastlePhase(state, kingdomId));
        if (completed >= 5) return;
        int phase = completed + 1;
        if (population < CASTLE_POPULATION[phase - 1]) return;
        String key = kingdomId + ":" + phase;
        if (queuedCastlePhases.contains(key)) return;
        WorldMorphState.SettlementData capital = state.settlements().values().stream()
                .filter(s -> s.kingdomId().equals(kingdomId))
                .max(Comparator.comparingInt(WorldMorphState.SettlementData::population)).orElse(null);
        if (capital == null || !capital.id().equals(settlement.id())) return;
        boolean alreadyStarted = state.history().stream().anyMatch(event ->
                event.type().equals("CASTLE_PHASE_STARTED")
                        && event.description().contains(kingdomId.toString())
                        && event.description().contains("phase " + phase + " "));
        if (!alreadyStarted && !state.spendTreasury(kingdomId, CASTLE_COST[phase - 1], "castle phase " + phase)) return;
        BlockPos castleXZ = capital.center().offset(12, 0, 12);
        int castleY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, castleXZ.getX(), castleXZ.getZ());
        BlockPos base = new BlockPos(castleXZ.getX(), castleY, castleXZ.getZ());
        queue.add(new Project(Kind.CASTLE, capital.id(), kingdomId, capital.name() + " castle",
                base, phase, castlePlan(base, phase)));
        queuedCastlePhases.add(key);
        state.history("CASTLE_PHASE_STARTED", "Kingdom " + kingdomId + ": castle phase " + phase + " construction started for " + capital.name());
    }

    public void tick(ServerLevel level, WorldMorphState state, HousingManager housing) {
        Project project = queue.peek();
        if (project == null) return;
        if (project.cooldown > 0) { project.cooldown--; return; }

        List<Villager> builders = level.getEntitiesOfClass(Villager.class,
                new net.minecraft.world.phys.AABB(project.base).inflate(48),
                v -> v.isAlive() && !v.isBaby()).stream().limit(3).toList();
        if (builders.isEmpty()) return;

        BlockPos staging = project.kind == Kind.HOUSE || project.kind == Kind.BANK
                ? project.base.offset(-2, 0, 3)
                : project.base.offset(-2, 0, 12);
        int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                staging.getX(), staging.getZ());
        staging = new BlockPos(staging.getX(), sy, staging.getZ());
        boolean arrived = false;
        for (Villager villager : builders) {
            if (villager.blockPosition().distSqr(staging) > 9) {
                villager.getNavigation().moveTo(staging.getX() + 0.5, staging.getY(), staging.getZ() + 0.5, 0.95D);
            } else {
                arrived = true;
                villager.getNavigation().stop();
            }
        }
        if (!arrived) return;

        int placed = 0;
        while (project.index < project.blocks.size() && placed < 1) {
            Placement placement = project.blocks.get(project.index++);
            if (level.getBlockState(placement.pos()).isAir() || level.getBlockState(placement.pos()).canBeReplaced()) {
                level.setBlock(placement.pos(), placement.state(), 3);
                placed++;
            }
        }
        project.cooldown = 5;
        if (project.index >= project.blocks.size()) {
            queue.remove();
            if (project.kind == Kind.HOUSE) {
                pendingHouses.compute(project.settlementId, (id, n) -> Math.max(0, (n == null ? 1 : n) - 1));
                housing.addHouse(project.settlementId, 4);
                state.history("HOUSE_BUILT", project.name + " was completed block by block by villagers.");
            } else if (project.kind == Kind.BANK) {
                bankSettlements.add(project.settlementId);
                state.history("BANK_BUILT", "Settlement " + project.settlementId + ": " + project.name + " was completed block by block by citizens.");
            } else if (project.kind == Kind.CASTLE) {
                completedCastlePhases.put(project.kingdomId, project.castlePhase);
                queuedCastlePhases.remove(project.kingdomId + ":" + project.castlePhase);
                state.history("CASTLE_PHASE_" + project.castlePhase,
                        "Kingdom " + project.kingdomId + ": " + project.name + " completed castle phase " + project.castlePhase + ".");
            } else {
                monumentSettlements.add(project.settlementId);
                state.history("MONUMENT_RAISED", "Settlement " + project.settlementId + ": " + project.name + " was built block by block by villagers.");
            }
        }
    }

    private int historyCastlePhase(WorldMorphState state, UUID kingdomId) {
        int max = 0;
        for (WorldMorphState.HistoryEvent event : state.history()) {
            if (event.type().startsWith("CASTLE_PHASE_") && event.description().contains(kingdomId.toString())) {
                try { max = Math.max(max, Integer.parseInt(event.type().substring("CASTLE_PHASE_".length()))); }
                catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    private List<Placement> bankPlan(BlockPos b) {
        List<Placement> out = new ArrayList<>();
        BlockState foundation = state(Blocks.STONE_BRICKS);
        BlockState wall = state(Blocks.POLISHED_ANDESITE);
        BlockState roof = state(Blocks.DARK_OAK_PLANKS);
        for (int x=0;x<9;x++) for(int z=0;z<7;z++) add(out,b.offset(x,0,z),foundation);
        for (int y=1;y<=4;y++) for(int x=0;x<9;x++) for(int z=0;z<7;z++) {
            boolean edge=x==0||x==8||z==0||z==6;
            if(!edge) continue;
            boolean door=z==0&&x>=3&&x<=5&&y<=2;
            boolean window=y==2&&((z==0||z==6)&&(x==1||x==7));
            add(out,b.offset(x,y,z),door?state(Blocks.AIR):window?state(Blocks.GLASS_PANE):wall);
        }
        for(int x=-1;x<=9;x++) for(int z=-1;z<=7;z++) {
            if(x==-1||x==9||z==-1||z==7) add(out,b.offset(x,5,z),roof);
        }
        add(out,b.offset(4,1,3),state(Blocks.CHEST));
        add(out,b.offset(3,1,3),state(Blocks.LECTERN));
        add(out,b.offset(5,1,3),state(Blocks.BARREL));
        add(out,b.offset(4,2,0),state(Blocks.LANTERN));
        return out;
    }

    private List<Placement> housePlan(BlockPos b, int tier) {
        List<Placement> out = new ArrayList<>();
        BlockState floor = state(tier == 0 ? Blocks.COBBLESTONE : tier == 1 ? Blocks.STONE_BRICKS : Blocks.POLISHED_ANDESITE);
        BlockState wall = state(tier == 0 ? Blocks.OAK_PLANKS : tier == 1 ? Blocks.SPRUCE_PLANKS : Blocks.DARK_OAK_PLANKS);
        BlockState log = state(tier == 0 ? Blocks.OAK_LOG : tier == 1 ? Blocks.SPRUCE_LOG : Blocks.DARK_OAK_LOG);
        BlockState roof = state(tier == 0 ? Blocks.SPRUCE_STAIRS : tier == 1 ? Blocks.STONE_BRICK_STAIRS : Blocks.DEEPSLATE_BRICK_STAIRS);
        for (int x=0;x<7;x++) for(int z=0;z<7;z++) add(out,b.offset(x,0,z),floor);
        for (int y=1;y<=3;y++) for(int x=0;x<7;x++) for(int z=0;z<7;z++) {
            boolean edge=x==0||x==6||z==0||z==6;
            if (!edge) continue;
            if (z==0 && x==3 && y<=2) continue;
            boolean corner=(x==0||x==6)&&(z==0||z==6);
            boolean window=y==2 && (((z==0||z==6)&&(x==2||x==4))||((x==0||x==6)&&(z==2||z==4)));
            add(out,b.offset(x,y,z),corner?log:window?state(Blocks.GLASS_PANE):wall);
        }
        for(int layer=0;layer<3;layer++) {
            int min=layer,max=6-layer,y=4+layer;
            for(int x=min;x<=max;x++){add(out,b.offset(x,y,min),roof);add(out,b.offset(x,y,max),roof);}
            for(int z=min+1;z<max;z++){add(out,b.offset(min,y,z),roof);add(out,b.offset(max,y,z),roof);}
        }
        Block ridge=tier==0?Blocks.SPRUCE_PLANKS:tier==1?Blocks.STONE_BRICKS:Blocks.POLISHED_DEEPSLATE;
        for(int x=2;x<=4;x++) add(out,b.offset(x,6,3),state(ridge));
        if(tier>=1){add(out,b.offset(3,1,3),state(Blocks.CRAFTING_TABLE));add(out,b.offset(2,1,3),state(Blocks.BARREL));}
        if(tier>=2){add(out,b.offset(4,1,3),state(Blocks.LANTERN));add(out,b.offset(3,1,4),state(Blocks.BOOKSHELF));}
        return out;
    }

    private List<Placement> castlePlan(BlockPos b, int phase) {
        List<Placement> out = new ArrayList<>();
        BlockState stone=state(Blocks.STONE_BRICKS), dark=state(Blocks.DEEPSLATE_BRICKS);
        int radius = phase >= 3 ? 34 : 24;
        int height = phase >= 4 ? 8 : 5;
        for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
            if(Math.abs(x)!=radius && Math.abs(z)!=radius) continue;
            for(int y=0;y<height;y++) {
                if (z==-radius && x>=-2 && x<=2 && y<3) continue;
                add(out,b.offset(x,y,z),stone);
            }
            if((x+z)%2==0) add(out,b.offset(x,height,z),dark);
        }
        int towerHeight = 8 + phase*2;
        int[][] corners={{-radius,-radius},{-radius,radius},{radius,-radius},{radius,radius}};
        for(int[] c:corners) for(int y=0;y<towerHeight;y++) for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            if(Math.abs(dx)==1||Math.abs(dz)==1||y==towerHeight-1)
                add(out,b.offset(c[0]+dx,y,c[1]+dz), y>=towerHeight-2?dark:stone);
        }
        // Inner keep is the original phase; later phases visibly raise and reinforce it.
        int keepRadius=phase>=4?9:6;
        int keepHeight=phase>=5?22:phase>=4?16:9+phase;
        for(int x=-keepRadius;x<=keepRadius;x++) for(int z=-keepRadius;z<=keepRadius;z++) {
            if(Math.abs(x)!=keepRadius && Math.abs(z)!=keepRadius) continue;
            for(int y=0;y<keepHeight;y++) add(out,b.offset(x,y,z), (y>=keepHeight-2)?dark:stone);
        }
        if(phase>=2) {
            for(int x=-radius-8;x<=radius+8;x++) for(int z=-radius-8;z<=radius+8;z++) {
                if(Math.abs(x)!=radius+8 && Math.abs(z)!=radius+8) continue;
                for(int y=0;y<(phase>=4?5:3);y++) add(out,b.offset(x,y,z),stone);
            }
        }
        if(phase>=3) {
            for(int x=-radius-3;x<=radius+3;x+=2) for(int z=-radius-3;z<=radius+3;z+=2)
                add(out,b.offset(x, height+1, z),state(Blocks.LANTERN));
        }
        if(phase>=4) {
            for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++)
                add(out,b.offset(x,keepHeight,z),state(Blocks.STONE_BRICK_SLAB));
        }
        if(phase>=5) {
            for(int y=keepHeight+1;y<=keepHeight+6;y++) add(out,b.offset(0,y,0),state(Blocks.GOLD_BLOCK));
            for(int x=-2;x<=2;x++) add(out,b.offset(x,keepHeight+7,0),state(Blocks.GOLD_BLOCK));
        }
        return out;
    }

    private static BlockState state(Block block) { return block.defaultBlockState(); }
    private static void add(List<Placement> out, BlockPos pos, BlockState state) {
        out.add(new Placement(pos.immutable(), state));
    }
}
