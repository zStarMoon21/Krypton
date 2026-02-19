package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;
import skid.krypton.utils.RenderUtils;

import java.awt.Color;
import java.util.List;

public class PathRenderer {
    private final MinecraftClient mc;
    private PathScanner pathScanner;
    
    // Brighter, more visible colors
    private static final Color PATH_COLOR = new Color(0, 255, 255, 255); // Bright cyan, full opacity
    private static final Color HAZARD_COLOR = new Color(255, 0, 0, 150);  // Semi-transparent red
    private static final float LINE_WIDTH = 3.0f; // Thicker lines
    
    public PathRenderer(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void setPathScanner(PathScanner scanner) {
        this.pathScanner = scanner;
    }
    
    public void render(MatrixStack matrixStack, float tickDelta) {
        if (pathScanner == null || mc.player == null) return;
        
        List<BlockPos> path = pathScanner.getCurrentPath();
        List<BlockPos> hazards = pathScanner.getHazardList();
        
        matrixStack.push();
        
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Render path with bright lines
        if (path != null && path.size() >= 2) {
            renderPath(matrixStack, path);
        }
        
        // Render hazards with red squares
        if (hazards != null && !hazards.isEmpty()) {
            renderHazards(matrixStack, hazards);
        }
        
        matrixStack.pop();
    }
    
    private void renderPath(MatrixStack matrixStack, List<BlockPos> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            BlockPos current = path.get(i);
            BlockPos next = path.get(i + 1);
            
            // Only connect blocks that are close (1-2 blocks apart)
            if (Math.abs(current.getX() - next.getX()) <= 2 &&
                Math.abs(current.getY() - next.getY()) <= 2 &&
                Math.abs(current.getZ() - next.getZ()) <= 2) {
                
                Vec3d currentPos = new Vec3d(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5);
                Vec3d nextPos = new Vec3d(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5);
                
                // Draw thick, bright line
                RenderUtils.renderLine(matrixStack, PATH_COLOR, currentPos, nextPos);
            }
        }
    }
    
    private void renderHazards(MatrixStack matrixStack, List<BlockPos> hazards) {
        for (BlockPos hazard : hazards) {
            // Draw red box around hazard
            RenderUtils.renderFilledBox(matrixStack,
                hazard.getX(), hazard.getY(), hazard.getZ(),
                hazard.getX() + 1, hazard.getY() + 1, hazard.getZ() + 1,
                HAZARD_COLOR);
        }
    }
}
