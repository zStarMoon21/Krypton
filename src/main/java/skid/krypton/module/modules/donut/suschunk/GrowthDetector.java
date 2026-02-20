package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public class GrowthDetector {

    public int scanChunk(WorldChunk chunk, ChunkData data) {
        int score = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
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
        while (chunk.getBlockState(mutable).getBlock() instanceof BambooBlock) {
            height++;
            mutable.move(0, -1, 0);
        }
        return height >= 12;
    }

    private boolean isTallKelp(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();
        while (chunk.getBlockState(mutable).getBlock() instanceof KelpPlantBlock) {
            height++;
            mutable.move(0, 1, 0);
        }
        return height >= 15;
    }

    private boolean isSugarCaneFarm(WorldChunk chunk, BlockPos pos) {
        BlockPos below = pos.down();
        Block belowBlock = chunk.getBlockState(below).getBlock();

        if (!(belowBlock instanceof SandBlock || belowBlock instanceof GrassBlock || belowBlock instanceof FarmlandBlock)) {
            return false;
        }

        // Check for adjacent water
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (chunk.getBlockState(below.add(dx, 0, dz)).getFluidState().isStill()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isLargeDripstoneChain(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();
        
        mutable.set(pos);
        while (chunk.getBlockState(mutable).getBlock() instanceof PointedDripstoneBlock) {
            height++;
            mutable.move(0, 1, 0);
        }
        
        mutable.set(pos);
        while (chunk.getBlockState(mutable).getBlock() instanceof PointedDripstoneBlock) {
            height++;
            mutable.move(0, -1, 0);
        }
        
        return height >= 8;
    }

    private boolean isLongVine(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();
        while (chunk.getBlockState(mutable).getBlock() instanceof VineBlock) {
            height++;
            mutable.move(0, -1, 0);
        }
        return height >= 10;
    }
}
