package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import skid.krypton.utils.RenderUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class HoleRenderer {
    
    private final MinecraftClient mc;
    
    // Bright, solid colors
    private static final Color HOLE_1x1_COLOR = new Color(255, 0, 0, 255);
    private static final Color HOLE_2x1_COLOR = new Color(255, 165, 0, 255);
    private static final Color HOLE_3x1_COLOR = new Color(255, 255, 0, 255);
    
    private int scanCooldown = 0;
    private final List<Hole> holes = new ArrayList<>();
    
    public HoleRenderer(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void renderHoles(MatrixStack matrices) {
        if (mc.world == null || mc.player == null) return;
        
        // Scan for holes every 20 ticks
        if (--scanCooldown <= 0) {
            scanHoles();
            scanCooldown = 20;
        }
        
        if (holes.isEmpty()) return;
        
        // Save matrix state and apply camera transform
        matrices.push();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        // Render all holes
        for (Hole hole : holes) {
            renderHole(matrices, hole);
        }
        
        matrices.pop();
    }
    
    private void scanHoles() {
        holes.clear();
        if (mc.player == null) return;
        
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = 20;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -10; y <= 5; y++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    if (isOneByOneHole(checkPos)) {
                        holes.add(new Hole(checkPos, HoleType.ONE_BY_ONE));
                    }
                    else if (isTwoByOneHole(checkPos)) {
                        holes.add(new Hole(checkPos, HoleType.TWO_BY_ONE));
                    }
                    else if (isThreeByOneHole(checkPos)) {
                        holes.add(new Hole(checkPos, HoleType.THREE_BY_ONE));
                    }
                }
            }
        }
    }
    
    private boolean isOneByOneHole(BlockPos pos) {
        // Check if it's a 1x1 shaft going down at least 3 blocks
        for (int i = 0; i < 3; i++) {
            if (!mc.world.getBlockState(pos.down(i)).isAir()) {
                return false;
            }
        }
        
        // Check surrounding blocks (should be solid)
        return isSolidBlock(pos.down().north()) &&
               isSolidBlock(pos.down().south()) &&
               isSolidBlock(pos.down().east()) &&
               isSolidBlock(pos.down().west());
    }
    
    private boolean isTwoByOneHole(BlockPos pos) {
        // Check for 2x1 hole (2 blocks wide)
        for (int i = 0; i < 3; i++) {
            if (!mc.world.getBlockState(pos.down(i)).isAir() ||
                !mc.world.getBlockState(pos.down(i).east()).isAir()) {
                return false;
            }
        }
        
        return isSolidBlock(pos.down().north()) &&
               isSolidBlock(pos.down().south()) &&
               isSolidBlock(pos.down().west()) &&
               isSolidBlock(pos.down().east(2));
    }
    
    private boolean isThreeByOneHole(BlockPos pos) {
        // Check for 3x1 hole (3 blocks wide)
        for (int i = 0; i < 3; i++) {
            if (!mc.world.getBlockState(pos.down(i)).isAir() ||
                !mc.world.getBlockState(pos.down(i).east()).isAir() ||
                !mc.world.getBlockState(pos.down(i).east(2)).isAir()) {
                return false;
            }
        }
        
        return isSolidBlock(pos.down().north()) &&
               isSolidBlock(pos.down().south()) &&
               isSolidBlock(pos.down().west()) &&
               isSolidBlock(pos.down().east(3));
    }
    
    private boolean isSolidBlock(BlockPos pos) {
        if (!mc.world.isChunkLoaded(pos)) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return block != Blocks.AIR && 
               block != Blocks.CAVE_AIR &&
               block != Blocks.VOID_AIR &&
               block != Blocks.WATER && 
               block != Blocks.LAVA;
    }
    
    private void renderHole(MatrixStack matrices, Hole hole) {
        BlockPos pos = hole.pos;
        Color color = getColorForType(hole.type);
        
        // Calculate box boundaries
        double minX = pos.getX();
        double minY = pos.getY();
        double minZ = pos.getZ();
        double maxX = pos.getX() + hole.type.width;
        double maxY = pos.getY() + 1;
        double maxZ = pos.getZ() + 1;
        
        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        
        // Render the box outline
        renderBoxOutline(matrices, box, color);
        
        // Render vertical line going down
        double centerX = minX + (hole.type.width / 2.0);
        double centerZ = minZ + 0.5;
        double bottomY = pos.getY() - 15;
        
        RenderUtils.renderLine(matrices, color,
                new Vec3d(centerX, minY, centerZ),
                new Vec3d(centerX, bottomY, centerZ));
    }
    
    private void renderBoxOutline(MatrixStack matrices, Box box, Color color) {
        // Bottom edges
        renderLine(matrices, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
        renderLine(matrices, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
        renderLine(matrices, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        renderLine(matrices, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, color);
        
        // Top edges
        renderLine(matrices, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        renderLine(matrices, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
        renderLine(matrices, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        renderLine(matrices, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
        
        // Vertical edges
        renderLine(matrices, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
        renderLine(matrices, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        renderLine(matrices, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
        renderLine(matrices, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
    }
    
    private void renderLine(MatrixStack matrices, double x1, double y1, double z1,
                            double x2, double y2, double z2, Color color) {
        RenderUtils.renderLine(matrices, color,
                new Vec3d(x1, y1, z1),
                new Vec3d(x2, y2, z2));
    }
    
    private Color getColorForType(HoleType type) {
        return switch (type) {
            case ONE_BY_ONE -> HOLE_1x1_COLOR;
            case TWO_BY_ONE -> HOLE_2x1_COLOR;
            case THREE_BY_ONE -> HOLE_3x1_COLOR;
        };
    }
    
    private static class Hole {
        BlockPos pos;
        HoleType type;
        
        Hole(BlockPos pos, HoleType type) {
            this.pos = pos;
            this.type = type;
        }
    }
    
    private enum HoleType {
        ONE_BY_ONE(1),
        TWO_BY_ONE(2),
        THREE_BY_ONE(3);
        
        int width;
        
        HoleType(int width) {
            this.width = width;
        }
    }
}
