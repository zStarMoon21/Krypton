package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.KelpPlantBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.WorldChunk;

public class KelpGapDetector {

    public boolean isOceanChunk(WorldChunk chunk) {
        RegistryEntry<Biome> biomeEntry = chunk.getBiomeForNoiseGen(8, 0, 8);
        RegistryKey<Biome> biomeKey = biomeEntry.getKey().orElse(null);

        return biomeKey == BiomeKeys.OCEAN ||
               biomeKey == BiomeKeys.DEEP_OCEAN ||
               biomeKey == BiomeKeys.COLD_OCEAN ||
               biomeKey == BiomeKeys.DEEP_COLD_OCEAN ||
               biomeKey == BiomeKeys.FROZEN_OCEAN ||
               biomeKey == BiomeKeys.DEEP_FROZEN_OCEAN ||
               biomeKey == BiomeKeys.LUKEWARM_OCEAN ||
               biomeKey == BiomeKeys.DEEP_LUKEWARM_OCEAN ||
               biomeKey == BiomeKeys.WARM_OCEAN;
    }

    public int scanForKelpGaps(WorldChunk chunk, ChunkData data) {
        int score = 0;
        int kelpCount = 0;
        int deepWaterCount = 0;

        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                int topY = chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, x, z);
                int seaLevel = chunk.getWorld().getSeaLevel();

                if (seaLevel - topY > 10) {
                    deepWaterCount++;
                }

                for (int y = topY; y <= seaLevel; y++) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    if (chunk.getBlockState(pos).getBlock() instanceof KelpPlantBlock) {
                        kelpCount++;
                        break;
                    }
                }
            }
        }

        if (deepWaterCount > 5 && kelpCount < 5) {
            score += 10;
        }

        if (hasCutPattern(chunk)) {
            score += 8;
        }

        return score;
    }

    private boolean hasCutPattern(WorldChunk chunk) {
        int flatTops = 0;

        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                int topY = chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, x, z);
                int seaLevel = chunk.getWorld().getSeaLevel();
                int highestKelp = 0;

                for (int y = topY; y <= seaLevel; y++) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    if (chunk.getBlockState(pos).getBlock() instanceof KelpPlantBlock) {
                        highestKelp = y;
                    }
                }

                if (highestKelp > 0 && seaLevel - highestKelp > 5) {
                    flatTops++;
                }
            }
        }

        return flatTops > 10;
    }
}
