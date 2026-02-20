package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import skid.krypton.utils.RenderUtils;

import java.awt.Color;

public class ChunkRenderer {

    private final MinecraftClient mc;

    // Much more visible colors
    private static final Color CHUNK_COLOR = new Color(100, 255, 100, 120); // Much more opaque
    private static final Color BORDER_COLOR = new Color(0, 255, 0, 255); // Bright green borders

    private static final double THICKNESS = 1.0; // Thicker so it's definitely visible
    private static final double RENDER_HEIGHT = 60.0;

    public ChunkRenderer(MinecraftClient mc) {
        this.mc = mc;
    }

    public void renderChunkHighlight(MatrixStack matrices, ChunkPos chunkPos) {
        if (mc.world == null || mc.player == null) return;

        // Calculate chunk boundaries
        double minX = chunkPos.getStartX();
        double minZ = chunkPos.getStartZ();
        double maxX = chunkPos.getEndX() + 1;
        double maxZ = chunkPos.getEndZ() + 1;

        // Create the highlight box at Y=60
        Box box = new Box(minX, RENDER_HEIGHT, minZ, maxX, RENDER_HEIGHT + THICKNESS, maxZ);

        renderChunkBox(matrices, box);
        
        // Also render vertical lines at chunk corners for better visibility
        renderVerticalCorners(matrices, minX, maxX, minZ, maxZ);
        
        // Add a beacon-style beam at the center for extra visibility
        renderCenterBeam(matrices, minX, maxX, minZ, maxZ);
    }

    private void renderChunkBox(MatrixStack matrices, Box box) {
        // Render the transparent fill
        RenderUtils.renderFilledBox(matrices,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                CHUNK_COLOR);

        // Render the top edges
        renderTopEdges(matrices, box);
        
        // Render bottom edges for extra visibility
        renderBottomEdges(matrices, box);
    }

    private void renderTopEdges(MatrixStack matrices, Box box) {
        double y = box.maxY;

        renderLine(matrices, box.minX, y, box.minZ, box.maxX, y, box.minZ);
        renderLine(matrices, box.maxX, y, box.minZ, box.maxX, y, box.maxZ);
        renderLine(matrices, box.maxX, y, box.maxZ, box.minX, y, box.maxZ);
        renderLine(matrices, box.minX, y, box.maxZ, box.minX, y, box.minZ);
    }
    
    private void renderBottomEdges(MatrixStack matrices, Box box) {
        double y = box.minY;

        renderLine(matrices, box.minX, y, box.minZ, box.maxX, y, box.minZ);
        renderLine(matrices, box.maxX, y, box.minZ, box.maxX, y, box.maxZ);
        renderLine(matrices, box.maxX, y, box.maxZ, box.minX, y, box.maxZ);
        renderLine(matrices, box.minX, y, box.maxZ, box.minX, y, box.minZ);
    }

    private void renderVerticalCorners(MatrixStack matrices, double minX, double maxX, double minZ, double maxZ) {
        // Render vertical lines at the four corners from Y=60 down to player height
        double playerY = mc.player.getY();
        double bottomY = Math.min(RENDER_HEIGHT, playerY - 5);
        double topY = Math.max(RENDER_HEIGHT + THICKNESS, playerY + 5);

        // Four corners - long vertical lines
        renderLine(matrices, minX, bottomY, minZ, minX, topY, minZ);
        renderLine(matrices, maxX, bottomY, minZ, maxX, topY, minZ);
        renderLine(matrices, maxX, bottomY, maxZ, maxX, topY, maxZ);
        renderLine(matrices, minX, bottomY, maxZ, minX, topY, maxZ);
    }
    
    private void renderCenterBeam(MatrixStack matrices, double minX, double maxX, double minZ, double maxZ) {
        double centerX = (minX + maxX) / 2;
        double centerZ = (minZ + maxZ) / 2;
        double playerY = mc.player.getY();
        
        // Render a vertical beam at the center of the chunk
        renderLine(matrices, centerX, RENDER_HEIGHT - 10, centerZ, centerX, RENDER_HEIGHT + THICKNESS + 10, centerZ);
    }

    private void renderLine(MatrixStack matrices, double x1, double y1, double z1,
                            double x2, double y2, double z2) {
        RenderUtils.renderLine(matrices, BORDER_COLOR,
                new Vec3d(x1, y1, z1),
                new Vec3d(x2, y2, z2));
    }
}
