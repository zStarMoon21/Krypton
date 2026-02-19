package skid.krypton.manager.tunnel;

import java.util.Random;

public class RandomPause {
    private final Random random = new Random();
    private int pauseTimer = 0;
    private boolean isPaused = false;
    
    public boolean shouldPause() {
        if (isPaused) {
            pauseTimer--;
            if (pauseTimer <= 0) {
                isPaused = false;
            }
            return true;
        }
        
        // Random pause every 30-60 seconds
        if (random.nextInt(1000) == 0) { // ~every 50 seconds
            pauseTimer = 20 + random.nextInt(100); // 1-6 seconds (20 ticks = 1 second)
            isPaused = true;
            return true;
        }
        
        return false;
    }
    
    public void resetPause() {
        isPaused = false;
        pauseTimer = 0;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
}
