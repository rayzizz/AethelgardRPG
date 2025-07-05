package me.ray.aethelgardRPG.modules.item.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.api.CharacterAPI;
import me.ray.aethelgardRPG.modules.item.ItemModule;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class PlayerItemUseListener implements Listener {

    private final AethelgardRPG plugin;
    private final ItemModule itemModule;
    private final CharacterAPI characterAPI;

    public PlayerItemUseListener(AethelgardRPG plugin) {
        this.plugin = plugin;
        this.itemModule = plugin.getModuleManager().getModule(ItemModule.class);
        this.characterAPI = plugin.getCharacterAPI();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (itemModule == null || characterAPI == null) return;

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack itemInHand = event.getItem();

            if (itemInHand != null && itemModule.isRPGItem(itemInHand)) {
                Optional<Integer> levelReqOpt = itemModule.getItemLevelRequirement(itemInHand);
                if (levelReqOpt.isPresent()) {
                    if (characterAPI.getPlayerLevel(player) < levelReqOpt.get()) {
                        event.setCancelled(true);
                        // CORREÇÃO: Usar o idioma individual do jogador
                        player.sendMessage(plugin.getMessage(player, "item.chat_messages.cannot-use.level-too-low", levelReqOpt.get()));
                        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
                            player.updateInventory();
                        }
                    }
                }
            }
        }
    }
}