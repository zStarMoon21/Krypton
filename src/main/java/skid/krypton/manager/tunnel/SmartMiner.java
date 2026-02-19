package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class SmartMiner {
    private final MinecraftClient mc;
    private BlockPos currentTarget;
    private int breakProgress = 0;
    private int stuckCounter = 0;
    private static final int MAX_STUCK_TICKS = 40; // 2 seconds max to break a block
    
    public SmartMiner(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void mine() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        // Only mine if looking at a block
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
            stopMining();
            return;
        }
        
        BlockPos lookingAt = hit.getBlockPos();
        
        // Don't mine air
        if (mc.world.getBlockState(lookingAt).isAir()) {
            stopMining();
            return;
        }
        
        // If we're looking at a different block, reset progress
        if (currentTarget == null || !currentTarget.equals(lookingAt)) {
            currentTarget = lookingAt;
            breakProgress = 0;
            stuckCounter = 0;
        }
        
        // Mine the block
        mc.interactionManager.updateBlockBreakingProgress(lookingAt, hit.getSide());
        mc.player.swingHand(Hand.MAIN_HAND);
        breakProgress++;
        stuckCounter++;
        
        // Check if we're stuck (block not breaking)
        if (stuckCounter > MAX_STUCK_TICKS) {
            // Maybe the block is unbreakable, skip it
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
        breakProgress = 0;
        stuckCounter = 0;
    }
    
    public boolean isBlockMined(BlockPos pos) {
        if (mc.world == null) return true;
        return mc.world.getBlockState(pos).isAir();
    }
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
}
