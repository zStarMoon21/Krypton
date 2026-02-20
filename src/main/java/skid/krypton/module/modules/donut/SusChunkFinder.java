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
import skid.krypton.utils.EncryptedString;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SusChunkFinder extends Module {

    // Settings
    private final BooleanSetting debug = new BooleanSetting("Debug", false);
    private final BooleanSetting holeEsp = new BooleanSetting("Hole ESP", true); // NEW OPTION

    // Detection components
    private GrowthDetector growthDetector;
    private KelpGapDetector kelpGapDetector;
    private LightDetector lightDetector;
    private PillarDetector pillarDetector;
    private UptimeTracker uptimeTracker;
    private ChunkRenderer chunkRenderer;
    private HoleRenderer holeRenderer; // NEW

    // Chunk data storage
    private final Map<ChunkPos, ChunkData> chunkDataMap = new ConcurrentHashMap<>();
    private final Set<ChunkPos> suspiciousChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();

    // Fixed threshold
    private static final int SUSPICION_THRESHOLD = 30;

    // Scan timing
    private int scanIndex = 0;
    private int cleanupCooldown = 0;
    private List<WorldChunk> loadedChunks = new ArrayList<>();

    public SusChunkFinder() {
        super("SusChunk", "Highlights suspicious chunks and holes", -1, Category.DONUT);
        this.addSettings(debug, holeEsp); // Added holeEsp to settings
    }

    @Override
    public void onEnable() {
        growthDetector = new GrowthDetector();
        kelpGapDetector = new KelpGapDetector();
        lightDetector = new LightDetector();
        pillarDetector = new PillarDetector();
        uptimeTracker = new UptimeTracker();
        chunkRenderer = new ChunkRenderer(mc);
        holeRenderer = new HoleRenderer(mc); // Initialize hole renderer

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

        // Update loaded chunks list every 20 ticks
        if (scanIndex == 0) {
            updateLoadedChunks();
        }

        // Scan one chunk per tick to spread out workload
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
        if (mc.player == null) return;

        // Render suspicious chunks
        if (!suspiciousChunks.isEmpty()) {
            for (ChunkPos pos : suspiciousChunks) {
                chunkRenderer.renderChunkHighlight(event.matrixStack, pos);
            }
        }

        // Render holes if enabled
        if (holeEsp.getValue()) {
            holeRenderer.renderHoles(event.matrixStack);
        }

        // Debug info if enabled
        if (debug.getValue() && !suspiciousChunks.isEmpty()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§aSuspicious chunks: " + suspiciousChunks.size()), true);
            }
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

        // Growth detection
        score += growthDetector.scanChunk(chunk, data);

        // Kelp gap detection (ocean bases)
        if (kelpGapDetector.isOceanChunk(chunk)) {
            score += kelpGapDetector.scanForKelpGaps(chunk, data);
        }

        // Underground light detection
        score += lightDetector.scanForLightPatterns(chunk, data);

        // Pillar detection
        score += pillarDetector.scanForPillars(chunk, data);

        // Uptime score
        score += uptimeTracker.calculateUptimeScore(data);

        data.setTotalScore(score);
        data.incrementTimesSeen();

        // Mark as suspicious if threshold reached (fixed at 30)
        if (score >= SUSPICION_THRESHOLD) {
            suspiciousChunks.add(pos);
            
            if (debug.getValue() && mc.player != null) {
                mc.player.sendMessage(Text.literal("§aFound suspicious chunk at [" + pos.x + ", " + pos.z + "] score: " + score), false);
            }
        }
    }

    private void cleanupDistantChunks() {
        if (mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();

        // Remove chunks that are too far away
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
