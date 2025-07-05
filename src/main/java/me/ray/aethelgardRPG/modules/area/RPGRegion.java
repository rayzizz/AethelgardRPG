package me.ray.aethelgardRPG.modules.area;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Set;

public class RPGRegion {
    private final String id; // ID único da região
    private final String displayName;
    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final int priority; // Para resolver sobreposições, maior prioridade prevalece

    // Regras da área
    private boolean pvpAllowed;
    private Set<String> allowedMobSpawns; // IDs de mobs customizados ou EntityTypes vanilla
    private boolean preventDefaultMobSpawns; // Se true, apenas mobs em allowedMobSpawns podem spawnar
    private List<String> activeEvents; // IDs de eventos que podem ocorrer nesta área

    // Mensagens de entrada/saída
    private String entryMessage;
    private String exitMessage;

    public RPGRegion(String id, String displayName, World world,
                     int x1, int y1, int z1, int x2, int y2, int z2,
                     int priority, boolean pvpAllowed, Set<String> allowedMobSpawns,
                     boolean preventDefaultMobSpawns, List<String> activeEvents,
                     String entryMessage, String exitMessage) {
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.priority = priority;
        this.pvpAllowed = pvpAllowed;
        this.allowedMobSpawns = allowedMobSpawns;
        this.preventDefaultMobSpawns = preventDefaultMobSpawns;
        this.activeEvents = activeEvents;
        this.entryMessage = entryMessage;
        this.exitMessage = exitMessage;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public World getWorld() { return world; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    public int getPriority() { return priority; }
    public boolean isPvpAllowed() { return pvpAllowed; }
    public Set<String> getAllowedMobSpawns() { return allowedMobSpawns; }
    public boolean isPreventDefaultMobSpawns() { return preventDefaultMobSpawns; }
    public List<String> getActiveEvents() { return activeEvents; }
    public String getEntryMessage() { return entryMessage; }
    public String getExitMessage() { return exitMessage; }

    public void setPvpAllowed(boolean pvpAllowed) { this.pvpAllowed = pvpAllowed; }
    // ... outros setters se as regras puderem ser alteradas dinamicamente

    public boolean contains(Location location) {
        if (location == null || !location.getWorld().equals(this.world)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    @Override
    public String toString() {
        return "RPGRegion{" +
               "id='" + id + '\'' +
               ", displayName='" + displayName + '\'' +
               ", priority=" + priority +
               '}';
    }
}