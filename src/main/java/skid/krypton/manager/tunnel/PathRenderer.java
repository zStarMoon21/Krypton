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
    private List<BlockPos> currentPath;
    private BlockPos hazard;
    private boolean enabled = true;
    
    public PathRenderer(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void setPath(List<BlockPos> path) {
        this.currentPath = path;
    }
    
    public void setHazard(BlockPos hazard) {
        this.hazard = hazard;
    }
    
    public void render(MatrixStack matrixStack, float tickDelta) {
        if (!enabled || currentPath == null || currentPath.isEmpty() || mc.player == null) return;
        
        // Save matrix state
        matrixStack.push();
        
        // Get camera position for proper 3D rendering
        Vec3d camPos = RenderUtils.getCameraPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Render the path lines with less intensity
        renderPathLines(matrixStack);
        
        // Render the current target
        renderTarget(matrixStack);
        
        // Render hazard if present
        renderHazard(matrixStack);
        
        matrixStack.pop();
    }
    
    private void renderPathLines(MatrixStack matrixStack) {
        if (currentPath.size() < 2) return;
        
        for (int i = 0; i < currentPath.size() - 1; i++) {
            BlockPos current = currentPath.get(i);
            BlockPos next = currentPath.get(i + 1);
            
            // Only render if blocks are close enough (avoid long lines)
            if (Math.abs(current.getX() - next.getX()) > 3 || 
                Math.abs(current.getY() - next.getY()) > 3 || 
                Math.abs(current.getZ() - next.getZ()) > 3) {
                continue;
            }
            
            Vec3d currentPos = new Vec3d(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5);
            Vec3d nextPos = new Vec3d(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5);
            
            // Use a softer cyan color
            Color pathColor = new Color(100, 255, 255, 150);
            
            try {
                RenderUtils.renderLine(matrixStack, pathColor, currentPos, nextPos);
            } catch (Exception e) {
                // Ignore rendering errors
            }
        }
    }
    
    private void renderTarget(MatrixStack matrixStack) {
        if (currentPath.isEmpty()) return;
        
        BlockPos target = currentPath.get(0);
        
        // Only render if target is within reasonable distance
        if (mc.player != null) {
            double distance = mc.player.getBlockPos().getManhattanDistance(target);
            if (distance > 20) return;
        }
        
        try {
            // Render a subtle box around the target
            RenderUtils.renderFilledBox(matrixStack,
                target.getX(), target.getY(), target.getZ(),
                target.getX() + 1, target.getY() + 1, target.getZ() + 1,
                new Color(0, 255, 0, 30));
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    private void renderHazard(MatrixStack matrixStack) {
        if (hazard == null) return;
        
        try {
            RenderUtils.renderFilledBox(matrixStack,
                hazard.getX(), hazard.getY(), hazard.getZ(),
                hazard.getX() + 1, hazard.getY() + 1, hazard.getZ() + 1,
                new Color(255, 0, 0, 40));
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
