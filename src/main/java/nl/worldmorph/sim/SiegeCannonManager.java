package nl.worldmorph.sim;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Small command-driven siege cannon prototype. Cannons are physical block
 * structures; firing spends a shot and creates a controlled in-game blast.
 */
public final class SiegeCannonManager {
    public record Cannon(UUID owner, BlockPos position, Direction facing, int shotsFired) {}
    private final Map<UUID, Cannon> cannons = new LinkedHashMap<>();
    private final Set<UUID> craftedKits = new HashSet<>();

    public boolean hasCraftedKit(UUID owner) { return craftedKits.contains(owner); }
    public boolean craftKit(UUID owner) { return craftedKits.add(owner); }
    public boolean consumeKit(UUID owner) { return craftedKits.remove(owner); }

    public Cannon build(ServerLevel level, UUID owner, BlockPos origin, Direction facing) {
        Direction forward = facing.getAxis().isHorizontal() ? facing : Direction.SOUTH;
        Direction right = forward.getClockWise();
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                BlockPos p = origin.offset(right.getStepX() * x + forward.getStepX() * z, -1,
                    right.getStepZ() * x + forward.getStepZ() * z);
                level.setBlockAndUpdate(p, Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(origin, Blocks.IRON_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(origin.above(), Blocks.IRON_BLOCK.defaultBlockState());
        for (int z = 1; z <= 3; z++) {
            BlockPos p = origin.relative(forward, z).above();
            level.setBlockAndUpdate(p, Blocks.IRON_BARS.defaultBlockState());
        }
        Cannon cannon = new Cannon(owner, origin, forward, 0);
        cannons.put(owner, cannon);
        return cannon;
    }

    public Cannon get(UUID owner) {
        return cannons.get(owner);
    }

    public boolean fire(ServerLevel level, Entity shooter, UUID owner) {
        Cannon cannon = cannons.get(owner);
        if (cannon == null) return false;
        BlockPos p = cannon.position().relative(cannon.facing(), 4).above();
        // Use a TNT-style Minecraft explosion, not a real-world weapon simulation.
        level.explode(shooter, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
            3.0F, Level.ExplosionInteraction.TNT);
        cannons.put(owner, new Cannon(owner, cannon.position(), cannon.facing(), cannon.shotsFired() + 1));
        return true;
    }

    public int shotsFired(UUID owner) {
        Cannon cannon = cannons.get(owner);
        return cannon == null ? 0 : cannon.shotsFired();
    }
}
