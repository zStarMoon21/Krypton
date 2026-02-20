package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import skid.krypton.utils.RenderUtils;

import java.awt.Color;

public class ChunkRenderer {

    // Soft bright green - like shader debug overlays
    private static final Color CHUNK_COLOR = new Color(120, 255, 120, 70); // Soft green with transparency
    private static final Color BORDER_COLOR = new Color(100, 230, 100, 180); // Slightly darker for border
    private static final Color TEXT_COLOR = Color.WHITE;

    // Render at Y=60 (above most terrain)
    private static final int RENDER_HEIGHT = 60;

    public void renderChunkHighlight(MatrixStack matrixStack, ChunkPos chunkPos, int score) {
        // Calculate chunk boundaries
        double minX = chunkPos.getStartX();
        double minZ = chunkPos.getStartZ();
        double maxX = chunkPos.getEndX() + 1;
        double maxZ = chunkPos.getEndZ() + 1;

        matrixStack.push();

        // Translate to camera position for proper 3D rendering
        Vec3d camPos = RenderUtils.getCameraPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // Render the chunk highlight at Y=60
        renderChunkLayer(matrixStack, minX, RENDER_HEIGHT, minZ, maxX, RENDER_HEIGHT + 0.5, maxZ);

        // Render chunk borders (edges)
        renderChunkBorders(matrixStack, minX, RENDER_HEIGHT, minZ, maxX, RENDER_HEIGHT + 0.5, maxZ);

        // Render score if requested
        if (score >= 0) {
            renderChunkScore(matrixStack, chunkPos, score);
        }

        matrixStack.pop();
    }

    private void renderChunkLayer(MatrixStack matrixStack, double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
        // Render the top layer of the chunk
        RenderUtils.renderFilledBox(matrixStack,
            (float) minX, (float) minY, (float) minZ,
            (float) maxX, (float) maxY, (float) maxZ,
            CHUNK_COLOR);
    }

    private void renderChunkBorders(MatrixStack matrixStack, double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ) {
        // North border
        RenderUtils.renderLine(matrixStack, BORDER_COLOR,
            new Vec3d(minX, minY, minZ),
            new Vec3d(maxX, minY, minZ));

        // South border
        RenderUtils.renderLine(matrixStack, BORDER_COLOR,
            new Vec3d(minX, minY, maxZ),
            new Vec3d(maxX, minY, maxZ));

        // West border
        RenderUtils.renderLine(matrixStack, BORDER_COLOR,
            new Vec3d(minX, minY, minZ),
            new Vec3d(minX, minY, maxZ));

        // East border
        RenderUtils.renderLine(matrixStack, BORDER_COLOR,
            new Vec3d(maxX, minY, minZ),
            new Vec3d(maxX, minY, maxZ));

        // Also render at higher Y for visibility
        RenderUtils.renderLine(matrixStack, BORDER_COLOR,
            new Vec3d(minX, maxY, minZ),
            new Vec3d(maxX, maxY, minZ));

        RenderUtils.renderLine(matrixStack, BORDER_COLOR,
            new Vec3d(minX, maxY, maxZ),
            new Vec3d(maxX, maxY, maxZ));

        RenderUtils.renderLine(matrixStack, BORDER_COLOR,
            new Vec3d(minX, maxY, minZ),
            new Vec3d(minX, maxY, maxZ));

        RenderUtils.renderLine(matrixStack, BORDER_COLOR,
            new Vec3d(maxX, maxY, minZ),
            new Vec3d(maxX, maxY, maxZ));
    }

    private void renderChunkScore(MatrixStack matrixStack, ChunkPos chunkPos, int score) {
        // Calculate center of chunk
        double centerX = chunkPos.getStartX() + 8;
        double centerZ = chunkPos.getStartZ() + 8;
        Vec3d centerPos = new Vec3d(centerX, RENDER_HEIGHT + 1, centerZ);

        // Here you would render text using your client's text renderer
        // This is a placeholder - implement with your actual text rendering system
        // renderText(matrixStack, "Score: " + score, centerPos, TEXT_COLOR);
    }
}
