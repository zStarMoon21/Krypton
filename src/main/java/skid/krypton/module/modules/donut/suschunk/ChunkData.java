package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;

public class ChunkData {
    private final ChunkPos pos;
    private final long firstSeenTime;
    private long lastSeenTime;
    private long lastScanTime;
    private int totalScore;
    private int timesSeen;
    
    // Statistics
    private int growthCount;
    private int kelpGapCount;
    private int lightCount;
    private int pillarCount;

    public ChunkData(ChunkPos pos, MinecraftClient mc) {
        this.pos = pos;
        this.firstSeenTime = System.currentTimeMillis();
        this.lastSeenTime = firstSeenTime;
        this.lastScanTime = 0;
        this.totalScore = 0;
        this.timesSeen = 1;
    }

    public void updateLastScanTime() {
        this.lastScanTime = System.currentTimeMillis();
        this.lastSeenTime = lastScanTime;
        this.timesSeen++;
    }

    public long getLoadTime() {
        return lastSeenTime - firstSeenTime;
    }

    public long getTimeSinceLastScan() {
        return System.currentTimeMillis() - lastScanTime;
    }

    // Getters and setters
    public ChunkPos getPos() { return pos; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int score) { this.totalScore = score; }
    public int getTimesSeen() { return timesSeen; }
    
    public int getGrowthCount() { return growthCount; }
    public void setGrowthCount(int count) { this.growthCount = count; }
    
    public int getKelpGapCount() { return kelpGapCount; }
    public void setKelpGapCount(int count) { this.kelpGapCount = count; }
    
    public int getLightCount() { return lightCount; }
    public void setLightCount(int count) { this.lightCount = count; }
    
    public int getPillarCount() { return pillarCount; }
    public void setPillarCount(int count) { this.pillarCount = count; }
}
