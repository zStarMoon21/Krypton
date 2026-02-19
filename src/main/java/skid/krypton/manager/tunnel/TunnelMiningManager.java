package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class TunnelMiningManager {
    private final MinecraftClient mc;
    private BlockPos currentTarget = null;
    
    public TunnelMiningManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void mineBlock(BlockPos pos) {
        if (pos == null || mc.world == null || mc.player == null) return;
        if (mc.world.getBlockState(pos).isAir()) return;
        
        // Only mine if this is the block we're looking at
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos lookingAt = hitResult.getBlockPos();
            
            // Only mine if the target block matches what we're looking at
            if (lookingAt.equals(pos)) {
                mc.interactionManager.updateBlockBreakingProgress(pos, hitResult.getSide());
                mc.player.swingHand(Hand.MAIN_HAND);
                currentTarget = pos;
            }
        }
    }
    
    public void minePath(List<BlockPos> path) {
        if (path.isEmpty() || mc.player == null) return;
        
        // Get the block the player is currently looking at
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos lookingAt = hitResult.getBlockPos();
            
            // Check if the block we're looking at is in our path
            for (BlockPos pos : path) {
                if (pos.equals(lookingAt) && !isBlockMined(pos)) {
                    mineBlock(pos);
                    break;
                }
            }
        }
    }
    
    public boolean isBlockMined(BlockPos pos) {
        if (mc.world == null) return true;
        return mc.world.getBlockState(pos).isAir();
    }
    
    public boolean isPathFinished(List<BlockPos> path) {
        if (path.isEmpty()) return true;
        
        for (BlockPos pos : path) {
            if (!isBlockMined(pos)) {
                return false;
            }
        }
        return true;
    }
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
}
