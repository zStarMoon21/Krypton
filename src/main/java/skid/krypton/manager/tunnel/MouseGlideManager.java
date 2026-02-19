package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class MouseGlideManager {
    private final MinecraftClient mc;
    private final Random random = new Random();
    private float targetYaw;
    private float targetPitch;
    private int glideTimer = 0;
    private boolean isGliding = false;
    
    public MouseGlideManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void glideToBlock(BlockPos pos) {
        if (pos == null || mc.player == null) return;
        
        Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d direction = blockCenter.subtract(playerPos).normalize();
        
        float centerPitch = (float) Math.toDegrees(Math.asin(-direction.y));
        float centerYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        
        if (!isGliding) {
            this.targetYaw = centerYaw + (random.nextFloat() - 0.5f) * 3.0f;
            this.targetPitch = centerPitch + (random.nextFloat() - 0.5f) * 2.0f;
            this.glideTimer = 40 + random.nextInt(60);
            this.isGliding = true;
        }
        
        if (glideTimer > 20) {
            mc.player.setYaw(mc.player.getYaw() + (this.targetYaw - mc.player.getYaw()) * 0.05f);
            mc.player.setPitch(mc.player.getPitch() + (this.targetPitch - mc.player.getPitch()) * 0.05f);
        } else {
            mc.player.setYaw(mc.player.getYaw() + (centerYaw - mc.player.getYaw()) * 0.1f);
            mc.player.setPitch(mc.player.getPitch() + (centerPitch - mc.player.getPitch()) * 0.1f);
        }
        
        glideTimer--;
        if (glideTimer <= 0) {
            isGliding = false;
        }
    }
}
