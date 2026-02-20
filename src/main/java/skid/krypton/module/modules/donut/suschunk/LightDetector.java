package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;

public class LightDetector {

    public int scanForLightPatterns(WorldChunk chunk, ChunkData data) {
        int lightCount = 0;

        // Scan only every 2 blocks for performance
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                for (int y = chunk.getBottomY(); y <= 8; y += 2) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);

                    if (chunk.getBlockState(pos).isAir()) continue;
                    if (isNaturalLight(chunk, pos)) continue;

                    if (chunk.getWorld().getLightLevel(LightType.BLOCK, pos) >= 10) {
                        lightCount++;
                    }
                }
            }
        }

        // Scale score based on light count
        if (lightCount > 30) return 12;
        if (lightCount > 20) return 10;
        if (lightCount > 10) return 8;
        if (lightCount > 5) return 5;
        return 0;
    }

    private boolean isNaturalLight(WorldChunk chunk, BlockPos pos) {
        var block = chunk.getBlockState(pos).getBlock();
        return block == Blocks.LAVA || block == Blocks.GLOW_LICHEN;
    }
}
