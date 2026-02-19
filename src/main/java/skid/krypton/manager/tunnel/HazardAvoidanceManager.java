package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import skid.krypton.module.modules.donut.TunnelDirection;

import java.util.ArrayList;
import java.util.List;

public class HazardAvoidanceManager {
    private final MinecraftClient mc;
    
    public HazardAvoidanceManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public BlockPos detectHazard() {
        if (mc.player == null) return null;
        
        BlockPos playerPos = mc.player.getBlockPos();
        
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(checkPos).getBlock();
                    
                    if (block == Blocks.LAVA || block == Blocks.WATER) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
    
    public List<BlockPos> findSafePath(BlockPos hazard, TunnelDirection tunnelDir) {
        List<BlockPos> safePath = new ArrayList<>();
        if (mc.player == null) return safePath;
        
        Direction dir = toMinecraftDirection(tunnelDir);
        BlockPos playerPos = mc.player.getBlockPos();
        
        BlockPos up = playerPos.up(2);
        if (mc.world.getBlockState(up).isAir()) {
            safePath.add(up);
            safePath.add(up.offset(dir, 2));
            safePath.add(playerPos.offset(dir, 2));
        } else {
            Direction side = dir.rotateYClockwise();
            BlockPos sidePos = playerPos.offset(side, 2);
            safePath.add(sidePos);
            safePath.add(sidePos.offset(dir, 2));
        }
        
        return safePath;
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
}
