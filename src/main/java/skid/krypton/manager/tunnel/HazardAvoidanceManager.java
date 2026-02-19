package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class HazardAvoidanceManager {
    private final MinecraftClient mc;

    private static final Block[] HAZARDS = {
            Blocks.LAVA, Blocks.WATER, Blocks.GRAVEL
    };

    public HazardAvoidanceManager(MinecraftClient mc) {
        this.mc = mc;
    }

    public BlockPos detectHazard() {
        if (mc.player == null || mc.world == null) return null;

        BlockPos base = mc.player.getBlockPos();

        for (BlockPos pos : BlockPos.iterate(base.add(-2, -1, -2), base.add(2, 2, 2))) {
            Block block = mc.world.getBlockState(pos).getBlock();
            for (Block hazard : HAZARDS) {
                if (block == hazard) {
                    return pos.toImmutable();
                }
            }
        }
        return null;
    }
}
