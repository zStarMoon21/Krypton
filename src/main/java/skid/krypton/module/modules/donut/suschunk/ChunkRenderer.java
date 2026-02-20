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

    private static final Color CHUNK_COLOR = new Color(100, 255, 100, 40);
    private static final Color BORDER_COLOR = new Color(80, 220, 80, 200);

    private static final double RENDER_HEIGHT = 60.0;
    private static final double THICKNESS = 0.01;

    public ChunkRenderer(MinecraftClient mc) {
        this.mc = mc;
    }

    public void renderChunkHighlight(MatrixStack matrices, ChunkPos chunkPos) {
        if (mc.world == null) return;

        // Chunk boundaries
        double minX = chunkPos.getStartX();
        double minZ = chunkPos.getStartZ();
        double maxX = chunkPos.getEndX() + 1;
        double maxZ = chunkPos.getEndZ() + 1;

        // Fixed Y plane
        Box box = new Box(minX, RENDER_HEIGHT, minZ, maxX, RENDER_HEIGHT + THICKNESS, maxZ);

        renderChunkBox(matrices, box);
    }

    private void renderChunkBox(MatrixStack matrices, Box box) {
        RenderUtils.renderFilledBox(matrices,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                CHUNK_COLOR);

        renderTopEdges(matrices, box);
    }

    private void renderTopEdges(MatrixStack matrices, Box box) {
        double y = box.maxY;

        renderLine(matrices, box.minX, y, box.minZ, box.maxX, y, box.minZ);
        renderLine(matrices, box.maxX, y, box.minZ, box.maxX, y, box.maxZ);
        renderLine(matrices, box.maxX, y, box.maxZ, box.minX, y, box.maxZ);
        renderLine(matrices, box.minX, y, box.maxZ, box.minX, y, box.minZ);
    }

    private void renderLine(MatrixStack matrices, double x1, double y1, double z1,
                            double x2, double y2, double z2) {
        RenderUtils.renderLine(matrices, BORDER_COLOR,
                new Vec3d(x1, y1, z1),
                new Vec3d(x2, y2, z2));
    }
}
