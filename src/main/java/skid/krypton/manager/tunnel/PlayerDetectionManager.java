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

    for (PlayerEntity p : mc.world.getPlayers()) {
        if (p == mc.player || p.isSpectator()) continue;

        if (mc.player.distanceTo(p) <= radius) return p;
    }
    return null;
}

