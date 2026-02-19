package skid.krypton.module.modules.donut.tunnel;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class TunnelMiner {
    private final MinecraftClient mc;
    
    public TunnelMiner(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void minePath(List<BlockPos> path) {
        if (path.isEmpty()) return;
        
        for (BlockPos pos : path) {
            if (!isBlockMined(pos)) {
                mineBlock(pos);
                break;
            }
        }
    }
    
    public void mineBlock(BlockPos pos) {
        if (pos == null || mc.world == null || mc.player == null) return;
        if (mc.world.getBlockState(pos).isAir()) return;
        
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        
        mc.interactionManager.updateBlockBreakingProgress(pos, hitResult.getSide());
        mc.player.swingHand(Hand.MAIN_HAND);
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
}
