package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import skid.krypton.utils.RenderUtils;

import java.awt.Color;

public class ChunkRenderer {
    
    // Clean soft green - not too bright, not too dark
    private static final Color CHUNK_COLOR = new Color(100, 255, 100, 45);  // Very transparent fill
    private static final Color BORDER_COLOR = new Color(80, 220, 80, 160);  // Slightly visible border
    
    // Fixed render height at Y=60 (clean, above most terrain)
    private static final double RENDER_HEIGHT = 60.0;
    private static final double THICKNESS = 0.2; // Thin, clean highlight

    public void renderChunkHighlight(MatrixStack matrixStack, ChunkPos chunkPos) {
        matrixStack.push();
        
        // Get camera position for proper 3D rendering
        Vec3d camPos = getCameraPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Calculate chunk boundaries
        double minX = chunkPos.getStartX();
        double minZ = chunkPos.getStartZ();
        double maxX = chunkPos.getEndX() + 1;
        double maxZ = chunkPos.getEndZ() + 1;
        
        // Create a thin box at Y=60
        Box box = new Box(minX, RENDER_HEIGHT, minZ, maxX, RENDER_HEIGHT + THICKNESS, maxZ);
        
        // Render the box
        renderChunkBox(matrixStack, box);
        
        matrixStack.pop();
    }
    
    private void renderChunkBox(MatrixStack matrixStack, Box box) {
        // Render the transparent fill
        RenderUtils.renderFilledBox(matrixStack,
            (float) box.minX, (float) box.minY, (float) box.minZ,
            (float) box.maxX, (float) box.maxY, (float) box.maxZ,
            CHUNK_COLOR);
        
        // Render the border lines (just the top edges for cleaner look)
        renderTopEdges(matrixStack, box, BORDER_COLOR);
    }
    
    private void renderTopEdges(MatrixStack matrixStack, Box box, Color color) {
        double y = box.maxY; // Render at the top of the box
        
        // Four edges of the top rectangle
        renderLine(matrixStack, box.minX, y, box.minZ, box.maxX, y, box.minZ, color);
        renderLine(matrixStack, box.maxX, y, box.minZ, box.maxX, y, box.maxZ, color);
        renderLine(matrixStack, box.maxX, y, box.maxZ, box.minX, y, box.maxZ, color);
        renderLine(matrixStack, box.minX, y, box.maxZ, box.minX, y, box.minZ, color);
    }
    
    private void renderLine(MatrixStack matrixStack, double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        RenderUtils.renderLine(matrixStack, color, new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2));
    }
    
    private Vec3d getCameraPos() {
        if (mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) {
            return Vec3d.ZERO;
        }
        return mc.gameRenderer.getCamera().getPos();
    }
}
