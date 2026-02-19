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
    private final Color pathColor = new Color(0, 200, 255, 180);
    
    public PathRenderer(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public void updatePath(List<BlockPos> path) {
        this.currentPath = path;
    }
    
    public void render(MatrixStack matrixStack, float tickDelta) {
        if (currentPath == null || currentPath.size() < 2 || mc.player == null) return;
        
        matrixStack.push();
        
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Draw lines connecting path points
        for (int i = 0; i < currentPath.size() - 1; i++) {
            BlockPos current = currentPath.get(i);
            BlockPos next = currentPath.get(i + 1);
            
            Vec3d currentPos = new Vec3d(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5);
            Vec3d nextPos = new Vec3d(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5);
            
            RenderUtils.renderLine(matrixStack, pathColor, currentPos, nextPos);
        }
        
        matrixStack.pop();
    }
}
