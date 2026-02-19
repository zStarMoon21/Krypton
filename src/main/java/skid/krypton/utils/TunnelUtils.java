package skid.krypton.utils;

import net.minecraft.entity.player.PlayerEntity;
import skid.krypton.manager.tunnel.TunnelDirection;

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
        return switch (dir) {
            case NORTH -> 180;
            case SOUTH -> 0;
            case EAST -> -90;
            case WEST -> 90;
        };
    }
}
