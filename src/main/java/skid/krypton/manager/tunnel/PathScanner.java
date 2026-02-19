package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PathScanner {
    private final MinecraftClient mc;
    private List<BlockPos> currentPath = new ArrayList<>();
    private List<BlockPos> hazardList = new ArrayList<>();
    private Direction currentDirection;
    private BlockPos lastScanPos;
    private int scanCounter = 0;
    
    // Hazards to avoid
    private static final Block[] HAZARDS = {Blocks.LAVA, Blocks.WATER, Blocks.GRAVEL};
    private static final int SCAN_DISTANCE = 20;
    private static final int SCAN_INTERVAL = 20; // Scan every 20 ticks to save performance
    
    public PathScanner(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public List<BlockPos> scanPath(TunnelDirection tunnelDir) {
        if (mc.player == null) return new ArrayList<>();
        
        currentDirection = toMinecraftDirection(tunnelDir);
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Only scan every SCAN_INTERVAL ticks or if player moved significantly
        scanCounter++;
        if (scanCounter < SCAN_INTERVAL && lastScanPos != null && 
            playerPos.getManhattanDistance(lastScanPos) < 5) {
            return currentPath;
        }
        
        scanCounter = 0;
        lastScanPos = playerPos;
        
        // Clear old hazards
        hazardList.clear();
        
        // Scan forward 20 blocks
        List<BlockPos> newPath = new ArrayList<>();
        
        for (int i = 1; i <= SCAN_DISTANCE; i++) {
            BlockPos checkPos = playerPos.offset(currentDirection, i);
            
            // Check for hazards at this position
            List<BlockPos> hazards = findHazardsInArea(checkPos);
            if (!hazards.isEmpty()) {
                // Add hazards to list for rendering
                hazardList.addAll(hazards);
                
                // Find path around hazards
                List<BlockPos> detour = findQuickestDetour(checkPos, i);
                if (!detour.isEmpty()) {
                    newPath.addAll(detour);
                    break;
                }
            }
            
            // Add this position to path
            newPath.add(checkPos);
        }
        
        currentPath = newPath;
        return newPath;
    }
    
    private List<BlockPos> findHazardsInArea(BlockPos pos) {
        List<BlockPos> hazards = new ArrayList<>();
        
        // Check 3x3 area around the position
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    Block block = mc.world.getBlockState(checkPos).getBlock();
                    
                    for (Block hazard : HAZARDS) {
                        if (block == hazard) {
                            hazards.add(checkPos);
                        }
                    }
                }
            }
        }
        return hazards;
    }
    
    private List<BlockPos> findQuickestDetour(BlockPos hazardPos, int currentDistance) {
        List<BlockPos> detour = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Try each possible detour and pick the shortest
        List<List<BlockPos>> options = new ArrayList<>();
        
        // Option 1: Go up
        List<BlockPos> upPath = new ArrayList<>();
        BlockPos up = playerPos.up(2);
        if (isPathClear(up, 3)) {
            upPath.add(playerPos.up(2));
            upPath.add(playerPos.up(2).offset(currentDirection, 1));
            upPath.add(playerPos.up(2).offset(currentDirection, 2));
            upPath.add(playerPos.offset(currentDirection, 2));
            options.add(upPath);
        }
        
        // Option 2: Go left
        Direction left = currentDirection.rotateYCounterclockwise();
        List<BlockPos> leftPath = new ArrayList<>();
        BlockPos leftPos = playerPos.offset(left, 2);
        if (isPathClear(leftPos, 3)) {
            leftPath.add(playerPos.offset(left, 2));
            leftPath.add(playerPos.offset(left, 2).offset(currentDirection, 1));
            leftPath.add(playerPos.offset(left, 2).offset(currentDirection, 2));
            leftPath.add(playerPos.offset(currentDirection, 2));
            options.add(leftPath);
        }
        
        // Option 3: Go right
        Direction right = currentDirection.rotateYClockwise();
        List<BlockPos> rightPath = new ArrayList<>();
        BlockPos rightPos = playerPos.offset(right, 2);
        if (isPathClear(rightPos, 3)) {
            rightPath.add(playerPos.offset(right, 2));
            rightPath.add(playerPos.offset(right, 2).offset(currentDirection, 1));
            rightPath.add(playerPos.offset(right, 2).offset(currentDirection, 2));
            rightPath.add(playerPos.offset(currentDirection, 2));
            options.add(rightPath);
        }
        
        // Return the shortest path
        if (!options.isEmpty()) {
            options.sort((a, b) -> Integer.compare(a.size(), b.size()));
            return options.get(0);
        }
        
        return detour;
    }
    
    private boolean isPathClear(BlockPos start, int length) {
        for (int i = 0; i < length; i++) {
            BlockPos checkPos = start.offset(currentDirection, i);
            if (!findHazardsInArea(checkPos).isEmpty()) {
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
    
    public List<BlockPos> getHazardList() {
        return hazardList;
    }
    
    public BlockPos getNextTarget() {
        return currentPath.isEmpty() ? null : currentPath.get(0);
    }
    
    public void removeTarget(BlockPos pos) {
        currentPath.remove(pos);
    }
}
