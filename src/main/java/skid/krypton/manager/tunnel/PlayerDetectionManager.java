package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class PlayerDetectionManager {
    private final MinecraftClient mc;

    public PlayerDetectionManager(MinecraftClient mc) {
        this.mc = mc;
    }

    /**
     * Returns nearest visible player within radius, or null
     */
    public PlayerEntity checkForPlayers(double radius) {
        if (mc.world == null || mc.player == null) return null;

        PlayerEntity closest = null;
        double closestDist = radius * radius;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isSpectator() || player.isInvisible()) continue;

            double distSq = mc.player.squaredDistanceTo(player);
            if (distSq > closestDist) continue;

            // Optional visibility check (raycast)
            if (!mc.player.canSee(player)) continue;

            closest = player;
            closestDist = distSq;
        }

        return closest;
    }
}
