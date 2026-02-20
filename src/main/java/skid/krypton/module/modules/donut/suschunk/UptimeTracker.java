package skid.krypton.module.modules.donut.suschunk;

public class UptimeTracker {

    public void updateUptime(ChunkData data) {
        data.updateLoadTime();
    }

    public int calculateUptimeScore(ChunkData data) {
        long loadTime = data.getLoadTime(); // in milliseconds
        int timesSeen = data.getTimesSeen();

        // Convert to minutes
        long minutesLoaded = loadTime / (60 * 1000);

        int score = 0;

        // Loaded ≥ 10 minutes total
        if (minutesLoaded >= 10) {
            score += 5;
        }

        // Seen across sessions (timesSeen > 1 implies different sessions)
        if (timesSeen > 1) {
            score += 10;
        }

        // Always loaded (loaded for a long time)
        if (minutesLoaded >= 30) {
            score += 15;
        }

        return score;
    }
}
