package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerDetectionManager {
    private final MinecraftClient mc;
    
    public PlayerDetectionManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public PlayerEntity checkForPlayers(int radius) {
        if (mc.world == null || mc.player == null) return null;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            
            double distance = mc.player.distanceTo(player);
            
            if (player.isSpectator() || distance < radius) {
                return player;
            }
        }
        return null;
    }
}
