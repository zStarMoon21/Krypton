package skid.krypton.module.modules.donut.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerDetector {
    private final MinecraftClient mc;
    
    public PlayerDetector(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public PlayerEntity checkForPlayers(int radius) {
        if (mc.world == null || mc.player == null) return null;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            
            double distance = mc.player.distanceTo(player);
            
            // Check if player is in spectator or within radius
            if (player.isSpectator() || distance < radius) {
                return player;
            }
        }
        return null;
    }
}
