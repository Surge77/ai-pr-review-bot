package com.aireviewer.github;

/**
 * Abstraction over blocking sleeps so rate-limit backoff can be unit-tested
 * without real delays.
 */
public interface Sleeper {

    /**
     * Sleeps for the given duration.
     *
     * @param millis milliseconds to sleep (values {@code <= 0} are a no-op)
     */
    void sleep(long millis);
}
