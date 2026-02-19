package skid.krypton.module.modules.donut.tunnel;

import java.util.Random;

public class RandomStopManager {
    private int stopTimer = 0;
    private boolean isStopped = false;
    private int stopDuration = 0;
    private final Random random = new Random();
    
    public boolean shouldStop() {
        if (isStopped) {
            stopDuration--;
            if (stopDuration <= 0) {
                isStopped = false;
            }
            return true;
        }
        
        // Random stop chance (about every 30-60 seconds)
        // 1200 ticks = 60 seconds
        if (random.nextInt(1200) == 0) {
            stopDuration = 20 + random.nextInt(70); // 1 to 3.5 seconds
            isStopped = true;
            return true;
        }
        
        return false;
    }
    
    public void executeStop() {
        // Just stop moving - handled in main loop
    }
    
    public boolean isStopped() {
        return isStopped;
    }
}
