package skid.krypton.module.modules.donut.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class TunnelPathFinder {
    private final MinecraftClient mc;
    private static final int TUNNEL_WIDTH = 3;
    private static final int TUNNEL_HEIGHT = 3;
    
    public TunnelPathFinder(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public List<BlockPos> findPath(TunnelDirection tunnelDir, int length) {
        List<BlockPos> path = new ArrayList<>();
        if (mc.player == null) return path;
        
        Direction dir = toMinecraftDirection(tunnelDir);
        BlockPos startPos = mc.player.getBlockPos();
        
        // Mine in a 3x3 pattern for a proper tunnel
        for (int i = 0; i < length; i++) {
            BlockPos forward = startPos.offset(dir, i + 1);
            
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
        
        return path;
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
}
