package skid.krypton.module.modules.donut.suschunk;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

public class ChunkData {
    private final ChunkPos pos;
    private int totalScore = 0;
    private long firstSeenTime = 0;
    private long lastSeenTime = 0;
    private int timesSeen = 0;

    // Growth tracking sets
    private final Set<Long> tallBamboo = new HashSet<>();
    private final Set<Long> tallKelp = new HashSet<>();
    private final Set<Long> sugarCaneFarms = new HashSet<>();
    private final Set<Long> amethystClusters = new HashSet<>();
    private final Set<Long> largeDripstone = new HashSet<>();
    private final Set<Long> longVines = new HashSet<>();

    // Light tracking
    private final Set<Long> lightSources = new HashSet<>();

    // Pillar tracking
    private final Set<Long> pillars = new HashSet<>();

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

    // ===== GROWTH TRACKING METHODS =====
    public void addTallBamboo(long pos) { tallBamboo.add(pos); }
    public void addTallKelp(long pos) { tallKelp.add(pos); }
    public void addSugarCaneFarm(long pos) { sugarCaneFarms.add(pos); }
    public void addAmethystCluster(long pos) { amethystClusters.add(pos); }
    public void addLargeDripstone(long pos) { largeDripstone.add(pos); }
    public void addLongVine(long pos) { longVines.add(pos); }

    public int getTallBambooCount() { return tallBamboo.size(); }
    public int getTallKelpCount() { return tallKelp.size(); }
    public int getSugarCaneFarmCount() { return sugarCaneFarms.size(); }
    public int getAmethystClusterCount() { return amethystClusters.size(); }
    public int getLargeDripstoneCount() { return largeDripstone.size(); }
    public int getLongVineCount() { return longVines.size(); }

    // ===== LIGHT TRACKING METHODS =====
    public void addLightSource(long pos) { lightSources.add(pos); }
    public int getLightSourceCount() { return lightSources.size(); }

    // ===== PILLAR TRACKING METHODS =====
    public void addPillar(long pos) { pillars.add(pos); }
    public int getPillarCount() { return pillars.size(); }

    // ===== GETTERS & SETTERS =====
    public ChunkPos getPos() { return pos; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int score) { this.totalScore = score; }
    public int getTimesSeen() { return timesSeen; }
}
