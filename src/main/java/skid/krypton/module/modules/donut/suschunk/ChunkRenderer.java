package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import skid.krypton.utils.RenderUtils;

import java.awt.Color;

public class ChunkRenderer {

    private final MinecraftClient mc;

    private static final Color CHUNK_COLOR = new Color(100, 255, 100, 60); // Slightly more opaque
    private static final Color BORDER_COLOR = new Color(80, 220, 80, 255); // Full opacity borders

    private static final double THICKNESS = 0.5; // Thicker so it's more visible
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
    }

    private void renderChunkBox(MatrixStack matrices, Box box) {
        // Render the transparent fill
        RenderUtils.renderFilledBox(matrices,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                CHUNK_COLOR);

        // Render the top edges
        renderTopEdges(matrices, box);
    }

    private void renderTopEdges(MatrixStack matrices, Box box) {
        double y = box.maxY;

        renderLine(matrices, box.minX, y, box.minZ, box.maxX, y, box.minZ);
        renderLine(matrices, box.maxX, y, box.minZ, box.maxX, y, box.maxZ);
        renderLine(matrices, box.maxX, y, box.maxZ, box.minX, y, box.maxZ);
        renderLine(matrices, box.minX, y, box.maxZ, box.minX, y, box.minZ);
    }

    private void renderVerticalCorners(MatrixStack matrices, double minX, double maxX, double minZ, double maxZ) {
        // Render vertical lines at the four corners from Y=60 down to player height
        double playerY = mc.player.getY();
        double startY = Math.max(RENDER_HEIGHT, playerY - 10);
        double endY = Math.min(RENDER_HEIGHT + THICKNESS, playerY + 10);

        // Only render if the vertical line would be visible
        if (startY < endY) {
            // Four corners
            renderLine(matrices, minX, startY, minZ, minX, endY, minZ);
            renderLine(matrices, maxX, startY, minZ, maxX, endY, minZ);
            renderLine(matrices, maxX, startY, maxZ, maxX, endY, maxZ);
            renderLine(matrices, minX, startY, maxZ, minX, endY, maxZ);
        }
    }

    private void renderLine(MatrixStack matrices, double x1, double y1, double z1,
                            double x2, double y2, double z2) {
        RenderUtils.renderLine(matrices, BORDER_COLOR,
                new Vec3d(x1, y1, z1),
                new Vec3d(x2, y2, z2));
    }
}
