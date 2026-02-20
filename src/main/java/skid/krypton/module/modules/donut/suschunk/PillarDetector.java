package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public class PillarDetector {

    public int scanForPillars(WorldChunk chunk, ChunkData data) {
        int pillarCount = 0;

        // Scan every 2 blocks for performance
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                int height = 0;
                Block lastBlock = null;
                BlockPos.Mutable mutable = new BlockPos.Mutable(
                    chunk.getPos().getStartX() + x,
                    chunk.getBottomY(),
                    chunk.getPos().getStartZ() + z
                );

                // Scan every 2 blocks vertically too
                for (int y = chunk.getBottomY(); y < chunk.getTopY(); y += 2) {
                    mutable.setY(y);
                    Block block = chunk.getBlockState(mutable).getBlock();

                    if (block == Blocks.AIR) {
                        if (height >= 6 && isValidPillar(lastBlock)) {
                            pillarCount++;
                        }
                        height = 0;
                        lastBlock = null;
                        continue;
                    }

                    if (block == lastBlock) {
                        height += 2; // We're scanning every 2 blocks
                    } else {
                        if (height >= 6 && isValidPillar(lastBlock)) {
                            pillarCount++;
                        }
                        height = 2;
                        lastBlock = block;
                    }
                }

                if (height >= 6 && isValidPillar(lastBlock)) {
                    pillarCount++;
                }
            }
        }

        // Scale score based on pillar count
        if (pillarCount > 15) return 10;
        if (pillarCount > 10) return 8;
        if (pillarCount > 5) return 6;
        if (pillarCount > 2) return 4;
        return 0;
    }

    private boolean isValidPillar(Block block) {
        if (block == null) return false;
        if (isTreeBlock(block)) return false;
        if (block == Blocks.POINTED_DRIPSTONE || block == Blocks.DRIPSTONE_BLOCK) return false;
        return true;
    }

    private boolean isTreeBlock(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG ||
               block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG ||
               block == Blocks.MANGROVE_LOG || block == Blocks.CHERRY_LOG;
    }
}
