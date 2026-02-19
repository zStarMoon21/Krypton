package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class PixelGlide {
    private final MinecraftClient mc;
    private final Random random = new Random();
    
    // Ultra subtle movement - just a few pixels
    private static final float MAX_PIXEL_MOVE = 0.08f; // ~2 pixels at 1080p
    private static final int GLIDE_TICKS = 8;
    
    private float targetYaw;
    private float targetPitch;
    private float centerYaw;
    private float centerPitch;
    private int glideTimer = 0;
    private boolean isGliding = false;
    
    public PixelGlide(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void update(BlockPos targetBlock) {
        if (targetBlock == null || mc.player == null) return;
        
        // Calculate perfect center of block
        Vec3d blockCenter = new Vec3d(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d direction = blockCenter.subtract(playerPos).normalize();
        
        float newCenterPitch = (float) Math.toDegrees(Math.asin(-direction.y));
        float newCenterYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        
        // Normalize yaw
        while (newCenterYaw - mc.player.getYaw() > 180) newCenterYaw -= 360;
        while (newCenterYaw - mc.player.getYaw() < -180) newCenterYaw += 360;
        
        // Start new glide if target changed
        if (centerYaw != newCenterYaw || centerPitch != newCenterPitch) {
            centerYaw = newCenterYaw;
            centerPitch = newCenterPitch;
            startGlide();
        }
        
        if (isGliding) {
            doGlide();
        } else {
            // Stay at center when not gliding
            mc.player.setYaw(centerYaw);
            mc.player.setPitch(centerPitch);
        }
    }
    
    private void startGlide() {
        targetYaw = centerYaw + (random.nextFloat() - 0.5f) * 2 * MAX_PIXEL_MOVE;
        targetPitch = centerPitch + (random.nextFloat() - 0.5f) * 2 * (MAX_PIXEL_MOVE * 0.7f);
        glideTimer = GLIDE_TICKS;
        isGliding = true;
    }
    
    private void doGlide() {
        if (glideTimer <= 0) {
            isGliding = false;
            return;
        }
        
        float progress = 1.0f - ((float) glideTimer / GLIDE_TICKS);
        
        if (progress < 0.5f) {
            // First half: move slightly away from center
            float t = progress * 2;
            mc.player.setYaw(MathHelper.lerp(t, centerYaw, targetYaw));
            mc.player.setPitch(MathHelper.lerp(t, centerPitch, targetPitch));
        } else {
            // Second half: move back to center
            float t = (progress - 0.5f) * 2;
            mc.player.setYaw(MathHelper.lerp(t, targetYaw, centerYaw));
            mc.player.setPitch(MathHelper.lerp(t, targetPitch, centerPitch));
        }
        
        glideTimer--;
    }
    
    public boolean isGliding() {
        return isGliding;
    }
}
