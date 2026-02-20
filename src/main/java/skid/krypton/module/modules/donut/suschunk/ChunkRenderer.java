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

    // Bright and visible colors
    private static final Color CHUNK_COLOR = new Color(0, 255, 0, 100);
    private static final Color BORDER_COLOR = new Color(255, 255, 255, 255);

    private static final double THICKNESS = 1.0;
    private static final double RENDER_HEIGHT = 60.0;

    public ChunkRenderer(MinecraftClient mc) {
        this.mc = mc;
    }

    public void renderChunkHighlight(MatrixStack matrices, ChunkPos chunkPos) {
        if (mc.world == null || mc.player == null) return;

        // Save the current matrix state
        matrices.push();
        
        // Get camera position for proper 3D rendering
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Calculate chunk boundaries
        double minX = chunkPos.getStartX();
        double minZ = chunkPos.getStartZ();
        double maxX = chunkPos.getEndX() + 1;
        double maxZ = chunkPos.getEndZ() + 1;

        // Create the highlight box at Y=60
        Box box = new Box(minX, RENDER_HEIGHT, minZ, maxX, RENDER_HEIGHT + THICKNESS, maxZ);

        // Render the box
        renderChunkBox(matrices, box);
        
        // Render vertical lines at chunk corners
        renderVerticalCorners(matrices, minX, maxX, minZ, maxZ);
        
        // Restore the matrix state
        matrices.pop();
    }

    private void renderChunkBox(MatrixStack matrices, Box box) {
        // Render the transparent fill
        RenderUtils.renderFilledBox(matrices,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                CHUNK_COLOR);

        // Render all edges of the box
        renderBoxEdges(matrices, box);
    }

    private void renderBoxEdges(MatrixStack matrices, Box box) {
        // Bottom edges
        renderLine(matrices, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ);
        renderLine(matrices, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ);
        renderLine(matrices, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ);
        renderLine(matrices, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ);
        
        // Top edges
        renderLine(matrices, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ);
        renderLine(matrices, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ);
        renderLine(matrices, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ);
        renderLine(matrices, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ);
        
        // Vertical edges
        renderLine(matrices, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ);
        renderLine(matrices, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ);
        renderLine(matrices, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ);
        renderLine(matrices, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ);
    }

    private void renderVerticalCorners(MatrixStack matrices, double minX, double maxX, double minZ, double maxZ) {
        double playerY = mc.player.getY();
        double topY = Math.max(RENDER_HEIGHT, playerY);
        double bottomY = Math.min(RENDER_HEIGHT, playerY);

        // Four corners - vertical lines connecting to player height
        renderLine(matrices, minX, bottomY, minZ, minX, topY, minZ);
        renderLine(matrices, maxX, bottomY, minZ, maxX, topY, minZ);
        renderLine(matrices, maxX, bottomY, maxZ, maxX, topY, maxZ);
        renderLine(matrices, minX, bottomY, maxZ, minX, topY, maxZ);
    }

    private void renderLine(MatrixStack matrices, double x1, double y1, double z1,
                            double x2, double y2, double z2) {
        RenderUtils.renderLine(matrices, BORDER_COLOR,
                new Vec3d(x1, y1, z1),
                new Vec3d(x2, y2, z2));
    }
}
