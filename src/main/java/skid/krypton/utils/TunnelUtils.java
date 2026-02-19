package skid.krypton.utils;

import net.minecraft.entity.player.PlayerEntity;
import skid.krypton.module.modules.donut.tunnel.TunnelDirection;

public class TunnelUtils {
    
    public static TunnelDirection getInitialDirection(PlayerEntity player) {
        float yaw = player.getYaw() % 360.0f;
        if (yaw < 0.0f) yaw += 360.0f;
        
        if (yaw >= 45.0f && yaw < 135.0f) return TunnelDirection.WEST;
        if (yaw >= 135.0f && yaw < 225.0f) return TunnelDirection.NORTH;
        if (yaw >= 225.0f && yaw < 315.0f) return TunnelDirection.EAST;
        return TunnelDirection.SOUTH;
    }
    
    public static float getDirectionYaw(TunnelDirection dir) {
        switch(dir) {
            case NORTH: return 180;
            case SOUTH: return 0;
            case EAST: return -90;
            case WEST: return 90;
            default: return 0;
        }
    }
    
    public static boolean isSafeBlock(net.minecraft.block.Block block) {
        return block != net.minecraft.block.Blocks.LAVA && 
               block != net.minecraft.block.Blocks.FLOWING_LAVA &&
               block != net.minecraft.block.Blocks.WATER && 
               block != net.minecraft.block.Blocks.FLOWING_WATER;
    }
}
