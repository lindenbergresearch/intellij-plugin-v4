package org.antlr.intellij.plugin.misc;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Locale;

/**
 * A high-precision stopwatch that supports starting, stopping, resetting,
 * and querying the elapsed time in both nanoseconds and human-readable formats.
 *
 * <p>Precision:
 * <ul>
 *     <li>Below 1 ms: microseconds with two decimal places</li>
 *     <li>Below 1 s: milliseconds with two decimal places</li>
 *     <li>Below 1 min: seconds with two decimal places</li>
 *     <li>Below 1 hour: minutes and seconds</li>
 *     <li>1 hour and above: hours, minutes, and seconds</li>
 * </ul>
 */
@Getter
@Setter
public class Stopwatch {
    private long startTime;
    private long elapsedNanos;
    private boolean running;
    
    
    public Stopwatch() {
        reset();
    }
    
    
    public static Stopwatch createStarted() {
        var stopwatch = new Stopwatch();
        stopwatch.start();
        return stopwatch;
    }
    
    
    /**
     * Starts the stopwatch if it is not already running.
     */
    public void start() {
        if (!running) {
            startTime = System.nanoTime();
            running = true;
        }
    }
    
    
    /**
     * Stops the stopwatch and adds the elapsed time to total.
     */
    public void stop() {
        if (running) {
            elapsedNanos += System.nanoTime() - startTime;
            running = false;
        }
    }
    
    
    /**
     * Resets the stopwatch.
     */
    public void reset() {
        elapsedNanos = 0;
        running = false;
    }
    
    
    /**
     * Restarts the stopwatch.
     */
    public void restart() {
        reset();
        start();
    }
    
    
    /**
     * Returns the current time in a human-readable format and resets
     * the stopwatch.
     *
     * @return Human-readable elapsed time.
     */
    public String getElapsedTimeAndReset() {
        var elapsedTime = this.toString();
        reset();
        return elapsedTime;
    }
    
    
    /**
     * Tests if the current measured time exceeds the given value.
     *
     * @param millis Threshold in milliseconds.
     * @return True if the given time exceeds the current measured time.
     */
    public boolean exceedsMillis(long millis) {
        return (elapsedNanos / 1_000_000.f) >= millis;
    }
    
    
    /**
     * Tests if the current measured time exceeds the given value.
     *
     * @param seconds Threshold in seconds.
     * @return True if the given time exceeds the current measured time.
     */
    public boolean exceedsSeconds(long seconds) {
        return (elapsedNanos / 1_000_000_000.f) >= seconds;
    }
    
    
    /**
     * Returns the elapsed time in nanoseconds.
     *
     * @return total elapsed time in nanoseconds
     */
    public long getElapsedTimeNanos() {
        return running ? elapsedNanos + (System.nanoTime() - startTime) : elapsedNanos;
    }
    
    
    public Duration getElapsedTimeDuration() {
        return Duration.ofNanos(getElapsedTimeNanos());
    }
    
    
    /**
     * Returns the elapsed time as a human-readable formatted string.
     *
     * @return formatted elapsed time string
     */
    public String getElapsedTimeFormatted() {
        var nanos = getElapsedTimeNanos();
        var duration = Duration.ofNanos(nanos);
        
        var hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (duration.toMillis() < 1) {
            // Below 1 ms: show in microseconds with 2 decimal places
            var us = nanos / 1_000.0;
            return String.format(Locale.ENGLISH, "%.2fµs", us);
        }
        if (duration.toSeconds() < 1) {
            // Below 1 s: show in milliseconds with 2 decimal places
            var ms = nanos / 1_000_000.0;
            return String.format(Locale.ENGLISH, "%.2fms", ms);
        }
        if (duration.toMinutes() < 1) {
            // Below 1 minute: show in seconds with 2 decimal places
            var sec = nanos / 1_000_000_000.0;
            return String.format(Locale.ENGLISH, "%.2fs", sec);
        }
        if (duration.toHours() < 1) {
            // Below 1 hour: format as Xm Ys
            return String.format(Locale.ENGLISH, "%dm %02ds", minutes, seconds);
        }
        // 1 hour or more: format as Xh Ym Zs
        return String.format(Locale.ENGLISH, "%dh %02dm %02ds", hours, minutes, seconds);
    }
    
    
    /**
     * Returns a string representation of the current stopwatch time.
     *
     * @return human-readable formatted time
     */
    @Override
    public String toString() {
        return getElapsedTimeFormatted();
    }
}