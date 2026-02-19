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
    private float originalYaw;
    private float originalPitch;
    
    // Reduce gliding intensity
    private static final float MAX_GLIDE_OFFSET = 1.5f; // Reduced from 3.0f
    private static final int GLIDE_DURATION = 30; // Reduced from 40-60
    
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
        
        // Normalize yaw
        while (centerYaw - mc.player.getYaw() > 180) centerYaw -= 360;
        while (centerYaw - mc.player.getYaw() < -180) centerYaw += 360;
        
        if (!isGliding) {
            // Very subtle glide - just tiny movements
            this.targetYaw = centerYaw + (random.nextFloat() - 0.5f) * MAX_GLIDE_OFFSET;
            this.targetPitch = centerPitch + (random.nextFloat() - 0.5f) * (MAX_GLIDE_OFFSET * 0.7f);
            this.glideTimer = GLIDE_DURATION;
            this.isGliding = true;
            this.originalYaw = mc.player.getYaw();
            this.originalPitch = mc.player.getPitch();
        }
        
        if (glideTimer > 0) {
            // Very smooth, subtle movement
            float progress = (float) glideTimer / GLIDE_DURATION;
            
            if (glideTimer > GLIDE_DURATION / 2) {
                // First half: move slightly away from center
                float factor = 1.0f - progress;
                mc.player.setYaw(originalYaw + (targetYaw - centerYaw) * factor);
                mc.player.setPitch(originalPitch + (targetPitch - centerPitch) * factor);
            } else {
                // Second half: move back to center
                float factor = 1.0f - (progress * 2.0f);
                mc.player.setYaw(targetYaw + (centerYaw - targetYaw) * factor);
                mc.player.setPitch(targetPitch + (centerPitch - targetPitch) * factor);
            }
            
            glideTimer--;
        } else {
            // Ensure we're back at center
            mc.player.setYaw(centerYaw);
            mc.player.setPitch(centerPitch);
            isGliding = false;
        }
    }
    
    public boolean isGliding() {
        return isGliding;
    }
}
