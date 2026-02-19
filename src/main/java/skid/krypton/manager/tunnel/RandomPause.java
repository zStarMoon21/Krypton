package skid.krypton.manager.tunnel;

import java.util.Random;

public class RandomPause {
    private final Random random = new Random();
    private int pauseTimer = 0;
    private boolean isPaused = false;
    
    // Stops every 10-17 seconds (200-340 ticks)
    private static final int MIN_INTERVAL = 200;  // 10 seconds minimum between pauses
    private static final int MAX_INTERVAL = 340;  // 17 seconds maximum between pauses
    
    private static final int MIN_PAUSE_TIME = 20;  // 1 second minimum
    private static final int MAX_PAUSE_TIME = 100; // 5 seconds maximum
    
    private int ticksUntilNextPause = 0;
    
    public RandomPause() {
        // Initialize random first pause
        scheduleNextPause();
    }
    
    private void scheduleNextPause() {
        ticksUntilNextPause = MIN_INTERVAL + random.nextInt(MAX_INTERVAL - MIN_INTERVAL);
    }
    
    public boolean shouldPause() {
        if (isPaused) {
            pauseTimer--;
            if (pauseTimer <= 0) {
                isPaused = false;
                scheduleNextPause(); // Schedule next pause after this one ends
            }
            return true;
        }
        
        // Count down to next pause
        if (ticksUntilNextPause > 0) {
            ticksUntilNextPause--;
        }
        
        // Time for a pause
        if (ticksUntilNextPause <= 0) {
            pauseTimer = MIN_PAUSE_TIME + random.nextInt(MAX_PAUSE_TIME - MIN_PAUSE_TIME);
            isPaused = true;
            return true;
        }
        
        return false;
    }
    
    public void resetPause() {
        isPaused = false;
        pauseTimer = 0;
        scheduleNextPause();
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public int getTicksUntilNextPause() {
        return ticksUntilNextPause;
    }
}
