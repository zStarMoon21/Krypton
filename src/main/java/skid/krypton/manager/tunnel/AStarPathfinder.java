package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.*;

public class AStarPathfinder {
    private final MinecraftClient mc;
    private List<BlockPos> currentPath = new ArrayList<>();
    private List<BlockPos> hazardPositions = new ArrayList<>();
    private Direction currentDirection;
    
    private static final int SCAN_DISTANCE = 20;
    private static final int SEARCH_HEIGHT = 3;
    private static final Block[] HAZARDS = {Blocks.LAVA, Blocks.WATER, Blocks.GRAVEL};
    private static final Block[] UNBREAKABLE = {Blocks.BEDROCK, Blocks.BARRIER};
    
    public AStarPathfinder(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public List<BlockPos> findPath(TunnelDirection dir) {
        if (mc.player == null || mc.world == null) return new ArrayList<>();
        
        currentDirection = toMinecraftDirection(dir);
        hazardPositions.clear();
        
        BlockPos start = mc.player.getBlockPos();
        BlockPos target = findTargetPosition(start, currentDirection, SCAN_DISTANCE);
        
        if (target == null) return new ArrayList<>();
        
        // A* algorithm
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, PathNode> nodeMap = new HashMap<>();
        
        PathNode startNode = new PathNode(start);
        startNode.calculateCosts(start, target);
        openSet.add(startNode);
        nodeMap.put(start, startNode);
        
        int iterations = 0;
        int maxIterations = 500; // Prevent infinite loops
        
        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            PathNode current = openSet.poll();
            
            if (current.pos.equals(target) || isNearTarget(current.pos, target)) {
                return reconstructPath(current);
            }
            
            closedSet.add(current.pos);
            
            // Check neighbors in 3D
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        
                        BlockPos neighborPos = current.pos.add(x, y, z);
                        
                        // Don't go too far up/down
                        if (Math.abs(neighborPos.getY() - start.getY()) > SEARCH_HEIGHT) continue;
                        
                        if (closedSet.contains(neighborPos)) continue;
                        
                        // Check if position is walkable/mineable
                        if (!isPositionValid(neighborPos)) {
                            // If it's a hazard, add to hazard list
                            if (isHazard(neighborPos)) {
                                hazardPositions.add(neighborPos);
                            }
                            continue;
                        }
                        
                        PathNode neighbor = nodeMap.get(neighborPos);
                        if (neighbor == null) {
                            neighbor = new PathNode(neighborPos);
                            nodeMap.put(neighborPos, neighbor);
                        }
                        
                        double tentativeG = current.gCost + calculateCost(current.pos, neighborPos);
                        
                        if (!openSet.contains(neighbor)) {
                            neighbor.parent = current;
                            neighbor.gCost = tentativeG;
                            neighbor.calculateCosts(start, target);
                            openSet.add(neighbor);
                        } else if (tentativeG < neighbor.gCost) {
                            neighbor.parent = current;
                            neighbor.gCost = tentativeG;
                            neighbor.calculateCosts(start, target);
                            // Re-add to update priority
                            openSet.remove(neighbor);
                            openSet.add(neighbor);
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>();
    }
    
    private BlockPos findTargetPosition(BlockPos start, Direction dir, int distance) {
        for (int i = distance; i > 0; i--) {
            BlockPos check = start.offset(dir, i);
            // Check if any blocks in this area are mineable
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    BlockPos minePos;
                    if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                        minePos = check.add(x, y, 0);
                    } else {
                        minePos = check.add(0, y, x);
                    }
                    
                    Block block = mc.world.getBlockState(minePos).getBlock();
                    if (block != Blocks.AIR && !isUnbreakable(block)) {
                        return check;
                    }
                }
            }
        }
        return start.offset(dir, distance);
    }
    
    private double calculateCost(BlockPos from, BlockPos to) {
        Block block = mc.world.getBlockState(to).getBlock();
        double baseCost = Math.sqrt(from.getSquaredDistance(to));
        
        // Cost modifiers
        if (block == Blocks.AIR) return baseCost * 0.5; // Air is cheap
        if (isHazard(to)) return baseCost * 10; // Hazards are expensive
        if (isUnbreakable(block)) return Double.MAX_VALUE; // Unbreakable = impossible
        
        // Normal blocks have normal cost
        return baseCost;
    }
    
    private boolean isPositionValid(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        
        // Check if it's a hazard (add to hazard list but still consider)
        if (isHazard(pos)) {
            return false; // Don't path through hazards
        }
        
        // Can't path through unbreakable blocks
        if (isUnbreakable(block)) {
            return false;
        }
        
        return true;
    }
    
    private boolean isHazard(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        for (Block hazard : HAZARDS) {
            if (block == hazard) return true;
        }
        return false;
    }
    
    private boolean isUnbreakable(Block block) {
        for (Block ub : UNBREAKABLE) {
            if (block == ub) return true;
        }
        return false;
    }
    
    private boolean isNearTarget(BlockPos current, BlockPos target) {
        return Math.abs(current.getX() - target.getX()) <= 2 &&
               Math.abs(current.getY() - target.getY()) <= 2 &&
               Math.abs(current.getZ() - target.getZ()) <= 2;
    }
    
    private List<BlockPos> reconstructPath(PathNode endNode) {
        List<BlockPos> path = new ArrayList<>();
        PathNode current = endNode;
        
        while (current != null) {
            path.add(0, current.pos);
            current = current.parent;
        }
        
        // Remove start position
        if (!path.isEmpty() && path.get(0).equals(mc.player.getBlockPos())) {
            path.remove(0);
        }
        
        currentPath = path;
        return path;
    }
    
    private Direction toMinecraftDirection(TunnelDirection dir) {
        return switch (dir) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
        };
    }
    
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }
    
    public List<BlockPos> getHazardPositions() {
        return hazardPositions;
    }
    
    public BlockPos getFirstTarget() {
        return currentPath.isEmpty() ? null : currentPath.get(0);
    }
    
    public void removeFirstTarget() {
        if (!currentPath.isEmpty()) {
            currentPath.remove(0);
        }
    }
}
