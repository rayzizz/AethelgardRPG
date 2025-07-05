package me.ray.aethelgardRPG.modules.area.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.area.AreaModule;
import me.ray.aethelgardRPG.modules.area.RPGRegion;
import me.ray.aethelgardRPG.modules.area.areas.AreaManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

public class PlayerAreaListener implements Listener {

    private final AethelgardRPG plugin;
    private final AreaManager areaManager;

    public PlayerAreaListener(AethelgardRPG plugin, AreaModule areaModule) {
        this.plugin = plugin;
        this.areaManager = areaModule.getAreaManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Otimização: só checar se o jogador mudou de bloco
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        checkAndUpdatePlayerRegion(event.getPlayer(), to);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        checkAndUpdatePlayerRegion(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkAndUpdatePlayerRegion(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        areaManager.removePlayerFromCache(event.getPlayer());
    }

    private void checkAndUpdatePlayerRegion(Player player, Location newLocation) {
        Optional<RPGRegion> regionAtNewLocation = areaManager.getRegionAt(newLocation);
        areaManager.updatePlayerRegion(player, regionAtNewLocation.orElse(null));
    }
}