package me.ray.aethelgardRPG.core.performance;

import me.ray.aethelgardRPG.AethelgardRPG;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PerformanceMonitor {

    private final AethelgardRPG plugin;
    private final Map<String, Long> startTimes = new HashMap<>();
    private final Map<String, Long> executionTimes = new HashMap<>(); // Stores last execution time in nanoseconds

    public PerformanceMonitor(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    public void startTiming(String id) {
        boolean enabled = plugin.getConfigManager().getPerformanceConfig().getBoolean("enabled", false);
        // plugin.getLogger().info("[PerfDebug] startTiming called for ID: " + id + " | Monitoring enabled: " + enabled); // Log de Debug
        if (!enabled) {
            return;
        }
        startTimes.put(id, System.nanoTime());
        // plugin.getLogger().info("[PerfDebug] Started timing for: " + id); // Log de Debug
    }

    public void stopTiming(String id) {
        boolean enabled = plugin.getConfigManager().getPerformanceConfig().getBoolean("enabled", false);
        // plugin.getLogger().info("[PerfDebug] stopTiming called for ID: " + id + " | Monitoring enabled: " + enabled + " | Start time exists: " + startTimes.containsKey(id)); // Log de Debug
        if (!enabled || !startTimes.containsKey(id)) {
            return;
        }
        long endTime = System.nanoTime();
        long startTime = startTimes.remove(id);
        long duration = endTime - startTime;
        executionTimes.put(id, duration);
        // plugin.getLogger().info("[PerfDebug] Stopped timing for: " + id + " | Duration: " + (duration / 1_000_000.0) + " ms"); // Log de Debug

        if (plugin.getConfigManager().getPerformanceConfig().getBoolean("log-to-console", false)) {
            plugin.getLogger().info(String.format("Performance: '%s' took %.3f ms", id, duration / 1_000_000.0));
        }
    }
    public Map<String, Long> getExecutionTimes() {
        // Returns a copy to prevent external modification
        return new HashMap<>(executionTimes);
    }
}