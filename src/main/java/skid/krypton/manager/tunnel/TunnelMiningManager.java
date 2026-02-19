package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class TunnelMiningManager {
    private final MinecraftClient mc;
    private BlockPos currentTarget = null;
    private int breakProgress = 0;
    private int breakCooldown = 0;
    
    public TunnelMiningManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void mineTarget(BlockPos target) {
        if (target == null || mc.world == null || mc.player == null) return;
        
        // Check if target is already mined
        if (mc.world.getBlockState(target).isAir()) {
            currentTarget = null;
            return;
        }
        
        // Check if we're looking at the target
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos lookingAt = hitResult.getBlockPos();
            
            // Only mine if we're looking at our target
            if (lookingAt.equals(target)) {
                // Mine the block
                mc.interactionManager.updateBlockBreakingProgress(target, hitResult.getSide());
                mc.player.swingHand(Hand.MAIN_HAND);
                currentTarget = target;
                breakProgress++;
                breakCooldown = 0;
            } else {
                // Not looking at target, reset progress
                mc.interactionManager.cancelBlockBreaking();
                breakProgress = 0;
            }
        } else {
            // Not looking at any block
            mc.interactionManager.cancelBlockBreaking();
            breakProgress = 0;
        }
    }
    
    public void resetMining() {
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        breakProgress = 0;
        currentTarget = null;
    }
    
    public boolean isBlockMined(BlockPos pos) {
        if (mc.world == null) return true;
        return mc.world.getBlockState(pos).isAir();
    }
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
}package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class TunnelMiningManager {
    private final MinecraftClient mc;
    private BlockPos currentTarget = null;
    private int breakProgress = 0;
    private int breakCooldown = 0;
    
    public TunnelMiningManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void mineTarget(BlockPos target) {
        if (target == null || mc.world == null || mc.player == null) return;
        
        // Check if target is already mined
        if (mc.world.getBlockState(target).isAir()) {
            currentTarget = null;
            return;
        }
        
        // Check if we're looking at the target
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos lookingAt = hitResult.getBlockPos();
            
            // Only mine if we're looking at our target
            if (lookingAt.equals(target)) {
                // Mine the block
                mc.interactionManager.updateBlockBreakingProgress(target, hitResult.getSide());
                mc.player.swingHand(Hand.MAIN_HAND);
                currentTarget = target;
                breakProgress++;
                breakCooldown = 0;
            } else {
                // Not looking at target, reset progress
                mc.interactionManager.cancelBlockBreaking();
                breakProgress = 0;
            }
        } else {
            // Not looking at any block
            mc.interactionManager.cancelBlockBreaking();
            breakProgress = 0;
        }
    }
    
    public void resetMining() {
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        breakProgress = 0;
        currentTarget = null;
    }
    
    public boolean isBlockMined(BlockPos pos) {
        if (mc.world == null) return true;
        return mc.world.getBlockState(pos).isAir();
    }
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
}
