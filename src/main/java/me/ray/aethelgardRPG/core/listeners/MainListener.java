package me.ray.aethelgardRPG.core.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MainListener implements Listener {

    private final AethelgardRPG plugin;

    public MainListener(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    // Example:
    // @EventHandler
    // public void onPluginEnableEvent(PluginEnableEvent event) {
    // if (event.getPlugin().equals(plugin)) {
    // plugin.getLogger().info("MainListener detected plugin enable.");
    // }
    // }
    
}