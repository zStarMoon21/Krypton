package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class SmartMiner {
    private final MinecraftClient mc;
    private BlockPos currentTarget;
    private int stuckCounter = 0;
    private static final int MAX_STUCK_TICKS = 40;
    
    public SmartMiner(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void mine() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
            stopMining();
            return;
        }
        
        BlockPos lookingAt = hit.getBlockPos();
        
        if (mc.world.getBlockState(lookingAt).isAir()) {
            stopMining();
            return;
        }
        
        if (currentTarget == null || !currentTarget.equals(lookingAt)) {
            currentTarget = lookingAt;
            stuckCounter = 0;
        }
        
        mc.interactionManager.updateBlockBreakingProgress(lookingAt, hit.getSide());
        mc.player.swingHand(Hand.MAIN_HAND);
        stuckCounter++;
        
        if (stuckCounter > MAX_STUCK_TICKS) {
            currentTarget = null;
            stuckCounter = 0;
            stopMining();
        }
    }
    
    public void stopMining() {
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        currentTarget = null;
        stuckCounter = 0;
    }
    
    public boolean isBlockMined(BlockPos pos) {
        if (mc.world == null) return true;
        return mc.world.getBlockState(pos).isAir();
    }
}
