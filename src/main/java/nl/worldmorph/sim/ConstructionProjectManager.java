package nl.worldmorph.sim;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.npc.NpcManager;
import nl.worldmorph.npc.NpcProfile;

/**
 * Places every structure one block at a time while nearby villagers walk to the work site.
 * Projects are serialized through a queue so separate buildings do not overlap in progress.
 */
public final class ConstructionProjectManager {
    private enum Kind { HOUSE, BANK, CASTLE, MONUMENT, ROAD, WALL, MARKET, BLACKSMITH, LIBRARY, TOWN_HALL, FARM }
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
    private final Set<UUID> roadSettlements = new HashSet<>();
    private final Set<UUID> wallSettlements = new HashSet<>();
    private final Set<UUID> marketSettlements = new HashSet<>();
    private final Set<UUID> blacksmithSettlements = new HashSet<>();
    private final Set<UUID> librarySettlements = new HashSet<>();
    private final Set<UUID> townHallSettlements = new HashSet<>();
    private final Set<UUID> farmSettlements = new HashSet<>();
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

    public void requestRoadsIfReady(ServerLevel level, WorldMorphState state, WorldMorphState.SettlementData settlement) {
        if (settlement.population() < 5 || roadSettlements.contains(settlement.id())) return;
        boolean existing = state.history().stream().anyMatch(e -> e.type().equals("ROADS_BUILT") && e.description().contains(settlement.id().toString()));
        if (existing) { roadSettlements.add(settlement.id()); return; }
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                settlement.center().getX(), settlement.center().getZ());
        BlockPos base = new BlockPos(settlement.center().getX(), y, settlement.center().getZ());
        List<Placement> blocks = new ArrayList<>();
        for (int d=-14; d<=14; d++) {
            add(blocks, ground(level, base.getX()+d, base.getZ()), state(Blocks.GRAVEL));
            add(blocks, ground(level, base.getX(), base.getZ()+d), state(Blocks.GRAVEL));
        }
        queue.add(new Project(Kind.ROAD, settlement.id(), settlement.kingdomId(), settlement.name()+" roads", base, 0, blocks));
        roadSettlements.add(settlement.id());
        state.history("ROAD_ORDERED", "Citizens of " + settlement.name() + " started building local roads.");
    }

    public void requestWallIfReady(ServerLevel level, WorldMorphState state, WorldMorphState.SettlementData settlement) {
        if (settlement.population() < 50 || wallSettlements.contains(settlement.id())) return;
        boolean existing = state.history().stream().anyMatch(e -> e.type().equals("WALL_BUILT") && e.description().contains(settlement.id().toString()));
        if (existing) { wallSettlements.add(settlement.id()); return; }
        int radius = 20;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                settlement.center().getX(), settlement.center().getZ());
        BlockPos base = new BlockPos(settlement.center().getX(), y, settlement.center().getZ());
        List<Placement> blocks = new ArrayList<>();
        for (int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
            if(Math.abs(x)!=radius && Math.abs(z)!=radius) continue;
            boolean gate = z==-radius && x>=-2 && x<=2;
            for(int h=0;h<4;h++) if(!gate || h>=3) add(blocks, base.offset(x,h,z), state(Blocks.COBBLESTONE));
            if((x+z)%4==0) add(blocks, base.offset(x,4,z), state(Blocks.STONE_BRICK_WALL));
        }
        queue.add(new Project(Kind.WALL, settlement.id(), settlement.kingdomId(), settlement.name()+" walls", base, 0, blocks));
        wallSettlements.add(settlement.id());
        state.history("WALL_ORDERED", "Citizens of " + settlement.name() + " started building defensive walls.");
    }

    private BlockPos ground(ServerLevel level, int x, int z) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
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

    public void requestVillageBuildingsIfReady(ServerLevel level, WorldMorphState state, WorldMorphState.SettlementData s) {
        int p=s.population();
        if (p>=12 && !marketSettlements.contains(s.id())) queueSimpleBuilding(level,state,s,Kind.MARKET,s.center().offset(10,0,-6),marketPlan(s.center().offset(10,0,-6)),marketSettlements,"MARKET");
        if (p>=20 && !blacksmithSettlements.contains(s.id())) queueSimpleBuilding(level,state,s,Kind.BLACKSMITH,s.center().offset(-10,0,6),blacksmithPlan(s.center().offset(-10,0,6)),blacksmithSettlements,"BLACKSMITH");
        if (p>=30 && !librarySettlements.contains(s.id())) queueSimpleBuilding(level,state,s,Kind.LIBRARY,s.center().offset(10,0,7),libraryPlan(s.center().offset(10,0,7)),librarySettlements,"LIBRARY");
        if (p>=40 && !townHallSettlements.contains(s.id())) queueSimpleBuilding(level,state,s,Kind.TOWN_HALL,s.center().offset(-10,0,-7),townHallPlan(s.center().offset(-10,0,-7)),townHallSettlements,"TOWN_HALL");
        if (p>=8 && !farmSettlements.contains(s.id())) queueSimpleBuilding(level,state,s,Kind.FARM,s.center().offset(0,0,14),farmPlan(s.center().offset(0,0,14)),farmSettlements,"FARM");
    }

    private void queueSimpleBuilding(ServerLevel level, WorldMorphState state, WorldMorphState.SettlementData s,
                                     Kind kind, BlockPos base, List<Placement> plan, Set<UUID> registry, String label) {
        boolean built=state.history().stream().anyMatch(e->e.type().equals(label+"_BUILT")&&e.description().contains(s.id().toString()));
        boolean queued=queue.stream().anyMatch(p->p.kind==kind&&p.settlementId.equals(s.id()));
        if(built||queued)return;
        int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,base.getX(),base.getZ());
        base=new BlockPos(base.getX(),y,base.getZ());
        queue.add(new Project(kind,s.id(),s.kingdomId(),s.name()+" "+label.toLowerCase(),base,0,plan));
        registry.add(s.id());
        state.history(label+"_ORDERED","Citizens of "+s.name()+" started building the "+label.toLowerCase()+".");
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

    public void tick(ServerLevel level, WorldMorphState state, HousingManager housing, NpcManager npcs) {
        Project project = queue.peek();
        if (project == null) return;
        if (project.cooldown > 0) { project.cooldown--; return; }

        List<Villager> builders = level.getEntitiesOfClass(Villager.class,
                new net.minecraft.world.phys.AABB(project.base).inflate(48),
                v -> v.isAlive() && !v.isBaby() && isCitizenBuilder(v, project, npcs)).stream().limit(3).toList();
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
            } else if (project.kind == Kind.WALL) {
                state.history("WALL_BUILT", "Settlement " + project.settlementId + ": " + project.name + " was completed block by block by citizens.");
            } else if (project.kind == Kind.ROAD) {
                state.history("ROADS_BUILT", "Settlement " + project.settlementId + ": " + project.name + " were completed block by block by citizens.");
            } else if (project.kind == Kind.CASTLE) {
                completedCastlePhases.put(project.kingdomId, project.castlePhase);
                queuedCastlePhases.remove(project.kingdomId + ":" + project.castlePhase);
                state.history("CASTLE_PHASE_" + project.castlePhase,
                        "Kingdom " + project.kingdomId + ": " + project.name + " completed castle phase " + project.castlePhase + ".");
            } else if (project.kind == Kind.MARKET || project.kind == Kind.BLACKSMITH
                    || project.kind == Kind.LIBRARY || project.kind == Kind.TOWN_HALL || project.kind == Kind.FARM) {
                String type = project.kind.name() + "_BUILT";
                state.history(type, "Settlement " + project.settlementId + ": " + project.name + " was completed block by block by citizens.");
            } else {
                monumentSettlements.add(project.settlementId);
                state.history("MONUMENT_RAISED", "Settlement " + project.settlementId + ": " + project.name + " was built block by block by villagers.");
            }
        }
    }

    private boolean isCitizenBuilder(Villager villager, Project project, NpcManager npcs) {
        NpcProfile profile = npcs.get(villager.getUUID());
        return profile != null && profile.alive()
                && project.settlementId.equals(profile.settlementId())
                && ("BUILDER".equals(profile.job()) || project.kind != Kind.CASTLE);
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

    private List<Placement> boxPlan(BlockPos b, BlockState floor, BlockState wall, BlockState roof, int w, int d, int h) {
        List<Placement> out=new ArrayList<>();
        for(int x=0;x<w;x++) for(int z=0;z<d;z++) add(out,b.offset(x,0,z),floor);
        for(int y=1;y<=h;y++) for(int x=0;x<w;x++) for(int z=0;z<d;z++) {
            if(x!=0&&x!=w-1&&z!=0&&z!=d-1) continue;
            boolean door=z==0&&x==w/2&&y<=2;
            boolean window=y==2&&((z==0||z==d-1)&&(x==1||x==w-2));
            add(out,b.offset(x,y,z),door?state(Blocks.AIR):window?state(Blocks.GLASS_PANE):wall);
        }
        for(int x=-1;x<=w;x++) for(int z=-1;z<=d;z++)
            if(x==-1||x==w||z==-1||z==d) add(out,b.offset(x,h+1,z),roof);
        return out;
    }
    private List<Placement> marketPlan(BlockPos b) {
        List<Placement> o=new ArrayList<>();
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) add(o,b.offset(x*4,0,z*4),state(Blocks.SPRUCE_PLANKS));
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) { add(o,b.offset(x*4,1,z*4),state(Blocks.OAK_FENCE)); add(o,b.offset(x*4,2,z*4),state(Blocks.WHITE_WOOL)); }
        return o;
    }
    private List<Placement> blacksmithPlan(BlockPos b){ return boxPlan(b,state(Blocks.COBBLESTONE),state(Blocks.STONE_BRICKS),state(Blocks.SPRUCE_PLANKS),7,7,3); }
    private List<Placement> libraryPlan(BlockPos b){ List<Placement> o=boxPlan(b,state(Blocks.OAK_PLANKS),state(Blocks.SPRUCE_PLANKS),state(Blocks.DARK_OAK_PLANKS),9,7,4); add(o,b.offset(4,1,3),state(Blocks.BOOKSHELF)); add(o,b.offset(3,1,3),state(Blocks.LECTERN)); return o; }
    private List<Placement> townHallPlan(BlockPos b){ List<Placement> o=boxPlan(b,state(Blocks.STONE_BRICKS),state(Blocks.OAK_LOG),state(Blocks.SPRUCE_PLANKS),11,9,4); add(o,b.offset(5,1,4),state(Blocks.BELL)); add(o,b.offset(5,5,4),state(Blocks.LANTERN)); return o; }
    private List<Placement> farmPlan(BlockPos b){ List<Placement> o=new ArrayList<>(); for(int x=0;x<15;x++) for(int z=0;z<9;z++) { if(x==7) add(o,b.offset(x,0,z),state(Blocks.WATER)); else add(o,b.offset(x,0,z),state(Blocks.FARMLAND)); } for(int x=0;x<15;x+=2) add(o,b.offset(x,1,0),state(Blocks.OAK_FENCE)); add(o,b.offset(7,1,4),state(Blocks.WATER)); return o; }

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
