package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import skid.krypton.utils.TunnelUtils;

import java.util.ArrayList;
import java.util.List;

public class PathFinder {
    private final MinecraftClient mc;
    private List<BlockPos> currentPath = new ArrayList<>();
    private BlockPos currentTarget;
    private TunnelDirection currentDirection;
    private HazardAvoidanceManager hazardAvoid;
    
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
        
        // Check for hazards first
        BlockPos hazard = hazardAvoid.detectHazard();
        if (hazard != null) {
            return hazardAvoid.findSafePath(hazard, direction);
        }
        
        // Calculate tunnel path (3x3 tunnel)
        for (int i = 0; i < length; i++) {
            BlockPos forward = startPos.offset(dir, i + 1);
            
            // Check if path is blocked by hazard
            if (isHazardNearby(forward)) {
                return hazardAvoid.findSafePath(forward, direction);
            }
            
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
        if (!path.isEmpty()) {
            this.currentTarget = path.get(0);
        }
        
        return path;
    }
    
    private boolean isHazardNearby(BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    Block block = mc.world.getBlockState(checkPos).getBlock();
                    if (block == Blocks.LAVA || block == Blocks.WATER) {
                        return true;
                    }
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
               block != Blocks.TRAPPED_CHEST;
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
            currentTarget = currentPath.get(0);
        } else {
            currentTarget = null;
        }
    }
    
    public void removeCurrentTarget() {
        if (!currentPath.isEmpty()) {
            currentPath.remove(0);
            updateTarget();
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
