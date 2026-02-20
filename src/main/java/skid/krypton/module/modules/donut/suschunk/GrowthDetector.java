package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public class GrowthDetector {

    public int scanChunk(WorldChunk chunk, ChunkData data) {
        int score = 0;

        // Scan only every 2 blocks for performance
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                for (int y = chunk.getBottomY(); y < chunk.getTopY(); y += 2) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    Block block = chunk.getBlockState(pos).getBlock();

                    // Bamboo (≥12 tall)
                    if (block instanceof BambooBlock && isTallBamboo(chunk, pos)) {
                        score += 4;
                    }
                    // Kelp (≥15 tall)
                    else if (block instanceof KelpPlantBlock && isTallKelp(chunk, pos)) {
                        score += 3;
                    }
                    // Sugar cane farm
                    else if (block instanceof SugarCaneBlock && isSugarCaneFarm(chunk, pos)) {
                        score += 6;
                    }
                    // Amethyst cluster
                    else if (block instanceof AmethystClusterBlock) {
                        score += 5;
                    }
                    // Dripstone chain
                    else if (block instanceof PointedDripstoneBlock && isLargeDripstoneChain(chunk, pos)) {
                        score += 5;
                    }
                    // Long vines
                    else if (block instanceof VineBlock && isLongVine(chunk, pos)) {
                        score += 2;
                    }
                }
            }
        }

        return score;
    }

    private boolean isTallBamboo(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();
        for (int i = 0; i < 15; i++) {
            if (!(chunk.getBlockState(mutable).getBlock() instanceof BambooBlock)) break;
            height++;
            mutable.move(0, -1, 0);
        }
        return height >= 12;
    }

    private boolean isTallKelp(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();
        for (int i = 0; i < 20; i++) {
            if (!(chunk.getBlockState(mutable).getBlock() instanceof KelpPlantBlock)) break;
            height++;
            mutable.move(0, 1, 0);
        }
        return height >= 15;
    }

    private boolean isSugarCaneFarm(WorldChunk chunk, BlockPos pos) {
        BlockPos below = pos.down();
        Block belowBlock = chunk.getBlockState(below).getBlock();

        // Check for sand, grass, or farmland by block type
        if (!(belowBlock == Blocks.SAND || 
              belowBlock == Blocks.RED_SAND ||
              belowBlock instanceof GrassBlock || 
              belowBlock == Blocks.FARMLAND)) {
            return false;
        }

        // Quick water check (only check 4 directions)
        return chunk.getBlockState(below.north()).getFluidState().isStill() ||
               chunk.getBlockState(below.south()).getFluidState().isStill() ||
               chunk.getBlockState(below.east()).getFluidState().isStill() ||
               chunk.getBlockState(below.west()).getFluidState().isStill();
    }

    private boolean isLargeDripstoneChain(WorldChunk chunk, BlockPos pos) {
        int height = 1;
        BlockPos.Mutable mutable = pos.mutableCopy();
        
        mutable.move(0, 1, 0);
        while (chunk.getBlockState(mutable).getBlock() instanceof PointedDripstoneBlock) {
            height++;
            mutable.move(0, 1, 0);
        }
        
        mutable.set(pos);
        mutable.move(0, -1, 0);
        while (chunk.getBlockState(mutable).getBlock() instanceof PointedDripstoneBlock) {
            height++;
            mutable.move(0, -1, 0);
        }
        
        return height >= 8;
    }

    private boolean isLongVine(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();
        for (int i = 0; i < 15; i++) {
            if (!(chunk.getBlockState(mutable).getBlock() instanceof VineBlock)) break;
            height++;
            mutable.move(0, -1, 0);
        }
        return height >= 10;
    }
}
