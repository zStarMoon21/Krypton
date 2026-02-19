package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class PreciseMiner {
    private final MinecraftClient mc;
    private BlockPos currentTarget;
    private int cooldown;

    public PreciseMiner(MinecraftClient mc) {
        this.mc = mc;
    }

    public void mine() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (cooldown-- > 0) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
            stopMining();
            return;
        }

        BlockPos pos = hit.getBlockPos();
        if (mc.world.getBlockState(pos).isAir()) {
            stopMining();
            return;
        }

        // Start breaking if target changed
        if (currentTarget == null || !currentTarget.equals(pos)) {
            mc.player.swingHand(Hand.MAIN_HAND);
            currentTarget = pos;
        }

        mc.interactionManager.updateBlockBreakingProgress(pos, hit.getSide());
        cooldown = 2; // legit speed (10 CPS)
    }

    public void stopMining() {
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        currentTarget = null;
        cooldown = 0;
    }

    public boolean isBlockMined(BlockPos pos) {
        return mc.world == null || mc.world.getBlockState(pos).isAir();
    }

    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
}
