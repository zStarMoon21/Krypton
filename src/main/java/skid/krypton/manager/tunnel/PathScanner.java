package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class PathScanner {
    private final MinecraftClient mc;
    private List<BlockPos> currentPath = new ArrayList<>();
    private Direction currentDirection;
    
    // Hazards to avoid
    private static final Block[] HAZARDS = {Blocks.LAVA, Blocks.WATER, Blocks.GRAVEL};
    
    public PathScanner(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public List<BlockPos> scanPath(TunnelDirection tunnelDir, int scanDistance) {
        if (mc.player == null) return new ArrayList<>();
        
        currentDirection = toMinecraftDirection(tunnelDir);
        List<BlockPos> path = new ArrayList<>();
        BlockPos startPos = mc.player.getBlockPos();
        
        // Scan forward block by block
        for (int i = 1; i <= scanDistance; i++) {
            BlockPos checkPos = startPos.offset(currentDirection, i);
            
            // Check for hazards
            BlockPos hazard = findHazardNearby(checkPos);
            if (hazard != null) {
                // Found hazard, find path around it
                List<BlockPos> detour = findDetour(hazard, i);
                if (!detour.isEmpty()) {
                    path.addAll(detour);
                }
                break;
            }
            
            // Add this position to path if it has minable blocks
            if (hasBlocksToMine(checkPos)) {
                path.add(checkPos);
            }
        }
        
        currentPath = path;
        return path;
    }
    
    private BlockPos findHazardNearby(BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    Block block = mc.world.getBlockState(checkPos).getBlock();
                    
                    for (Block hazard : HAZARDS) {
                        if (block == hazard) {
                            return checkPos;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private boolean hasBlocksToMine(BlockPos pos) {
        // Check the 3x3 tunnel face
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                BlockPos minePos;
                if (currentDirection == Direction.NORTH || currentDirection == Direction.SOUTH) {
                    minePos = pos.add(x, y, 0);
                } else {
                    minePos = pos.add(0, y, x);
                }
                
                Block block = mc.world.getBlockState(minePos).getBlock();
                if (block != Blocks.AIR && 
                    block != Blocks.BEDROCK && 
                    block != Blocks.LAVA && 
                    block != Blocks.WATER) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private List<BlockPos> findDetour(BlockPos hazardPos, int currentDistance) {
        List<BlockPos> detour = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Try going up first
        BlockPos up = playerPos.up(2).offset(currentDirection, currentDistance - 1);
        if (isPathClear(up, 3)) {
            detour.add(playerPos.up(2));
            detour.add(playerPos.up(2).offset(currentDirection, 1));
            detour.add(playerPos.up(2).offset(currentDirection, 2));
            detour.add(playerPos.offset(currentDirection, 2));
            return detour;
        }
        
        // Try going left
        Direction left = currentDirection.rotateYCounterclockwise();
        BlockPos leftPos = playerPos.offset(left, 2).offset(currentDirection, currentDistance - 1);
        if (isPathClear(leftPos, 3)) {
            detour.add(playerPos.offset(left, 2));
            detour.add(playerPos.offset(left, 2).offset(currentDirection, 1));
            detour.add(playerPos.offset(left, 2).offset(currentDirection, 2));
            detour.add(playerPos.offset(currentDirection, 2));
            return detour;
        }
        
        // Try going right
        Direction right = currentDirection.rotateYClockwise();
        BlockPos rightPos = playerPos.offset(right, 2).offset(currentDirection, currentDistance - 1);
        if (isPathClear(rightPos, 3)) {
            detour.add(playerPos.offset(right, 2));
            detour.add(playerPos.offset(right, 2).offset(currentDirection, 1));
            detour.add(playerPos.offset(right, 2).offset(currentDirection, 2));
            detour.add(playerPos.offset(currentDirection, 2));
            return detour;
        }
        
        return detour;
    }
    
    private boolean isPathClear(BlockPos start, int length) {
        for (int i = 0; i < length; i++) {
            BlockPos checkPos = start.offset(currentDirection, i);
            if (findHazardNearby(checkPos) != null) {
                return false;
            }
        }
        return true;
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
    
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }
    
    public BlockPos getNextTarget() {
        return currentPath.isEmpty() ? null : currentPath.get(0);
    }
    
    public void removeTarget(BlockPos pos) {
        currentPath.remove(pos);
    }
}
