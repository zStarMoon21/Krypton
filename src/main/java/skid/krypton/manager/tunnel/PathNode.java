package skid.krypton.manager.tunnel;

import net.minecraft.util.math.BlockPos;

public class PathNode implements Comparable<PathNode> {
    public BlockPos pos;
    public PathNode parent;
    public double gCost;
    public double hCost;
    public double fCost;
    
    public PathNode(BlockPos pos) {
        this.pos = pos;
    }
    
    public void calculateCosts(BlockPos start, BlockPos target) {
        this.gCost = start == null ? 0 : Math.sqrt(start.getSquaredDistance(pos));
        this.hCost = target == null ? 0 : Math.sqrt(target.getSquaredDistance(pos));
        this.fCost = gCost + hCost;
    }
    
    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.fCost, other.fCost);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PathNode node = (PathNode) obj;
        return pos.equals(node.pos);
    }
    
    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}
