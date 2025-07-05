package me.ray.aethelgardRPG.modules.character.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.api.CharacterAPI;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;

public class PlayerEquipmentListener implements Listener {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final CharacterAPI characterAPI;

    public PlayerEquipmentListener(AethelgardRPG plugin, CharacterModule characterModule) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.characterAPI = plugin.getCharacterAPI();
    }

    // Atualiza atributos ao entrar no servidor (após PlayerData ser carregado)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) characterModule.updatePlayerAttributesFromEquipment(event.getPlayer());
        }, 20L);
    }

    // Atualiza atributos ao respawnar
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) characterModule.updatePlayerAttributesFromEquipment(event.getPlayer());
        }, 2L);
    }

    // NOVO: Handler para a ação de equipar com o botão direito.
    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClickEquip(PlayerInteractEvent event) {
        // Lida apenas com cliques do botão direito
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // Garante que o evento seja para a mão principal para evitar disparos duplos
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();

        // Verifica se o item é uma peça de armadura
        if (!isArmorPiece(itemInHand)) {
            return;
        }

        // Agora sabemos que o jogador está tentando equipar uma armadura com o botão direito.
        // Aplicamos as mesmas verificações de requisitos.
        ItemModule itemModule = plugin.getModuleManager().getModule(ItemModule.class);
        if (characterAPI != null && itemModule != null && itemModule.isRPGItem(itemInHand)) {
            // Verifica o requisito de nível
            Optional<Integer> levelReqOpt = itemModule.getItemLevelRequirement(itemInHand);
            if (levelReqOpt.isPresent()) {
                if (characterAPI.getPlayerLevel(player) < levelReqOpt.get()) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getMessage(player, "item.chat_messages.cannot-use.level-too-low", levelReqOpt.get()));
                    player.updateInventory();
                }
            }
            // TODO: Adicionar outras verificações de requisitos aqui (classe, força, etc.), se necessário.
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        ItemStack itemToEquip = null;

        // --- Determina o item que está sendo equipado ---

        // Caso 1: Shift-click de um item do inventário principal.
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (event.getClickedInventory() instanceof PlayerInventory) {
                ItemStack clickedItem = event.getCurrentItem();
                if (isArmorPiece(clickedItem)) {
                    itemToEquip = clickedItem;
                }
            }
        }
        // Caso 2: Colocando um item diretamente em um slot de armadura.
        else if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (action == InventoryAction.SWAP_WITH_CURSOR || action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || action == InventoryAction.PLACE_SOME) {
                itemToEquip = event.getCursor();
            } else if (action == InventoryAction.HOTBAR_SWAP) {
                itemToEquip = player.getInventory().getItem(event.getHotbarButton());
            }
        }

        // --- Realiza a verificação de requisitos ---

        if (itemToEquip != null && itemToEquip.getType() != Material.AIR) {
            ItemModule itemModule = plugin.getModuleManager().getModule(ItemModule.class);
            if (characterAPI != null && itemModule != null && itemModule.isRPGItem(itemToEquip)) {
                // Verifica o requisito de nível
                Optional<Integer> levelReqOpt = itemModule.getItemLevelRequirement(itemToEquip);
                if (levelReqOpt.isPresent()) {
                    if (characterAPI.getPlayerLevel(player) < levelReqOpt.get()) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getMessage(player, "item.chat_messages.cannot-use.level-too-low", levelReqOpt.get()));
                        player.updateInventory(); // Força a atualização visual para o cliente
                        return; // Interrompe o processamento
                    }
                }
                // Adicione outras verificações de requisitos (classe, força, etc.) aqui, se necessário.
            }
        }

        // Se chegamos aqui, a ação é válida ou é uma ação de desequipar.
        // Agendamos a atualização de atributos para o próximo tick para garantir que o inventário esteja atualizado.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                characterModule.updatePlayerAttributesFromEquipment(player);
            }
        }, 1L);
    }

    // Se o jogador fechar o inventário, pode ser uma boa hora para garantir que os atributos estão corretos.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        // Apenas atualiza se for o inventário principal, para evitar atualizações desnecessárias ao fechar baús, etc.
        if (event.getInventory().getType() == InventoryType.CRAFTING) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) characterModule.updatePlayerAttributesFromEquipment(player);
            }, 1L);
        }
    }

    // Para itens dropados de slots de armadura/acessório
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        ItemModule itemModule = plugin.getModuleManager().getModule(ItemModule.class);
        if (itemModule != null && itemModule.isRPGItem(droppedItem)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) characterModule.updatePlayerAttributesFromEquipment(player);
            }, 1L);
        }
    }

    /**
     * Método auxiliar para verificar se um ItemStack é uma peça de armadura.
     * @param item O item a ser verificado.
     * @return true se for um capacete, peitoral, calça ou bota.
     */
    private boolean isArmorPiece(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
}