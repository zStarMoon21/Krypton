package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class NaturalMovement {
    private final MinecraftClient mc;
    private final Random random = new Random();
    
    // Movement settings
    private static final float MAX_PIXEL_MOVE = 0.12f; // ~2-3 pixels
    private static final int GLIDE_TICKS = 12;
    
    // Glide state
    private float targetYaw, targetPitch;
    private float centerYaw, centerPitch;
    private int glideTimer = 0;
    private boolean isGliding = false;
    
    // Movement smoothing
    private float forwardVelocity = 0;
    private float strafeVelocity = 0;
    private static final float ACCELERATION = 0.15f;
    private static final float DECELERATION = 0.3f;
    
    public NaturalMovement(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void updateAim(BlockPos target) {
        if (target == null || mc.player == null) return;
        
        // Calculate perfect center
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d blockCenter = new Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        Vec3d direction = blockCenter.subtract(eyePos).normalize();
        
        float newCenterYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float newCenterPitch = (float) Math.toDegrees(Math.asin(-direction.y));
        
        newCenterYaw = MathHelper.wrapDegrees(newCenterYaw);
        
        // Start new glide if target changed
        if (centerYaw != newCenterYaw || centerPitch != newCenterPitch) {
            centerYaw = newCenterYaw;
            centerPitch = newCenterPitch;
            startGlide();
        }
        
        if (isGliding) {
            performGlide();
        } else {
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
    
    private void performGlide() {
        if (glideTimer <= 0) {
            isGliding = false;
            return;
        }
        
        float progress = 1.0f - ((float) glideTimer / GLIDE_TICKS);
        
        if (progress < 0.5f) {
            // Move away from center
            float t = progress * 2;
            mc.player.setYaw(MathHelper.lerp(t, centerYaw, targetYaw));
            mc.player.setPitch(MathHelper.lerp(t, centerPitch, targetPitch));
        } else {
            // Move back to center
            float t = (progress - 0.5f) * 2;
            mc.player.setYaw(MathHelper.lerp(t, targetYaw, centerYaw));
            mc.player.setPitch(MathHelper.lerp(t, targetPitch, centerPitch));
        }
        
        glideTimer--;
    }
    
    public void updateMovement(boolean shouldMove, boolean needsStrafe, float strafeDirection) {
        // Smooth forward movement
        if (shouldMove) {
            forwardVelocity = Math.min(forwardVelocity + ACCELERATION, 1.0f);
        } else {
            forwardVelocity = Math.max(forwardVelocity - DECELERATION, 0);
        }
        
        // Smooth strafe movement
        if (needsStrafe) {
            strafeVelocity = strafeDirection * Math.min(Math.abs(strafeVelocity) + ACCELERATION, 1.0f);
        } else {
            strafeVelocity *= (1 - DECELERATION);
            if (Math.abs(strafeVelocity) < 0.01f) strafeVelocity = 0;
        }
        
        // Apply movement
        mc.options.forwardKey.setPressed(forwardVelocity > 0.5f);
        
        if (strafeVelocity > 0.1f) {
            mc.options.rightKey.setPressed(true);
            mc.options.leftKey.setPressed(false);
        } else if (strafeVelocity < -0.1f) {
            mc.options.rightKey.setPressed(false);
            mc.options.leftKey.setPressed(true);
        } else {
            mc.options.rightKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
        }
    }
    
    public void stopAll() {
        forwardVelocity = 0;
        strafeVelocity = 0;
        mc.options.forwardKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
    }
    
    public boolean isGliding() {
        return isGliding;
    }
}
