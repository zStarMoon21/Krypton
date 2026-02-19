package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.*;

public class PathFinder {
    private final MinecraftClient mc;
    private final HazardAvoidanceManager hazardAvoid;
    private List<BlockPos> currentPath = new ArrayList<>();
    private BlockPos currentTarget;
    private TunnelDirection currentDirection;
    
    // Path finding settings
    private static final int MAX_PATH_LENGTH = 20;
    private static final int TUNNEL_WIDTH = 3;
    private static final int TUNNEL_HEIGHT = 3;
    
    public PathFinder(MinecraftClient mc, HazardAvoidanceManager hazardAvoid) {
        this.mc = mc;
        this.hazardAvoid = hazardAvoid;
    }
    
    public List<BlockPos> findPath(TunnelDirection direction, int length) {
        if (mc.player == null) return new ArrayList<>();
        
        this.currentDirection = direction;
        List<BlockPos> path = new ArrayList<>();
        Direction dir = toMinecraftDirection(direction);
        BlockPos startPos = mc.player.getBlockPos();
        
        // First, check for hazards and get safe path if needed
        BlockPos hazard = hazardAvoid.detectHazard();
        if (hazard != null) {
            List<BlockPos> safePath = findSafePathAroundHazard(hazard, dir, startPos);
            if (!safePath.isEmpty()) {
                this.currentPath = safePath;
                this.currentTarget = safePath.isEmpty() ? null : safePath.get(0);
                return safePath;
            }
        }
        
        // Calculate tunnel path (3x3 tunnel)
        for (int i = 0; i < Math.min(length, MAX_PATH_LENGTH); i++) {
            BlockPos forward = startPos.offset(dir, i + 1);
            
            // Check if path is blocked by unbreakable block
            if (isPathBlocked(forward)) {
                break;
            }
            
            // Add all blocks in the 3x3 tunnel face
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    BlockPos minePos;
                    if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                        minePos = forward.add(x, y, 0);
                    } else {
                        minePos = forward.add(0, y, x);
                    }
                    
                    if (shouldMine(minePos)) {
                        path.add(minePos);
                    }
                }
            }
        }
        
        this.currentPath = path;
        this.currentTarget = path.isEmpty() ? null : path.get(0);
        return path;
    }
    
    private List<BlockPos> findSafePathAroundHazard(BlockPos hazard, Direction dir, BlockPos startPos) {
        List<BlockPos> safePath = new ArrayList<>();
        
        // Try to go up first
        BlockPos up = startPos.up(2);
        if (isSafe(up) && isSafe(up.offset(dir, 1)) && isSafe(up.offset(dir, 2))) {
            safePath.add(up);
            safePath.add(up.offset(dir, 1));
            safePath.add(up.offset(dir, 2));
            safePath.add(startPos.offset(dir, 2));
            return safePath;
        }
        
        // Try to go around sides
        Direction[] sides = {dir.rotateYClockwise(), dir.rotateYCounterclockwise()};
        for (Direction side : sides) {
            BlockPos sidePos = startPos.offset(side, 2);
            if (isSafe(sidePos) && isSafe(sidePos.offset(dir, 1)) && isSafe(sidePos.offset(dir, 2))) {
                safePath.add(sidePos);
                safePath.add(sidePos.offset(dir, 1));
                safePath.add(sidePos.offset(dir, 2));
                safePath.add(startPos.offset(dir, 2));
                return safePath;
            }
        }
        
        return safePath;
    }
    
    private boolean isSafe(BlockPos pos) {
        if (mc.world == null) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.AIR || (block != Blocks.LAVA && block != Blocks.WATER && block != Blocks.BEDROCK);
    }
    
    private boolean isPathBlocked(BlockPos pos) {
        if (mc.world == null) return false;
        // Check if any block in the tunnel face is unbreakable
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                BlockPos checkPos = pos.add(x, y, 0);
                Block block = mc.world.getBlockState(checkPos).getBlock();
                if (block == Blocks.BEDROCK) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean shouldMine(BlockPos pos) {
        if (mc.world == null) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return block != Blocks.AIR && 
               block != Blocks.BEDROCK && 
               block != Blocks.LAVA && 
               block != Blocks.WATER &&
               block != Blocks.CHEST &&
               block != Blocks.TRAPPED_CHEST &&
               block != Blocks.ENDER_CHEST;
    }
    
    private Direction toMinecraftDirection(TunnelDirection dir) {
        switch(dir) {
            case NORTH: return Direction.NORTH;
            case SOUTH: return Direction.SOUTH;
            case EAST: return Direction.EAST;
            case WEST: return Direction.WEST;
            default: return Direction.NORTH;
        }
    }
    
    public void updateTarget() {
        if (!currentPath.isEmpty()) {
            // Remove mined blocks from path
            currentPath.removeIf(pos -> mc.world != null && mc.world.getBlockState(pos).isAir());
            currentTarget = currentPath.isEmpty() ? null : currentPath.get(0);
        }
    }
    
    public void removeCurrentTarget() {
        if (!currentPath.isEmpty()) {
            currentPath.remove(0);
            currentTarget = currentPath.isEmpty() ? null : currentPath.get(0);
        }
    }
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
    
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }
    
    public boolean isPathFinished() {
        return currentPath.isEmpty();
    }
}
