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
    private static final Color PATH_COLOR = new Color(0, 200, 255, 150);
    private static final Color TARGET_COLOR = new Color(0, 255, 0, 80);
    private static final Color HAZARD_COLOR = new Color(255, 0, 0, 80);
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
        if (currentPath.isEmpty()) return;
        
        // Save matrix state
        matrixStack.push();
        
        // Get camera position for proper 3D rendering
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Render path lines (only between consecutive blocks)
        renderPathSegments(matrixStack, currentPath);
        
        // Render current target
        renderTarget(matrixStack, pathFinder.getCurrentTarget());
        
        // Render hazard
        renderHazard(matrixStack);
        
        matrixStack.pop();
    }
    
    private void renderPathSegments(MatrixStack matrixStack, List<BlockPos> path) {
        if (path.size() < 2) return;
        
        for (int i = 0; i < path.size() - 1; i++) {
            BlockPos current = path.get(i);
            BlockPos next = path.get(i + 1);
            
            // Check if blocks are adjacent (should be for a proper path)
            if (isAdjacent(current, next)) {
                Vec3d currentPos = new Vec3d(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5);
                Vec3d nextPos = new Vec3d(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5);
                
                // Only render if within distance
                if (mc.player.getBlockPos().getManhattanDistance(current) <= MAX_RENDER_DISTANCE) {
                    RenderUtils.renderLine(matrixStack, PATH_COLOR, currentPos, nextPos);
                }
            }
        }
    }
    
    private boolean isAdjacent(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= 1 &&
               Math.abs(a.getY() - b.getY()) <= 1 &&
               Math.abs(a.getZ() - b.getZ()) <= 1;
    }
    
    private void renderTarget(MatrixStack matrixStack, BlockPos target) {
        if (target == null) return;
        
        if (mc.player.getBlockPos().getManhattanDistance(target) <= MAX_RENDER_DISTANCE) {
            // Draw target outline
            RenderUtils.renderFilledBox(matrixStack,
                target.getX() + 0.1f, target.getY() + 0.1f, target.getZ() + 0.1f,
                target.getX() + 0.9f, target.getY() + 0.9f, target.getZ() + 0.9f,
                TARGET_COLOR);
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
