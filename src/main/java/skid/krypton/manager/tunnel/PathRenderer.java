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
    private PathFinder pathFinder;
    private BlockPos hazard;
    private boolean enabled = true;
    
    // Render settings
    private static final Color PATH_COLOR = new Color(0, 200, 255, 180);
    private static final Color HAZARD_COLOR = new Color(255, 0, 0, 100);
    private static final int MAX_RENDER_DISTANCE = 20;
    
    public PathRenderer(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void setPathFinder(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }
    
    public void setHazard(BlockPos hazard) {
        this.hazard = hazard;
    }
    
    public void render(MatrixStack matrixStack, float tickDelta) {
        if (!enabled || mc.player == null || pathFinder == null) return;
        
        List<BlockPos> currentPath = pathFinder.getCurrentPath();
        
        // Save matrix state
        matrixStack.push();
        
        // Get camera position for proper 3D rendering
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Render path lines
        if (!currentPath.isEmpty()) {
            renderPathSegments(matrixStack, currentPath);
        }
        
        // Render hazard if present
        if (hazard != null) {
            renderHazard(matrixStack);
        }
        
        matrixStack.pop();
    }
    
    private void renderPathSegments(MatrixStack matrixStack, List<BlockPos> path) {
        if (path.size() < 2) return;
        
        for (int i = 0; i < path.size() - 1; i++) {
            BlockPos current = path.get(i);
            BlockPos next = path.get(i + 1);
            
            // Only render if within distance
            if (mc.player.getBlockPos().getManhattanDistance(current) <= MAX_RENDER_DISTANCE) {
                Vec3d currentPos = new Vec3d(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5);
                Vec3d nextPos = new Vec3d(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5);
                
                RenderUtils.renderLine(matrixStack, PATH_COLOR, currentPos, nextPos);
            }
        }
    }
    
    private void renderHazard(MatrixStack matrixStack) {
        if (hazard == null) return;
        
        if (mc.player.getBlockPos().getManhattanDistance(hazard) <= MAX_RENDER_DISTANCE) {
            RenderUtils.renderFilledBox(matrixStack,
                hazard.getX() + 0.1f, hazard.getY() + 0.1f, hazard.getZ() + 0.1f,
                hazard.getX() + 0.9f, hazard.getY() + 0.9f, hazard.getZ() + 0.9f,
                HAZARD_COLOR);
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
