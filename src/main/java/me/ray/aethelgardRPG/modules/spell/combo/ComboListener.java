package me.ray.aethelgardRPG.modules.spell.combo;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot; // Import necessário

public class ComboListener implements Listener {

    private final AethelgardRPG plugin;
    private final ComboDetector comboDetector;

    public ComboListener(AethelgardRPG plugin, ComboDetector comboDetector) {
        this.plugin = plugin;
        this.comboDetector = comboDetector;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // CORREÇÃO: Garante que o evento seja processado apenas para a mão principal,
        // evitando que cada clique seja registrado duas vezes.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            comboDetector.recordClick(player, ClickType.LEFT);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            comboDetector.recordClick(player, ClickType.RIGHT);
        }
    }

}