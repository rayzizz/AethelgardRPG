package me.ray.aethelgardRPG.modules.character.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class PlayerHungerListener implements Listener {

    private final AethelgardRPG plugin;

    public PlayerHungerListener(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            // Para todos os outros jogadores, cancela qualquer alteração na barra de fome.
            // A fome é substituída pelo sistema de Stamina.
            event.setCancelled(true);
        }
    }
}