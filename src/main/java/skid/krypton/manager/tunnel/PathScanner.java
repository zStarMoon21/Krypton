package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class PathScanner {
    private final MinecraftClient mc;
    private final Map<BlockPos, Boolean> hazardCache = new HashMap<>();

    private List<BlockPos> path = new ArrayList<>(); // Make sure this variable exists
    private List<BlockPos> hazardList = new ArrayList<>();
    private Direction currentDirection;

    private BlockPos lastScanPos;
    private int tickCounter;

    private static final Block[] HAZARDS = {Blocks.LAVA, Blocks.WATER, Blocks.GRAVEL};
    private static final int SCAN_DISTANCE = 15;
    private static final int SCAN_INTERVAL = 10;

    public PathScanner(MinecraftClient mc) {
        this.mc = mc;
    }

    public List<BlockPos> scanPath(TunnelDirection dir) {
        if (mc.player == null || mc.world == null) return path;

        currentDirection = toMinecraftDirection(dir);
        BlockPos playerPos = mc.player.getBlockPos();

        tickCounter++;
        if (tickCounter < SCAN_INTERVAL && lastScanPos != null && playerPos.getManhattanDistance(lastScanPos) < 3) {
            return path;
        }

        tickCounter = 0;
        lastScanPos = playerPos;
        hazardCache.clear();
        hazardList.clear();

        List<BlockPos> newPath = new ArrayList<>();

        for (int i = 1; i <= SCAN_DISTANCE; i++) {
            BlockPos pos = playerPos.offset(currentDirection, i);

            if (isHazardNearby(pos)) {
                newPath.addAll(makeDetour(pos));
                break;
            }

            newPath.add(pos);
        }

        path = newPath;
        return path;
    }

    private boolean isHazardNearby(BlockPos pos) {
        for (BlockPos p : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 2, 1))) {
            if (isHazard(p)) {
                hazardList.add(p.toImmutable());
                return true;
            }
        }
        return false;
    }

    private boolean isHazard(BlockPos pos) {
        return hazardCache.computeIfAbsent(pos, p -> {
            Block b = mc.world.getBlockState(p).getBlock();
            for (Block h : HAZARDS) if (b == h) return true;
            return false;
        });
    }

    private List<BlockPos> makeDetour(BlockPos hazardPos) {
        List<BlockPos> detour = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();

        Direction left = currentDirection.rotateYCounterclockwise();
        Direction right = currentDirection.rotateYClockwise();

        BlockPos up = hazardPos.up(2);
        BlockPos leftPos = hazardPos.offset(left, 2);
        BlockPos rightPos = hazardPos.offset(right, 2);

        if (!isHazardNearby(up)) detour.add(up);
        else if (!isHazardNearby(leftPos)) detour.add(leftPos);
        else if (!isHazardNearby(rightPos)) detour.add(rightPos);

        detour.add(hazardPos.offset(currentDirection, 2));
        return detour;
    }

    private Direction toMinecraftDirection(TunnelDirection dir) {
        return switch (dir) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
        };
    }

    // FIX: Add getFirstTarget() method
    public BlockPos getFirstTarget() {
        if (path == null || path.isEmpty()) return null;
        return path.get(0);
    }

    // FIX: Add removeFirstTarget() method
    public void removeFirstTarget() {
        if (path != null && !path.isEmpty()) {
            path.remove(0);
        }
    }

    public List<BlockPos> getCurrentPath() { 
        return path; 
    }
    
    public List<BlockPos> getHazardList() { 
        return hazardList; 
    }
}
