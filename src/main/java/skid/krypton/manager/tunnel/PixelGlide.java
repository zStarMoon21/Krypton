package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class PixelGlide {
    private final MinecraftClient mc;
    private final Random random = new Random();

    private static final float MAX_MOVE = 0.15f;
    private static final int TICKS = 10;

    private float centerYaw, centerPitch;
    private float targetYaw, targetPitch;
    private int timer;

    public PixelGlide(MinecraftClient mc) {
        this.mc = mc;
    }

    public void update(BlockPos target) {
        if (mc.player == null || target == null) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d center = new Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        Vec3d dir = center.subtract(eye).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(Math.asin(-dir.y));

        centerYaw = MathHelper.wrapDegrees(yaw);
        centerPitch = pitch;

        if (timer <= 0) {
            targetYaw = centerYaw + random.nextFloat(-MAX_MOVE, MAX_MOVE);
            targetPitch = centerPitch + random.nextFloat(-MAX_MOVE, MAX_MOVE);
            timer = TICKS;
        }

        float t = (float) timer / TICKS;
        mc.player.setYaw(MathHelper.lerp(t, targetYaw, centerYaw));
        mc.player.setPitch(MathHelper.lerp(t, targetPitch, centerPitch));

        timer--;
    }
}
