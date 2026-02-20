package skid.krypton.module.modules.donut;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render3DEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.modules.donut.suschunk.*;
import skid.krypton.utils.EncryptedString;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SusChunkFinder extends Module {

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
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();

    // Fixed threshold
    private static final int SUSPICION_THRESHOLD = 26;

    // Performance
    private int scanIndex = 0;
    private int cleanupCooldown = 0;
    private List<WorldChunk> loadedChunks = new ArrayList<>();

    public SusChunkFinder() {
        super("SusChunk", "Highlights suspicious chunks in soft green", -1, Category.DONUT);
    }

    @Override
    public void onEnable() {
        growthDetector = new GrowthDetector();
        kelpGapDetector = new KelpGapDetector();
        lightDetector = new LightDetector();
        pillarDetector = new PillarDetector();
        uptimeTracker = new UptimeTracker();
        chunkRenderer = new ChunkRenderer(mc);

        chunkDataMap.clear();
        suspiciousChunks.clear();
        scannedChunks.clear();

        super.onEnable();
    }

    @Override
    public void onDisable() {
        chunkDataMap.clear();
        suspiciousChunks.clear();
        scannedChunks.clear();
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.world == null || mc.player == null) return;

        // Update loaded chunks every 20 ticks
        if (scanIndex == 0) {
            updateLoadedChunks();
        }

        // Scan one chunk per tick (spreads workload)
        if (!loadedChunks.isEmpty() && scanIndex < loadedChunks.size()) {
            WorldChunk chunk = loadedChunks.get(scanIndex);
            scanChunk(chunk);
            scanIndex++;
        } else {
            scanIndex = 0;
        }

        // Update uptime for all loaded chunks
        for (WorldChunk chunk : loadedChunks) {
            ChunkPos pos = chunk.getPos();
            ChunkData data = chunkDataMap.computeIfAbsent(pos, k -> new ChunkData(pos));
            uptimeTracker.updateUptime(data);
        }

        // Clean up distant chunks every 10 seconds
        if (--cleanupCooldown <= 0) {
            cleanupDistantChunks();
            cleanupCooldown = 200;
        }
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || suspiciousChunks.isEmpty()) return;

        for (ChunkPos pos : suspiciousChunks) {
            chunkRenderer.renderChunkHighlight(event.matrixStack, pos);
        }
    }

    private void updateLoadedChunks() {
        loadedChunks.clear();
        if (mc.world == null || mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();

        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(playerChunk.x + x, playerChunk.z + z);
                if (chunk != null) {
                    loadedChunks.add(chunk);
                }
            }
        }
    }

    private void scanChunk(WorldChunk chunk) {
        if (chunk == null) return;

        ChunkPos pos = chunk.getPos();
        
        // Only scan chunks once
        if (!scannedChunks.add(pos)) return;

        ChunkData data = chunkDataMap.computeIfAbsent(pos, k -> new ChunkData(pos));

        int score = 0;

        // 1. Growth detection
        score += growthDetector.scanChunk(chunk, data);

        // 2. Kelp gap detection
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

        // Mark as suspicious if threshold reached
        if (score >= SUSPICION_THRESHOLD && data.getLoadTime() > 600) { // 30 seconds minimum
            suspiciousChunks.add(pos);
        }
    }

    private void cleanupDistantChunks() {
        if (mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();

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
