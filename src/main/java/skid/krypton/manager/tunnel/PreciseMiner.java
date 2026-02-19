package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class PreciseMiner {
    private final MinecraftClient mc;
    private BlockPos currentTarget;
    private int miningCooldown = 0;
    
    public PreciseMiner(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void mine() {
        if (mc.player == null || mc.world == null) return;
        
        // Regular mining speed - no speed hacks
        if (miningCooldown > 0) {
            miningCooldown--;
            return;
        }
        
        // Only mine if looking at a block
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos lookingAt = hitResult.getBlockPos();
            
            // Mine the block we're looking at
            if (!mc.world.getBlockState(lookingAt).isAir()) {
                mc.interactionManager.updateBlockBreakingProgress(lookingAt, hitResult.getSide());
                mc.player.swingHand(Hand.MAIN_HAND);
                currentTarget = lookingAt;
                miningCooldown = 4; // Small delay between mining attempts (normal speed)
            }
        } else {
            stopMining();
        }
    }
    
    public void stopMining() {
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        currentTarget = null;
        miningCooldown = 0;
    }
    
    public boolean isBlockMined(BlockPos pos) {
        if (mc.world == null) return true;
        return mc.world.getBlockState(pos).isAir();
    }
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
}
