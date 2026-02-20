package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public class PillarDetector {

    public int scanForPillars(WorldChunk chunk, ChunkData data) {
        int score = 0;
        int pillarCount = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = 0;
                Block lastBlock = null;
                BlockPos.Mutable mutable = new BlockPos.Mutable(
                    chunk.getPos().getStartX() + x,
                    chunk.getBottomY(),
                    chunk.getPos().getStartZ() + z
                );

                // Scan upward through the column
                for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
                    mutable.setY(y);
                    Block currentBlock = chunk.getBlockState(mutable).getBlock();

                    // Skip air
                    if (currentBlock == Blocks.AIR) {
                        if (height >= 6) {
                            // Check if this was a valid pillar
                            if (isValidPillar(lastBlock)) {
                                data.addPillar(mutable.asLong());
                                pillarCount++;
                            }
                        }
                        height = 0;
                        lastBlock = null;
                        continue;
                    }

                    // Same block type continues pillar
                    if (currentBlock == lastBlock) {
                        height++;
                    } else {
                        // New block type, check if previous was a pillar
                        if (height >= 6 && isValidPillar(lastBlock)) {
                            data.addPillar(mutable.asLong());
                            pillarCount++;
                        }
                        height = 1;
                        lastBlock = currentBlock;
                    }
                }

                // Check at the end
                if (height >= 6 && isValidPillar(lastBlock)) {
                    data.addPillar(mutable.asLong());
                    pillarCount++;
                }
            }
        }

        // Score based on pillar count
        if (pillarCount > 10) {
            score += 10;
        } else if (pillarCount > 5) {
            score += 7;
        } else if (pillarCount > 2) {
            score += 4;
        }

        return score;
    }

    private boolean isValidPillar(Block block) {
        if (block == null) return false;

        // Exclude trees
        if (isTreeBlock(block)) return false;

        // Exclude dripstone
        if (block == Blocks.POINTED_DRIPSTONE || block == Blocks.DRIPSTONE_BLOCK) {
            return false;
        }

        // Include common building blocks
        return true;
    }

    private boolean isTreeBlock(Block block) {
        return block == Blocks.OAK_LOG ||
               block == Blocks.SPRUCE_LOG ||
               block == Blocks.BIRCH_LOG ||
               block == Blocks.JUNGLE_LOG ||
               block == Blocks.ACACIA_LOG ||
               block == Blocks.DARK_OAK_LOG ||
               block == Blocks.MANGROVE_LOG ||
               block == Blocks.CHERRY_LOG ||
               block == Blocks.STRIPPED_OAK_LOG ||
               block == Blocks.STRIPPED_SPRUCE_LOG ||
               block == Blocks.STRIPPED_BIRCH_LOG ||
               block == Blocks.STRIPPED_JUNGLE_LOG ||
               block == Blocks.STRIPPED_ACACIA_LOG ||
               block == Blocks.STRIPPED_DARK_OAK_LOG ||
               block == Blocks.STRIPPED_MANGROVE_LOG ||
               block == Blocks.STRIPPED_CHERRY_LOG;
    }
}
