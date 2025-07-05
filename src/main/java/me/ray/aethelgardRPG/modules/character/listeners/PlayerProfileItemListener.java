package me.ray.aethelgardRPG.modules.character.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.guis.PlayerProfileGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;

public class PlayerProfileItemListener implements Listener {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;

    public PlayerProfileItemListener(AethelgardRPG plugin, CharacterModule characterModule) {
        this.plugin = plugin;
        this.characterModule = characterModule;
    }

    private boolean isProfileCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        // Utiliza a chave pública do CharacterModule para a verificação
        return item.getItemMeta().getPersistentDataContainer().has(characterModule.PROFILE_COMPASS_KEY, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Agenda para rodar um pouco depois, garantindo que o inventário esteja estabelecido
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            characterModule.giveOrUpdateProfileCompass(event.getPlayer());
        }, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isProfileCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getMessage(event.getPlayer(), "general.cannot-drop-item"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        int compassSlot = plugin.getConfigManager().getCharacterSettingsConfig().getInt("profile_compass.slot", 8);

        // Impede que a bússola seja movida para fora de seu slot
        if (event.getSlot() == compassSlot && event.getClickedInventory() instanceof PlayerInventory && isProfileCompass(currentItem)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessage(player, "general.cannot-move-item"));
        }

        // Impede que outros itens sejam colocados no slot da bússola
        if (event.getSlot() == compassSlot && event.getClickedInventory() instanceof PlayerInventory && cursorItem != null && cursorItem.getType() != Material.AIR) {
            ItemStack itemInSlot = player.getInventory().getItem(compassSlot);
            if (isProfileCompass(itemInSlot)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessage(player, "general.cannot-place-in-slot"));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Remove a bússola dos drops na morte para que o jogador não a perca
        event.getDrops().removeIf(this::isProfileCompass);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isProfileCompass(event.getItem())) {
                event.setCancelled(true);
                // *** ALTERAÇÃO AQUI ***
                // Passamos o jogador e o módulo diretamente no construtor.
                new PlayerProfileGUI(plugin, characterModule, player).open();
            }
        }
    }
}