  package skid.krypton.manager.tunnel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class HazardAvoidanceManager {
    private final MinecraftClient mc;
    
    // Hazards to detect
    private static final Block[] HAZARDS = {Blocks.LAVA, Blocks.WATER, Blocks.GRAVEL};
    
    public HazardAvoidanceManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public BlockPos detectHazard() {
        if (mc.player == null) return null;
        
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Check area around player for hazards
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 4; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(checkPos).getBlock();
                    
                    for (Block hazard : HAZARDS) {
                        if (block == hazard) {
                            return checkPos;
                        }
                    }
                }
            }
        }
        return null;
    }
}
