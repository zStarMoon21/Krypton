package skid.krypton.module.modules.donut.suschunk;

public class UptimeTracker {

    public void updateUptime(ChunkData data) {
        data.updateLoadTime();
    }

    public int calculateUptimeScore(ChunkData data) {
        long minutesLoaded = data.getLoadTime() / (60 * 1000);
        int timesSeen = data.getTimesSeen();

        if (minutesLoaded >= 30) return 20;
        if (timesSeen > 1) return 10;
        if (minutesLoaded >= 10) return 5;
        return 0;
    }
}
