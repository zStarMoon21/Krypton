package skid.krypton.manager.tunnel;

import java.util.Random;

public class SmartPauser {
    private final Random random = new Random();
    
    private int pauseTimer = 0;
    private int nextPauseTimer = 0;
    private boolean isPaused = false;
    
    private static final int MIN_INTERVAL = 200;
    private static final int MAX_INTERVAL = 340;
    private static final int MIN_PAUSE = 20;
    private static final int MAX_PAUSE = 100;
    
    public SmartPauser() {
        scheduleNextPause();
    }
    
    private void scheduleNextPause() {
        nextPauseTimer = MIN_INTERVAL + random.nextInt(MAX_INTERVAL - MIN_INTERVAL);
    }
    
    public boolean shouldPause() {
        if (isPaused) {
            pauseTimer--;
            if (pauseTimer <= 0) {
                isPaused = false;
                scheduleNextPause();
            }
            return true;
        }
        
        nextPauseTimer--;
        if (nextPauseTimer <= 0) {
            pauseTimer = MIN_PAUSE + random.nextInt(MAX_PAUSE - MIN_PAUSE);
            isPaused = true;
            return true;
        }
        
        return false;
    }
    
    public void reset() {
        isPaused = false;
        pauseTimer = 0;
        scheduleNextPause();
    }
    
    public boolean isPaused() {
        return isPaused;
    }
}
