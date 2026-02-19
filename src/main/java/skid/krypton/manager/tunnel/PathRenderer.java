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
    private AStarPathfinder pathfinder;
    
    // Bright, visible colors
    private static final Color PATH_COLOR = new Color(0, 255, 255, 255); // Bright cyan
    private static final Color HAZARD_COLOR = new Color(255, 0, 0, 150);  // Semi-transparent red
    private static final Color TARGET_COLOR = new Color(0, 255, 0, 100);  // Semi-transparent green
    
    public PathRenderer(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void setPathfinder(AStarPathfinder pathfinder) {
        this.pathfinder = pathfinder;
    }
    
    public void render(MatrixStack matrixStack, float tickDelta) {
        if (pathfinder == null || mc.player == null) return;
        
        matrixStack.push();
        
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Render path
        List<BlockPos> path = pathfinder.getCurrentPath();
        if (path != null && path.size() >= 2) {
            renderPath(matrixStack, path);
        }
        
        // Render hazards
        List<BlockPos> hazards = pathfinder.getHazardPositions();
        if (hazards != null && !hazards.isEmpty()) {
            renderHazards(matrixStack, hazards);
        }
        
        // Render target
        BlockPos target = pathfinder.getFirstTarget();
        if (target != null) {
            renderTarget(matrixStack, target);
        }
        
        matrixStack.pop();
    }
    
    private void renderPath(MatrixStack matrixStack, List<BlockPos> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            BlockPos current = path.get(i);
            BlockPos next = path.get(i + 1);
            
            Vec3d currentPos = new Vec3d(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5);
            Vec3d nextPos = new Vec3d(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5);
            
            // Draw thick line
            for (int j = 0; j < 3; j++) {
                RenderUtils.renderLine(matrixStack, PATH_COLOR, currentPos, nextPos);
            }
        }
    }
    
    private void renderHazards(MatrixStack matrixStack, List<BlockPos> hazards) {
        for (BlockPos hazard : hazards) {
            // Draw red box
            RenderUtils.renderFilledBox(matrixStack,
                hazard.getX() + 0.1f, hazard.getY() + 0.1f, hazard.getZ() + 0.1f,
                hazard.getX() + 0.9f, hazard.getY() + 0.9f, hazard.getZ() + 0.9f,
                HAZARD_COLOR);
        }
    }
    
    private void renderTarget(MatrixStack matrixStack, BlockPos target) {
        // Draw green outline
        RenderUtils.renderFilledBox(matrixStack,
            target.getX() + 0.2f, target.getY() + 0.2f, target.getZ() + 0.2f,
            target.getX() + 0.8f, target.getY() + 0.8f, target.getZ() + 0.8f,
            TARGET_COLOR);
    }
}
