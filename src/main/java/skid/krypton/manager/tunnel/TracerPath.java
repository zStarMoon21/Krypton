package skid.krypton.manager.tunnel;

import net.minecraft.util.math.BlockPos;
import java.util.List;

public class TracerPath {
    private final List<BlockPos> points;
    private final BlockPos start;
    private final BlockPos end;
    
    public TracerPath(List<BlockPos> points) {
        this.points = points;
        this.start = points.isEmpty() ? null : points.get(0);
        this.end = points.isEmpty() ? null : points.get(points.size() - 1);
    }
    
    public List<BlockPos> getPoints() {
        return points;
    }
    
    public BlockPos getStart() {
        return start;
    }
    
    public BlockPos getEnd() {
        return end;
    }
    
    public boolean isEmpty() {
        return points.isEmpty();
    }
}
