package skid.krypton.module.modules.donut;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render3DEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.modules.donut.suschunk.*;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SusChunkFinder extends Module {

    // Settings
    private final BooleanSetting renderChunks = new BooleanSetting("Render", true);
    private final BooleanSetting showScore = new BooleanSetting("Show Score", false);
    private final NumberSetting threshold = new NumberSetting("Threshold", 10, 50, 26, 1);

    // Detection components
    private GrowthDetector growthDetector;
    private KelpGapDetector kelpGapDetector;
    private LightDetector lightDetector;
    private PillarDetector pillarDetector;
    private UptimeTracker uptimeTracker;
    private ChunkRenderer chunkRenderer;

    // Chunk data storage
    private final Map<ChunkPos, ChunkData> chunkDataMap = new ConcurrentHashMap<>();
    private final Set<ChunkPos> suspiciousChunks = ConcurrentHashMap.newKeySet();

    // Scan timing
    private int scanCooldown = 0;
    private static final int SCAN_INTERVAL = 40; // Scan every 2 seconds

    public SusChunkFinder() {
        super("SusChunk", "Highlights suspicious chunks in soft green", -1, Category.DONUT);
        this.addSettings(renderChunks, showScore, threshold);
    }

    @Override
    public void onEnable() {
        growthDetector = new GrowthDetector();
        kelpGapDetector = new KelpGapDetector();
        lightDetector = new LightDetector();
        pillarDetector = new PillarDetector();
        uptimeTracker = new UptimeTracker();
        chunkRenderer = new ChunkRenderer();

        chunkDataMap.clear();
        suspiciousChunks.clear();

        super.onEnable();
    }

    @Override
    public void onDisable() {
        chunkDataMap.clear();
        suspiciousChunks.clear();
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.world == null || mc.player == null) return;

        // Scan chunks periodically
        if (--scanCooldown <= 0) {
            scanLoadedChunks();
            scanCooldown = SCAN_INTERVAL;
        }

        // Update uptime for all loaded chunks
        for (WorldChunk chunk : getLoadedChunks()) {
            ChunkPos pos = chunk.getPos();
            ChunkData data = chunkDataMap.computeIfAbsent(pos, k -> new ChunkData(pos));
            uptimeTracker.updateUptime(data);
        }
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (!renderChunks.getValue() || mc.player == null || suspiciousChunks.isEmpty()) return;

        for (ChunkPos pos : suspiciousChunks) {
            ChunkData data = chunkDataMap.get(pos);
            if (data != null) {
                chunkRenderer.renderChunkHighlight(
                    event.matrixStack, 
                    pos, 
                    showScore.getValue() ? data.getTotalScore() : -1
                );
            }
        }
    }

    private void scanLoadedChunks() {
        int thresholdValue = threshold.getIntValue();

        for (WorldChunk chunk : getLoadedChunks()) {
            ChunkPos pos = chunk.getPos();
            ChunkData data = chunkDataMap.computeIfAbsent(pos, k -> new ChunkData(pos));

            // Reset score for new scan
            int score = 0;

            // 1. Growth detection
            score += growthDetector.scanChunk(chunk, data);

            // 2. Kelp gap detection (ocean bases)
            if (kelpGapDetector.isOceanChunk(chunk)) {
                score += kelpGapDetector.scanForKelpGaps(chunk, data);
            }

            // 3. Underground light detection
            score += lightDetector.scanForLightPatterns(chunk, data);

            // 4. Pillar detection
            score += pillarDetector.scanForPillars(chunk, data);

            // 5. Uptime score
            score += uptimeTracker.calculateUptimeScore(data);

            data.setTotalScore(score);
            data.incrementTimesSeen();

            // Mark as suspicious if threshold reached AND loaded for at least 30 seconds
            if (score >= thresholdValue && data.getLoadTime() > 600) { // 30 seconds = 600 ticks
                suspiciousChunks.add(pos);
            } 
            // Confidence check: if seen multiple times, keep it marked even if score drops slightly
            else if (suspiciousChunks.contains(pos) && data.getTimesSeen() > 2) {
                // Keep it marked - confidence is high
            } else {
                suspiciousChunks.remove(pos);
            }
        }
    }

    private List<WorldChunk> getLoadedChunks() {
        List<WorldChunk> chunks = new ArrayList<>();
        if (mc.world == null) return chunks;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();

        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(playerChunk.x + x, playerChunk.z + z);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }
}
