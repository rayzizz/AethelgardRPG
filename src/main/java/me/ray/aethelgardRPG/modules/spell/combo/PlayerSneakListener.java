package me.ray.aethelgardRPG.modules.spell.combo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerSneakListener implements Listener {

    private final ComboDetector comboDetector;

    public PlayerSneakListener(ComboDetector comboDetector) {
        this.comboDetector = comboDetector;
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) { // Jogador parou de agachar
            comboDetector.handlePlayerStopSneak(player);
        }
    }
}
