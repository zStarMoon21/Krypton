package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.Blocks;
import net.minecraft.block.KelpBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.WorldChunk;

public class KelpGapDetector {

    public boolean isOceanChunk(WorldChunk chunk) {
        var biome = chunk.getBiomeForNoiseGen(8, 0, 8).value();
        return biome == BiomeKeys.OCEAN || 
               biome == BiomeKeys.DEEP_OCEAN || 
               biome == BiomeKeys.COLD_OCEAN || 
               biome == BiomeKeys.DEEP_COLD_OCEAN ||
               biome == BiomeKeys.FROZEN_OCEAN || 
               biome == BiomeKeys.DEEP_FROZEN_OCEAN ||
               biome == BiomeKeys.LUKEWARM_OCEAN || 
               biome == BiomeKeys.DEEP_LUKEWARM_OCEAN ||
               biome == BiomeKeys.WARM_OCEAN;
    }

    public int scanForKelpGaps(WorldChunk chunk, ChunkData data) {
        int score = 0;
        int kelpCount = 0;
        int waterDepth = 0;

        // Check water depth and count kelp
        for (int x = 0; x < 16; x += 4) { // Sample every 4 blocks
            for (int z = 0; z < 16; z += 4) {
                int topY = chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, x, z);
                BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, topY, chunk.getPos().getStartZ() + z);

                // Check depth
                int seaLevel = chunk.getWorld().getSeaLevel();
                if (seaLevel - topY > 10) {
                    waterDepth++;
                }

                // Count kelp
                for (int y = topY; y <= seaLevel; y++) {
                    BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
                    if (chunk.getBlockState(checkPos).getBlock() instanceof KelpBlock) {
                        kelpCount++;
                        break;
                    }
                }
            }
        }

        // Deep ocean with few kelp = suspicious
        if (waterDepth > 5 && kelpCount < 5) {
            score += 10;
        }

        // Check for cut patterns (flat tops, straight lines)
        if (hasCutPattern(chunk)) {
            score += 8;
        }

        return score;
    }

    private boolean hasCutPattern(WorldChunk chunk) {
        int flatTops = 0;
        int straightLines = 0;

        // Detect flat kelp tops (kelp that doesn't reach surface)
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                int topY = chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, x, z);
                int seaLevel = chunk.getWorld().getSeaLevel();

                // Check if kelp stops well below surface
                boolean hasKelp = false;
                int highestKelp = 0;

                for (int y = topY; y <= seaLevel; y++) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    if (chunk.getBlockState(pos).getBlock() instanceof KelpBlock) {
                        hasKelp = true;
                        highestKelp = y;
                    }
                }

                if (hasKelp && seaLevel - highestKelp > 5) {
                    flatTops++;
                }
            }
        }

        // Detect straight line patterns (possible paths)
        for (int x = 0; x < 16; x++) {
            int kelpInLine = 0;
            for (int z = 0; z < 16; z++) {
                int topY = chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, x, z);
                boolean hasKelp = false;
                for (int y = topY; y <= chunk.getWorld().getSeaLevel(); y++) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    if (chunk.getBlockState(pos).getBlock() instanceof KelpBlock) {
                        hasKelp = true;
                        break;
                    }
                }
                if (!hasKelp) kelpInLine = 0;
                else kelpInLine++;
            }
            if (kelpInLine > 10) straightLines++;
        }

        return flatTops > 10 || straightLines > 3;
    }
}
