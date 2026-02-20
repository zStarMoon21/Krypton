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

    // Soft bright green - like shader debug overlays
    private static final Color FILL_COLOR = new Color(120, 255, 120, 70);
    private static final Color BORDER_COLOR = new Color(100, 230, 100, 180);

    private static final double RENDER_HEIGHT = 60.0;
    private static final double THICKNESS = 0.1;

    public ChunkRenderer(MinecraftClient mc) {
        this.mc = mc;
    }

    public void renderChunkHighlight(MatrixStack matrices, ChunkPos chunkPos) {
        if (mc.world == null || mc.player == null) return;

        matrices.push();

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        double minX = chunkPos.getStartX();
        double minZ = chunkPos.getStartZ();
        double maxX = chunkPos.getEndX() + 1;
        double maxZ = chunkPos.getEndZ() + 1;

        Box box = new Box(minX, RENDER_HEIGHT, minZ, maxX, RENDER_HEIGHT + THICKNESS, maxZ);

        // Render fill
        RenderUtils.renderFilledBox(matrices,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                FILL_COLOR);

        // Render borders
        renderBoxBorders(matrices, box);

        matrices.pop();
    }

    private void renderBoxBorders(MatrixStack matrices, Box box) {
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

    private void renderLine(MatrixStack matrices, double x1, double y1, double z1,
                            double x2, double y2, double z2) {
        RenderUtils.renderLine(matrices, BORDER_COLOR,
                new Vec3d(x1, y1, z1),
                new Vec3d(x2, y2, z2));
    }
}
