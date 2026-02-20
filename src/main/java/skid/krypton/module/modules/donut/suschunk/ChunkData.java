package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.util.math.ChunkPos;

public class ChunkData {
    private final ChunkPos pos;
    private int totalScore = 0;
    private long firstSeenTime = 0;
    private long lastSeenTime = 0;
    private int timesSeen = 0;

    public ChunkData(ChunkPos pos) {
        this.pos = pos;
        this.firstSeenTime = System.currentTimeMillis();
        this.lastSeenTime = firstSeenTime;
    }

    public void updateLoadTime() {
        this.lastSeenTime = System.currentTimeMillis();
    }

    public void incrementTimesSeen() {
        this.timesSeen++;
    }

    public long getLoadTime() {
        return lastSeenTime - firstSeenTime;
    }

    public ChunkPos getPos() { return pos; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int score) { this.totalScore = score; }
    public int getTimesSeen() { return timesSeen; }
}
