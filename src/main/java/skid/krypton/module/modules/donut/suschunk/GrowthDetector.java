package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public class GrowthDetector {

    public int scanChunk(WorldChunk chunk, ChunkData data) {
        int score = 0;

        // Scan the chunk for max-grown blocks
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    BlockState state = chunk.getBlockState(pos);
                    Block block = state.getBlock();

                    // Bamboo detection (≥12 blocks tall)
                    if (block instanceof BambooBlock && isTallBamboo(chunk, pos)) {
                        data.addTallBamboo(pos.asLong());
                        score += 4;
                    }
                    // Kelp detection (≥15 blocks tall)
                    else if (block instanceof KelpBlock && isTallKelp(chunk, pos)) {
                        data.addTallKelp(pos.asLong());
                        score += 3;
                    }
                    // Sugar cane farm pattern
                    else if (block instanceof SugarCaneBlock && isSugarCaneFarm(chunk, pos)) {
                        data.addSugarCaneFarm(pos.asLong());
                        score += 6;
                    }
                    // Amethyst cluster (fully grown)
                    else if (block instanceof AmethystClusterBlock && isFullyGrownAmethyst(state)) {
                        data.addAmethystCluster(pos.asLong());
                        score += 5;
                    }
                    // Large dripstone chains
                    else if (block instanceof PointedDripstoneBlock && isLargeDripstoneChain(chunk, pos)) {
                        data.addLargeDripstone(pos.asLong());
                        score += 5;
                    }
                    // Long vines (≥10 blocks)
                    else if (block instanceof VineBlock && isLongVine(chunk, pos)) {
                        data.addLongVine(pos.asLong());
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

        // Count bamboo blocks downward
        while (chunk.getBlockState(mutable).getBlock() instanceof BambooBlock) {
            height++;
            mutable.move(0, -1, 0);
        }

        return height >= 12;
    }

    private boolean isTallKelp(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();

        // Count kelp blocks upward
        while (chunk.getBlockState(mutable).getBlock() instanceof KelpBlock) {
            height++;
            mutable.move(0, 1, 0);
        }

        return height >= 15;
    }

    private boolean isSugarCaneFarm(WorldChunk chunk, BlockPos pos) {
        // Check if sugar cane is planted on sand/grass in a farm-like pattern
        BlockPos below = pos.down();
        BlockState belowState = chunk.getBlockState(below);

        // Sugar cane must be on sand, grass, or dirt
        if (!(belowState.getBlock() instanceof SandBlock || 
              belowState.getBlock() instanceof GrassBlock || 
              belowState.getBlock() instanceof FarmlandBlock)) {
            return false;
        }

        // Check for adjacent water (farms need water)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos checkPos = below.add(dx, 0, dz);
                if (chunk.getBlockState(checkPos).getBlock() instanceof FluidBlock) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isFullyGrownAmethyst(BlockState state) {
        if (state.getBlock() instanceof AmethystClusterBlock) {
            // Check if it's a full cluster (buds are smaller)
            return state.get(AmethystClusterBlock.FACING) != null;
        }
        return false;
    }

    private boolean isLargeDripstoneChain(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();

        // Count dripstone blocks upward and downward
        while (chunk.getBlockState(mutable).getBlock() instanceof PointedDripstoneBlock) {
            height++;
            mutable.move(0, 1, 0);
        }

        mutable.set(pos);
        while (chunk.getBlockState(mutable).getBlock() instanceof PointedDripstoneBlock) {
            height++;
            mutable.move(0, -1, 0);
        }

        return height >= 8; // Large chain
    }

    private boolean isLongVine(WorldChunk chunk, BlockPos pos) {
        int height = 0;
        BlockPos.Mutable mutable = pos.mutableCopy();

        // Count vines downward
        while (chunk.getBlockState(mutable).getBlock() instanceof VineBlock) {
            height++;
            mutable.move(0, -1, 0);
        }

        return height >= 10;
    }
}
