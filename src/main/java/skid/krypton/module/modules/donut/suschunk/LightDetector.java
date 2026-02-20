package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;

public class LightDetector {

    public int scanForLightPatterns(WorldChunk chunk, ChunkData data) {
        int score = 0;
        int lightClusterCount = 0;

        // Scan underground (Y ≤ 8)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y <= 8; y++) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);

                    // Skip if block is air
                    if (chunk.getBlockState(pos).isAir()) continue;

                    // Skip natural light sources
                    if (isNaturalLightSource(chunk, pos)) continue;

                    // Check light level
                    int lightLevel = chunk.getWorld().getLightLevel(LightType.BLOCK, pos);
                    if (lightLevel >= 10) {
                        data.addLightSource(pos.asLong());
                        lightClusterCount++;
                    }
                }
            }
        }

        // Score based on light cluster size and pattern
        if (lightClusterCount > 20) {
            score += 12; // Large light cluster
        } else if (lightClusterCount > 10) {
            score += 8; // Medium light cluster
        } else if (lightClusterCount > 5) {
            score += 4; // Small light cluster
        }

        // Bonus for grid patterns
        if (hasGridPattern(data)) {
            score += 6;
        }

        return score;
    }

    private boolean isNaturalLightSource(WorldChunk chunk, BlockPos pos) {
        var block = chunk.getBlockState(pos).getBlock();

        // Lava
        if (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) {
            return true;
        }

        // Glow lichen
        if (block == Blocks.GLOW_LICHEN) {
            return true;
        }

        // Other common natural light sources
        if (block == Blocks.GLOWSTONE ||
            block == Blocks.SHROOMLIGHT ||
            block == Blocks.JACK_O_LANTERN ||
            block == Blocks.LANTERN ||
            block == Blocks.TORCH ||
            block == Blocks.REDSTONE_TORCH ||
            block == Blocks.SEA_LANTERN ||
            block == Blocks.END_ROD) {
            return false; // These are player-placed
        }

        return false;
    }

    private boolean hasGridPattern(ChunkData data) {
        // Simple grid detection - look for evenly spaced light sources
        int count = data.getLightSourceCount();
        if (count < 9) return false;

        // In a real implementation, you'd check for 3x3 grid patterns
        // This is a simplified version
        return count > 16; // Rough indicator of grid
    }
}
