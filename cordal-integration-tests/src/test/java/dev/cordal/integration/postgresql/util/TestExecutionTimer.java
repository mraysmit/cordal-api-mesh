package dev.cordal.integration.postgresql.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for timing test execution phases and providing performance insights
 * Tracks execution time for different phases of integration testing
 */
public class TestExecutionTimer {
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionTimer.class);
    
    private final Map<String, PhaseTimer> phaseTimers;
    private final Instant testStartTime;
    private String currentPhase;
    
    public TestExecutionTimer() {
        this.phaseTimers = new LinkedHashMap<>();
        this.testStartTime = Instant.now();
        this.currentPhase = null;
        logger.debug("Test execution timer initialized");
    }
    
    /**
     * Start timing a new phase
     * 
     * @param phaseName Name of the phase to start timing
     */
    public void startPhase(String phaseName) {
        // End current phase if one is running
        if (currentPhase != null) {
            endPhase(currentPhase);
        }
        
        currentPhase = phaseName;
        PhaseTimer timer = new PhaseTimer(phaseName, Instant.now());
        phaseTimers.put(phaseName, timer);
        
        logger.info("⏱️  Started timing phase: {}", phaseName);
    }
    
    /**
     * End timing the current phase
     * 
     * @param phaseName Name of the phase to end (must match current phase)
     */
    public void endPhase(String phaseName) {
        if (!phaseName.equals(currentPhase)) {
            logger.warn("Attempting to end phase '{}' but current phase is '{}'", phaseName, currentPhase);
            return;
        }
        
        PhaseTimer timer = phaseTimers.get(phaseName);
        if (timer != null && timer.endTime == null) {
            timer.endTime = Instant.now();
            timer.duration = Duration.between(timer.startTime, timer.endTime);
            
            logger.info("⏱️  Completed phase: {} in {}", phaseName, formatDuration(timer.duration));
        }
        
        currentPhase = null;
    }
    
    /**
     * Get the duration of a specific phase
     * 
     * @param phaseName Name of the phase
     * @return Duration of the phase, or null if phase not found or not completed
     */
    public Duration getPhaseDuration(String phaseName) {
        PhaseTimer timer = phaseTimers.get(phaseName);
        return timer != null ? timer.duration : null;
    }
    
    /**
     * Get the total test execution time so far
     * 
     * @return Duration since test started
     */
    public Duration getTotalDuration() {
        return Duration.between(testStartTime, Instant.now());
    }
    
    /**
     * Check if a phase is currently running
     * 
     * @param phaseName Name of the phase to check
     * @return true if the phase is currently running
     */
    public boolean isPhaseRunning(String phaseName) {
        return phaseName.equals(currentPhase);
    }
    
    /**
     * Get all completed phases with their durations
     * 
     * @return Map of phase name to duration
     */
    public Map<String, Duration> getCompletedPhases() {
        Map<String, Duration> completed = new LinkedHashMap<>();
        
        for (Map.Entry<String, PhaseTimer> entry : phaseTimers.entrySet()) {
            PhaseTimer timer = entry.getValue();
            if (timer.duration != null) {
                completed.put(entry.getKey(), timer.duration);
            }
        }
        
        return completed;
    }
    
    /**
     * Log a summary of all phase timings
     */
    public void logSummary() {
        logger.info("=== TEST EXECUTION TIMING SUMMARY ===");
        
        Duration totalDuration = getTotalDuration();
        logger.info("Total test execution time: {}", formatDuration(totalDuration));
        
        if (phaseTimers.isEmpty()) {
            logger.info("No phases were timed");
            return;
        }
        
        logger.info("Phase breakdown:");
        
        Duration totalPhaseDuration = Duration.ZERO;
        for (Map.Entry<String, PhaseTimer> entry : phaseTimers.entrySet()) {
            String phaseName = entry.getKey();
            PhaseTimer timer = entry.getValue();
            
            if (timer.duration != null) {
                totalPhaseDuration = totalPhaseDuration.plus(timer.duration);
                double percentage = (timer.duration.toMillis() * 100.0) / totalDuration.toMillis();
                
                logger.info("  📊 {}: {} ({:.1f}%)", 
                           phaseName, formatDuration(timer.duration), percentage);
            } else {
                logger.info("  ⚠️  {}: Not completed", phaseName);
            }
        }
        
        // Calculate overhead (time not accounted for by phases)
        Duration overhead = totalDuration.minus(totalPhaseDuration);
        if (!overhead.isNegative() && !overhead.isZero()) {
            double overheadPercentage = (overhead.toMillis() * 100.0) / totalDuration.toMillis();
            logger.info("  🔧 Overhead: {} ({:.1f}%)", formatDuration(overhead), overheadPercentage);
        }
        
        logger.info("=== END TIMING SUMMARY ===");
    }
    
    /**
     * Log performance insights and recommendations
     */
    public void logPerformanceInsights() {
        logger.info("=== PERFORMANCE INSIGHTS ===");
        
        Map<String, Duration> completed = getCompletedPhases();
        if (completed.isEmpty()) {
            logger.info("No completed phases to analyze");
            return;
        }
        
        // Find slowest phase
        String slowestPhase = null;
        Duration slowestDuration = Duration.ZERO;
        
        for (Map.Entry<String, Duration> entry : completed.entrySet()) {
            if (entry.getValue().compareTo(slowestDuration) > 0) {
                slowestPhase = entry.getKey();
                slowestDuration = entry.getValue();
            }
        }
        
        if (slowestPhase != null) {
            logger.info("🐌 Slowest phase: {} ({})", slowestPhase, formatDuration(slowestDuration));
        }
        
        // Find fastest phase
        String fastestPhase = null;
        Duration fastestDuration = Duration.ofDays(1); // Start with a large duration
        
        for (Map.Entry<String, Duration> entry : completed.entrySet()) {
            if (entry.getValue().compareTo(fastestDuration) < 0) {
                fastestPhase = entry.getKey();
                fastestDuration = entry.getValue();
            }
        }
        
        if (fastestPhase != null) {
            logger.info("🚀 Fastest phase: {} ({})", fastestPhase, formatDuration(fastestDuration));
        }
        
        // Performance recommendations
        Duration totalDuration = getTotalDuration();
        if (totalDuration.toSeconds() > 60) {
            logger.info("💡 Test took over 1 minute - consider optimizing container startup or data generation");
        }
        
        if (slowestDuration.toSeconds() > 30) {
            logger.info("💡 Phase '{}' took over 30 seconds - consider optimization", slowestPhase);
        }
        
        logger.info("=== END PERFORMANCE INSIGHTS ===");
    }
    
    /**
     * Format a duration for human-readable display
     * 
     * @param duration Duration to format
     * @return Formatted string
     */
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "N/A";
        }
        
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = duration.toMillis() % 1000;
        
        if (minutes > 0) {
            return String.format("%dm %d.%03ds", minutes, seconds, millis);
        } else if (seconds > 0) {
            return String.format("%d.%03ds", seconds, millis);
        } else {
            return String.format("%dms", millis);
        }
    }
    
    /**
     * Reset the timer (clear all phases and restart)
     */
    public void reset() {
        phaseTimers.clear();
        currentPhase = null;
        logger.debug("Test execution timer reset");
    }
    
    /**
     * Internal class to track phase timing information
     */
    private static class PhaseTimer {
        final String phaseName;
        final Instant startTime;
        Instant endTime;
        Duration duration;
        
        PhaseTimer(String phaseName, Instant startTime) {
            this.phaseName = phaseName;
            this.startTime = startTime;
        }
    }
}
