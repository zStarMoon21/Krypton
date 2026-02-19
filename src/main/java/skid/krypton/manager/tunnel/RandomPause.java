package skid.krypton.manager.tunnel;

import java.util.Random;

public class RandomPause {
    private final Random random = new Random();

    private int pauseTimer;
    private int ticksUntilNextPause;
    private boolean paused;

    // Human-like timing (15s–40s between pauses)
    private static final int MIN_INTERVAL = 300;
    private static final int MAX_INTERVAL = 800;

    // Pause length (0.3s–4s)
    private static final int MIN_PAUSE = 6;
    private static final int MAX_PAUSE = 80;

    public RandomPause() {
        scheduleNextPause();
    }

    private void scheduleNextPause() {
        ticksUntilNextPause = MIN_INTERVAL + random.nextInt(MAX_INTERVAL - MIN_INTERVAL);
    }

    public boolean shouldPause() {
        if (paused) {
            if (--pauseTimer <= 0) {
                paused = false;
                scheduleNextPause();
            }
            return true;
        }

        if (--ticksUntilNextPause <= 0) {
            pauseTimer = MIN_PAUSE + random.nextInt(MAX_PAUSE - MIN_PAUSE);
            paused = true;
            return true;
        }

        return false;
    }

    public void resetPause() {
        paused = false;
        pauseTimer = 0;
        scheduleNextPause();
    }

    public boolean isPaused() {
        return paused;
    }

    public int getTicksUntilNextPause() {
        return ticksUntilNextPause;
    }
}
