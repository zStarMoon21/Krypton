package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class PixelGlideManager {
    private final MinecraftClient mc;
    private final Random random = new Random();
    
    private static final float MAX_OFFSET = 0.15f; // ~3-4 pixels
    private static final int GLIDE_DURATION = 10;
    
    private float targetYaw;
    private float targetPitch;
    private float startYaw;
    private float startPitch;
    private float centerYaw;
    private float centerPitch;
    private int glideTimer = 0;
    private int glidePhase = 0;
    private boolean isGliding = false;
    
    public PixelGlideManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void glideToCenter(BlockPos targetBlock) {
        if (targetBlock == null || mc.player == null) return;
        
        Vec3d blockCenter = new Vec3d(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d direction = blockCenter.subtract(playerPos).normalize();
        
        float newCenterPitch = (float) Math.toDegrees(Math.asin(-direction.y));
        float newCenterYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        
        while (newCenterYaw - mc.player.getYaw() > 180) newCenterYaw -= 360;
        while (newCenterYaw - mc.player.getYaw() < -180) newCenterYaw += 360;
        
        if (centerYaw != newCenterYaw || centerPitch != newCenterPitch) {
            centerYaw = newCenterYaw;
            centerPitch = newCenterPitch;
            startNewGlide();
        }
        
        if (isGliding) {
            performGlide();
        } else {
            mc.player.setYaw(centerYaw);
            mc.player.setPitch(centerPitch);
        }
    }
    
    private void startNewGlide() {
        startYaw = mc.player.getYaw();
        startPitch = mc.player.getPitch();
        
        float yawOffset = (random.nextFloat() - 0.5f) * 2 * MAX_OFFSET;
        float pitchOffset = (random.nextFloat() - 0.5f) * 2 * (MAX_OFFSET * 0.7f);
        
        targetYaw = centerYaw + yawOffset;
        targetPitch = centerPitch + pitchOffset;
        
        glideTimer = GLIDE_DURATION;
        glidePhase = 0;
        isGliding = true;
    }
    
    private void performGlide() {
        if (glideTimer <= 0) {
            isGliding = false;
            return;
        }
        
        float progress = 1.0f - ((float) glideTimer / GLIDE_DURATION);
        
        if (glidePhase == 0) {
            if (progress < 0.5f) {
                float t = progress * 2;
                mc.player.setYaw(MathHelper.lerp(t, startYaw, targetYaw));
                mc.player.setPitch(MathHelper.lerp(t, startPitch, targetPitch));
            } else {
                glidePhase = 1;
            }
        } else {
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
