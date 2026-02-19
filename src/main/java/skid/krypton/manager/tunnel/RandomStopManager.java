package skid.krypton.manager.tunnel;

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
        
        if (random.nextInt(1200) == 0) { // ~every 60 seconds
            stopDuration = 20 + random.nextInt(70); // 1-3.5 seconds
            isStopped = true;
            return true;
        }
        
        return false;
    }
    
    public boolean isStopped() {
        return isStopped;
    }
}
