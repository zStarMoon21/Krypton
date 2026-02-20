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
    private final Queue<ChunkPos> pendingScans = new ConcurrentLinkedQueue<>();

    // Threshold
    private static final int SUSPICION_THRESHOLD = 60; // Increased to 60

    // Performance
    private int cleanupCooldown = 0;

    public SusChunkFinder() {
        super("SusChunk", "Highlights highly suspicious chunks in soft green", -1, Category.DONUT);
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

        // Queue new chunks for scanning (fast operation)
        queueNewChunks();

        // Process one chunk per tick (spreads workload)
        if (!pendingScans.isEmpty()) {
            ChunkPos pos = pendingScans.poll();
            WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(pos.x, pos.z);
            if (chunk != null) {
                scanChunk(chunk);
            }
        }

        // Clean up distant chunks every 5 seconds
        if (--cleanupCooldown <= 0) {
            cleanupDistantChunks();
            cleanupCooldown = 100; // 5 seconds
        }
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || suspiciousChunks.isEmpty()) return;

        for (ChunkPos pos : suspiciousChunks) {
            chunkRenderer.renderChunkHighlight(event.matrixStack, pos);
        }
    }

    private void queueNewChunks() {
        if (mc.world == null || mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();

        // Quick scan of all chunks in render distance
        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                ChunkPos pos = new ChunkPos(playerChunk.x + x, playerChunk.z + z);
                
                // Only queue if not scanned and not already queued
                if (!scannedChunks.contains(pos) && !pendingScans.contains(pos)) {
                    pendingScans.add(pos);
                }
            }
        }
    }

    private void scanChunk(WorldChunk chunk) {
        if (chunk == null) return;

        ChunkPos pos = chunk.getPos();
        
        // Mark as scanned immediately to prevent re-queueing
        scannedChunks.add(pos);

        ChunkData data = chunkDataMap.computeIfAbsent(pos, k -> new ChunkData(pos));

        int score = 0;

        // 1. Growth detection (fast)
        score += growthDetector.scanChunk(chunk, data);

        // 2. Kelp gap detection (fast for ocean chunks)
        if (kelpGapDetector.isOceanChunk(chunk)) {
            score += kelpGapDetector.scanForKelpGaps(chunk, data);
        }

        // 3. Underground light detection (fast)
        score += lightDetector.scanForLightPatterns(chunk, data);

        // 4. Pillar detection (fast)
        score += pillarDetector.scanForPillars(chunk, data);

        // 5. Uptime score (instant)
        score += uptimeTracker.calculateUptimeScore(data);

        data.setTotalScore(score);

        // INSTANT HIGHLIGHT - no load time requirement
        if (score >= SUSPICION_THRESHOLD) {
            suspiciousChunks.add(pos);
        }
    }

    private void cleanupDistantChunks() {
        if (mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();

        // Remove chunks outside render distance + buffer
        suspiciousChunks.removeIf(pos -> {
            int dx = Math.abs(pos.x - playerChunk.x);
            int dz = Math.abs(pos.z - playerChunk.z);
            return dx > viewDist + 4 || dz > viewDist + 4;
        });

        scannedChunks.removeIf(pos -> {
            int dx = Math.abs(pos.x - playerChunk.x);
            int dz = Math.abs(pos.z - playerChunk.z);
            return dx > viewDist + 8 || dz > viewDist + 8;
        });

        pendingScans.removeIf(pos -> {
            int dx = Math.abs(pos.x - playerChunk.x);
            int dz = Math.abs(pos.z - playerChunk.z);
            return dx > viewDist + 8 || dz > viewDist + 8;
        });

        chunkDataMap.entrySet().removeIf(entry -> {
            ChunkPos pos = entry.getKey();
            int dx = Math.abs(pos.x - playerChunk.x);
            int dz = Math.abs(pos.z - playerChunk.z);
            return dx > viewDist + 8 || dz > viewDist + 8;
        });
    }
}
