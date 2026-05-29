package com.aireviewer.github;

import org.springframework.stereotype.Component;

/**
 * Default {@link Sleeper} backed by {@link Thread#sleep(long)}.
 */
@Component
public class ThreadSleeper implements Sleeper {

    @Override
    public void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
