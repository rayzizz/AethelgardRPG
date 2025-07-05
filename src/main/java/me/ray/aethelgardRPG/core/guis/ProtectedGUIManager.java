package me.ray.aethelgardRPG.core.guis;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectedGUIManager implements Listener {

    // Usamos um ConcurrentHashMap para segurança em operações assíncronas, como o logout do jogador.
    private final Map<UUID, ItemStack[]> savedInventories = new ConcurrentHashMap<>();

    /**
     * Chamado quando um jogador abre qualquer inventário.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        // Verifica se a GUI aberta é uma 'ProtectedGUI'.
        if (event.getInventory().getHolder() instanceof ProtectedGUI) {
            Player player = (Player) event.getPlayer();
            UUID playerUUID = player.getUniqueId();

            // Se o jogador já tiver um inventário salvo (caso raro, ex: bug ou outro plugin),
            // não sobrescrevemos para evitar perda de itens.
            if (savedInventories.containsKey(playerUUID)) {
                return;
            }

            // Salva o inventário atual do jogador.
            ItemStack[] contents = player.getInventory().getContents();
            savedInventories.put(playerUUID, contents);

            // Limpa o inventário do jogador.
            player.getInventory().clear();
        }
    }

    /**
     * Chamado quando um jogador fecha qualquer inventário.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Se o inventário do jogador foi salvo, restaura-o.
        // Usamos remove() para pegar o inventário e removê-lo do mapa em uma única operação.
        ItemStack[] savedContents = savedInventories.remove(playerUUID);
        if (savedContents != null) {
            // Restaura os itens.
            player.getInventory().setContents(savedContents);
        }
    }

    /**
     * Lida com o caso de um jogador deslogar com uma GUI protegida aberta.
     * Isso é CRUCIAL para evitar que os jogadores percam seus itens.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Se o jogador deslogou e seu inventário estava salvo, restaura-o imediatamente.
        ItemStack[] savedContents = savedInventories.remove(playerUUID);
        if (savedContents != null) {
            player.getInventory().setContents(savedContents);
            System.out.println("[AethelgardRPG] Inventário de " + player.getName() + " restaurado durante o logout forçado.");
        }
    }
}