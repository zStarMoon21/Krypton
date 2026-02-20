package skid.krypton.module.modules.donut;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
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
    private final BooleanSetting debug = new BooleanSetting("Debug", false);
    private final BooleanSetting showScore = new BooleanSetting("Show Score", false);
    private final NumberSetting threshold = new NumberSetting("Threshold", 10, 100, 60, 1);
    private final NumberSetting scanSpeed = new NumberSetting("Scan Speed", 1, 10, 3, 1);

    // Detection components
    private GrowthDetector growthDetector;
    private KelpGapDetector kelpGapDetector;
    private LightDetector lightDetector;
    private PillarDetector pillarDetector;
    private UptimeTracker uptimeTracker;
    private ChunkRenderer chunkRenderer;
    private ScoreRenderer scoreRenderer;

    // Data storage
    private final Map<ChunkPos, ChunkData> chunkDataMap = new ConcurrentHashMap<>();
    private final Set<ChunkPos> suspiciousChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkPos> pendingScans = new LinkedList<>();

    // Performance
    private int scanIndex = 0;
    private int ticksSinceLastCleanup = 0;
    private long lastScanTime = 0;

    public SusChunkFinder() {
        super("SusChunk", "Advanced chunk suspicion detector", -1, Category.DONUT);
        this.addSettings(debug, showScore, threshold, scanSpeed);
    }

    @Override
    public void onEnable() {
        growthDetector = new GrowthDetector();
        kelpGapDetector = new KelpGapDetector();
        lightDetector = new LightDetector();
        pillarDetector = new PillarDetector();
        uptimeTracker = new UptimeTracker();
        chunkRenderer = new ChunkRenderer(mc);
        scoreRenderer = new ScoreRenderer(mc);

        chunkDataMap.clear();
        suspiciousChunks.clear();
        scannedChunks.clear();
        pendingScans.clear();

        super.onEnable();
    }

    @Override
    public void onDisable() {
        chunkDataMap.clear();
        suspiciousChunks.clear();
        scannedChunks.clear();
        pendingScans.clear();
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.world == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        
        // Throttle scanning based on scanSpeed setting
        int scanDelay = 100 / scanSpeed.getIntValue(); // 100ms / speed
        if (now - lastScanTime < scanDelay) return;
        lastScanTime = now;

        // Get player's current chunk
        ChunkPos playerChunk = mc.player.getChunkPos();
        int viewDist = mc.options.getViewDistance().getValue();

        // Scan chunks in render distance
        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                ChunkPos pos = new ChunkPos(playerChunk.x + x, playerChunk.z + z);
                
                // Skip if already scanned
                if (scannedChunks.contains(pos)) continue;

                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(pos.x, pos.z);
                if (chunk != null) {
                    scanChunk(chunk);
                }
            }
        }

        // Clean up distant chunks every 5 seconds
        ticksSinceLastCleanup++;
        if (ticksSinceLastCleanup >= 100) {
            cleanupDistantChunks();
            ticksSinceLastCleanup = 0;
        }
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || suspiciousChunks.isEmpty()) return;

        int thresholdValue = threshold.getIntValue();

        for (ChunkPos pos : suspiciousChunks) {
            ChunkData data = chunkDataMap.get(pos);
            if (data == null) continue;

            // Only render if still suspicious
            if (data.getTotalScore() >= thresholdValue) {
                chunkRenderer.renderChunkHighlight(event.matrixStack, pos);
                
                if (showScore.getValue()) {
                    scoreRenderer.renderScore(event.matrixStack, pos, data.getTotalScore());
                }
            }
        }

        // Debug info
        if (debug.getValue() && mc.player.age % 40 == 0) {
            mc.player.sendMessage(Text.literal("§7[SusChunk] §a" + suspiciousChunks.size() + " chunks highlighted"), true);
        }
    }

    private void scanChunk(WorldChunk chunk) {
        if (chunk == null) return;

        ChunkPos pos = chunk.getPos();
        
        // Mark as scanned
        scannedChunks.add(pos);

        // Get or create data
        ChunkData data = chunkDataMap.computeIfAbsent(pos, k -> new ChunkData(pos, mc));

        int score = 0;

        // Run all detectors
        score += growthDetector.scanChunk(chunk, data);
        
        if (kelpGapDetector.isOceanChunk(chunk)) {
            score += kelpGapDetector.scanForKelpGaps(chunk, data);
        }
        
        score += lightDetector.scanForLightPatterns(chunk, data);
        score += pillarDetector.scanForPillars(chunk, data);
        score += uptimeTracker.calculateUptimeScore(data);

        data.setTotalScore(score);
        data.updateLastScanTime();

        // Update suspicious status
        int thresholdValue = threshold.getIntValue();
        if (score >= thresholdValue) {
            suspiciousChunks.add(pos);
            if (debug.getValue()) {
                System.out.println("Found suspicious chunk at " + pos + " with score " + score);
            }
        } else {
            suspiciousChunks.remove(pos);
        }
    }

    private void cleanupDistantChunks() {
        if (mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();

        // Remove chunks outside render distance
        suspiciousChunks.removeIf(pos -> {
            int dx = Math.abs(pos.x - playerChunk.x);
            int dz = Math.abs(pos.z - playerChunk.z);
            return dx > viewDist + 2 || dz > viewDist + 2;
        });

        scannedChunks.removeIf(pos -> {
            int dx = Math.abs(pos.x - playerChunk.x);
            int dz = Math.abs(pos.z - playerChunk.z);
            return dx > viewDist + 4 || dz > viewDist + 4;
        });

        chunkDataMap.entrySet().removeIf(entry -> {
            ChunkPos pos = entry.getKey();
            int dx = Math.abs(pos.x - playerChunk.x);
            int dz = Math.abs(pos.z - playerChunk.z);
            return dx > viewDist + 4 || dz > viewDist + 4;
        });
    }
}
