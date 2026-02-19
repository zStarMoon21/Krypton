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
    private int breakProgress = 0;
    
    public TunnelMiningManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void mineBlock(BlockPos pos) {
        if (pos == null || mc.world == null || mc.player == null) return;
        if (mc.world.getBlockState(pos).isAir()) {
            currentTarget = null;
            return;
        }
        
        // Always try to mine the target block, even if not looking exactly at it
        // The player will naturally look at it due to mouse glide
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        
        mc.interactionManager.updateBlockBreakingProgress(pos, hitResult.getSide());
        mc.player.swingHand(Hand.MAIN_HAND);
        currentTarget = pos;
        breakProgress++;
    }
    
    public void minePath(List<BlockPos> path) {
        if (path.isEmpty() || mc.player == null) return;
        
        BlockPos target = path.get(0);
        
        // Only mine if we're close enough
        double distance = mc.player.getBlockPos().getManhattanDistance(target);
        if (distance > 5) return;
        
        mineBlock(target);
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
