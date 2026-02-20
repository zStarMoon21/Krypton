package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import skid.krypton.utils.RenderUtils;

import java.awt.Color;

public class ChunkRenderer {
    
    // Soft bright green - like shader debug overlays
    private static final Color CHUNK_COLOR = new Color(120, 255, 120, 70);
    private static final Color BORDER_COLOR = new Color(100, 230, 100, 180);
    
    // Render at a fixed height (64 is a good middle ground)
    private static final int RENDER_HEIGHT = 64;
    private static final double THICKNESS = 0.3; // Thin box like Meteor client

    public void renderChunkHighlight(MatrixStack matrixStack, ChunkPos chunkPos) {
        matrixStack.push();
        
        // Get camera position for proper 3D rendering
        Vec3d camPos = RenderUtils.getCameraPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Calculate chunk boundaries
        double minX = chunkPos.getStartX();
        double minZ = chunkPos.getStartZ();
        double maxX = chunkPos.getEndX() + 1;
        double maxZ = chunkPos.getEndZ() + 1;
        double y = RENDER_HEIGHT;
        double h = THICKNESS;
        
        // Create box for the chunk highlight (flat box at Y level)
        Box box = new Box(minX, y, minZ, maxX, y + h, maxZ);
        
        // Render the box with both fill and outline (like Meteor's ShapeMode.Both)
        renderBox(matrixStack, box, CHUNK_COLOR, BORDER_COLOR);
        
        matrixStack.pop();
    }
    
    private void renderBox(MatrixStack matrixStack, Box box, Color fill, Color outline) {
        // Render filled box (semi-transparent)
        RenderUtils.renderFilledBox(matrixStack,
            (float) box.minX, (float) box.minY, (float) box.minZ,
            (float) box.maxX, (float) box.maxY, (float) box.maxZ,
            fill);
        
        // Render outline (borders)
        renderOutlines(matrixStack, box, outline);
    }
    
    private void renderOutlines(MatrixStack matrixStack, Box box, Color color) {
        // Bottom rectangle
        renderLine(matrixStack, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
        renderLine(matrixStack, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
        renderLine(matrixStack, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        renderLine(matrixStack, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, color);
        
        // Top rectangle
        renderLine(matrixStack, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        renderLine(matrixStack, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
        renderLine(matrixStack, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        renderLine(matrixStack, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
        
        // Vertical edges
        renderLine(matrixStack, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
        renderLine(matrixStack, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        renderLine(matrixStack, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
        renderLine(matrixStack, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
    }
    
    private void renderLine(MatrixStack matrixStack, double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        RenderUtils.renderLine(matrixStack, color, new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2));
    }
}
