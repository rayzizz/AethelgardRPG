package me.ray.aethelgardRPG.modules.character.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.api.CharacterAPI;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.List;
import java.util.Optional;

public class AccessorySlotListener implements Listener {

    private final AethelgardRPG plugin;
    private final ItemModule itemModule;
    private final CharacterAPI characterAPI;
    private final NamespacedKey PLACEHOLDER_KEY;

    public AccessorySlotListener(AethelgardRPG plugin) {
        this.plugin = plugin;
        this.itemModule = plugin.getModuleManager().getModule(ItemModule.class);
        this.characterAPI = plugin.getCharacterAPI();
        this.PLACEHOLDER_KEY = new NamespacedKey(plugin, "accessory_placeholder");
    }

    /**
     * Atualiza os placeholders de acessórios no inventário de um jogador.
     * Este método é chamado quando o inventário do jogador pode ter mudado (login, troca de idioma, etc.).
     * @param player O jogador cujo inventário deve ser atualizado.
     */
    public void refreshAccessoryPlaceholders(Player player) {
        if (!isPlaceholderPanesEnabled()) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        List<Integer> reservedSlots = getReservedSlots();
        // CORREÇÃO: Cria o placeholder com o contexto de idioma do jogador.
        ItemStack placeholder = createPlaceholderItem(player);

        for (int slot : reservedSlots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                ItemStack currentItem = inventory.getItem(slot);
                // CORREÇÃO: Substitui o placeholder existente para atualizar o idioma.
                if (currentItem == null || currentItem.getType() == Material.AIR || isPlaceholderItem(currentItem)) {
                    inventory.setItem(slot, placeholder);
                }
            }
        }
    }

    private void schedulePlaceholderRefresh(Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                if (isPlaceholderPanesEnabled()) {
                    this.refreshAccessoryPlaceholders(player);
                }
            }
        }, 1L);
    }

    private List<Integer> getReservedSlots() {
        return plugin.getConfigManager().getCharacterSettingsConfig().getIntegerList("accessory-slots.reserved");
    }

    private boolean isAccessorySlotsEnabled() {
        return plugin.getConfigManager().getCharacterSettingsConfig().getBoolean("accessory-slots.enabled", false);
    }

    private boolean isPlaceholderPanesEnabled() {
        return plugin.getConfigManager().getCharacterSettingsConfig().getBoolean("accessory-slots.use-placeholder-panes", true);
    }

    /**
     * Cria o item de placeholder (painel de vidro) com o nome traduzido para o jogador.
     * @param playerContext O jogador para quem o item está sendo criado.
     * @return O ItemStack do placeholder.
     */
    private ItemStack createPlaceholderItem(Player playerContext) {
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            // CORREÇÃO: Usa o contexto do jogador para obter a mensagem no idioma correto.
            meta.setDisplayName(plugin.getMessage(playerContext, "character.accessory_slot.placeholder.name"));
            meta.getPersistentDataContainer().set(PLACEHOLDER_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }

    public boolean isPlaceholderItem(ItemStack item) {
        return item != null && item.getType() == Material.BLACK_STAINED_GLASS_PANE &&
                item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(PLACEHOLDER_KEY, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (itemModule == null || characterAPI == null || !isAccessorySlotsEnabled()) return;

        Player player = (Player) event.getWhoClicked();
        List<Integer> reservedSlots = getReservedSlots();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory instanceof PlayerInventory) {
            int clickedSlot = event.getSlot();

            if (reservedSlots.contains(clickedSlot)) {
                ItemStack itemOnCursor = event.getCursor();
                ItemStack itemInSlot = event.getCurrentItem();

                if (itemOnCursor == null || itemOnCursor.getType() == Material.AIR) {
                    if (isPlaceholderItem(itemInSlot)) {
                        event.setCancelled(true);
                        return;
                    }
                    schedulePlaceholderRefresh(player);
                    return;
                }

                if (itemOnCursor.getType() != Material.AIR) {
                    if (isPlaceholderItem(itemOnCursor)) {
                        event.setCancelled(true);
                        return;
                    }
                    if (!itemModule.isAccessory(itemOnCursor)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getMessage(player, "general.cannot-place-non-accessory-in-slot"));
                        return;
                    }
                    Optional<Integer> levelReqOpt = itemModule.getItemLevelRequirement(itemOnCursor);
                    if (levelReqOpt.isPresent()) {
                        if (characterAPI.getPlayerLevel(player) < levelReqOpt.get()) {
                            event.setCancelled(true);
                            player.sendMessage(plugin.getMessage(player, "item.chat_messages.cannot-use.level-too-low", levelReqOpt.get()));
                            return;
                        }
                    }

                    if (isPlaceholderItem(itemInSlot)) {
                        event.setCancelled(true);
                        clickedInventory.setItem(clickedSlot, itemOnCursor.clone());
                        player.setItemOnCursor(null);
                        schedulePlaceholderRefresh(player);
                    } else {
                        schedulePlaceholderRefresh(player);
                    }
                    return;
                }

                if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
                    ItemStack itemFromHotbar = player.getInventory().getItem(event.getHotbarButton());

                    if (isPlaceholderItem(itemFromHotbar)) {
                        event.setCancelled(true);
                        return;
                    }

                    if (itemFromHotbar == null || itemFromHotbar.getType() == Material.AIR) {
                        if (isPlaceholderItem(itemInSlot)) {
                            event.setCancelled(true);
                            return;
                        }
                        schedulePlaceholderRefresh(player);
                        return;
                    }

                    if (itemFromHotbar.getType() != Material.AIR) {
                        if (!itemModule.isAccessory(itemFromHotbar)) {
                            event.setCancelled(true);
                            player.sendMessage(plugin.getMessage(player, "general.cannot-place-non-accessory-in-slot"));
                            return;
                        }
                        Optional<Integer> levelReqOptHotbar = itemModule.getItemLevelRequirement(itemFromHotbar);
                        if (levelReqOptHotbar.isPresent()) {
                            if (characterAPI.getPlayerLevel(player) < levelReqOptHotbar.get()) {
                                event.setCancelled(true);
                                player.sendMessage(plugin.getMessage(player, "item.chat_messages.cannot-use.level-too-low", levelReqOptHotbar.get()));
                                return;
                            }
                        }

                        if (isPlaceholderItem(itemInSlot)) {
                            event.setCancelled(true);
                            clickedInventory.setItem(clickedSlot, itemFromHotbar.clone());
                            player.getInventory().setItem(event.getHotbarButton(), null);
                            schedulePlaceholderRefresh(player);
                        } else {
                            schedulePlaceholderRefresh(player);
                        }
                    }
                    return;
                }
            }
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack itemBeingMoved = event.getCurrentItem();
            if (itemBeingMoved == null || itemBeingMoved.getType() == Material.AIR) return;

            if (isPlaceholderItem(itemBeingMoved)) {
                event.setCancelled(true);
                return;
            }

            boolean isItemAnAccessoryType = itemModule.isAccessory(itemBeingMoved);
            boolean playerCanEquipAccessory = true;

            if (isItemAnAccessoryType) {
                Optional<Integer> levelReqOpt = itemModule.getItemLevelRequirement(itemBeingMoved);
                if (levelReqOpt.isPresent() && characterAPI.getPlayerLevel(player) < levelReqOpt.get()) {
                    playerCanEquipAccessory = false;
                }
            }

            if (!isItemAnAccessoryType || !playerCanEquipAccessory) {
                if (clickedInventory instanceof PlayerInventory && reservedSlots.contains(event.getSlot())) {
                    schedulePlaceholderRefresh(player);
                } else {
                    event.setCancelled(true);
                    trySmartShiftClickToPlayerInventory(player, event);
                }
            } else {
                schedulePlaceholderRefresh(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (itemModule == null || characterAPI == null || !isAccessorySlotsEnabled()) return;

        Player player = (Player) event.getWhoClicked();
        List<Integer> reservedSlots = getReservedSlots();
        ItemStack draggedItem = event.getOldCursor();

        if (isPlaceholderItem(draggedItem)) {
            event.setCancelled(true);
            return;
        }

        if (draggedItem == null || draggedItem.getType() == Material.AIR) return;

        boolean cancelDrag = false;
        String cancelReasonMessageKey = "";
        Object[] cancelReasonArgs = {};

        if (!itemModule.isAccessory(draggedItem)) {
            cancelDrag = true;
            cancelReasonMessageKey = "general.cannot-drag-non-accessory-to-slot";
        } else {
            Optional<Integer> levelReqOpt = itemModule.getItemLevelRequirement(draggedItem);
            if (levelReqOpt.isPresent() && characterAPI.getPlayerLevel(player) < levelReqOpt.get()) {
                cancelDrag = true;
                cancelReasonMessageKey = "item.chat_messages.cannot-use.level-too-low";
                cancelReasonArgs = new Object[]{levelReqOpt.get()};
            }
        }

        if (cancelDrag) {
            boolean affectsReservedSlot = false;
            for (Integer slotIndex : event.getInventorySlots()) {
                if (event.getInventory() instanceof PlayerInventory) {
                    if (reservedSlots.contains(slotIndex)) {
                        affectsReservedSlot = true;
                        break;
                    }
                }
            }
            if (affectsReservedSlot) {
                event.setCancelled(true);
                if (!cancelReasonMessageKey.isEmpty()) {
                    player.sendMessage(plugin.getMessage(player, cancelReasonMessageKey, cancelReasonArgs));
                }
            }
        }

        boolean dragAffectsAccessorySlot = false;
        for (Integer slotIndex : event.getInventorySlots()) {
            if (event.getInventory() instanceof PlayerInventory && reservedSlots.contains(slotIndex)) {
                dragAffectsAccessorySlot = true;
                break;
            }
        }
        if (dragAffectsAccessorySlot && !event.isCancelled()) {
            schedulePlaceholderRefresh(player);
        }
    }

    private void trySmartShiftClickToPlayerInventory(Player player, InventoryClickEvent event) {
        ItemStack itemToMove = event.getCurrentItem();
        if (itemToMove == null || itemToMove.getType() == Material.AIR) return;

        PlayerInventory playerInv = player.getInventory();
        List<Integer> reservedAccessorySlots = getReservedSlots();

        if (tryMoveToArmorSlot(player, itemToMove, event.getClickedInventory(), event.getSlot())) {
            player.updateInventory();
            schedulePlaceholderRefresh(player);
            return;
        }

        for (int i = 0; i <= 35; i++) {
            if (i >= 9 && reservedAccessorySlots.contains(i)) continue;

            ItemStack slotItem = playerInv.getItem(i);
            if (slotItem != null && slotItem.isSimilar(itemToMove) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                int canAdd = slotItem.getMaxStackSize() - slotItem.getAmount();
                int adding = Math.min(canAdd, itemToMove.getAmount());
                slotItem.setAmount(slotItem.getAmount() + adding);
                itemToMove.setAmount(itemToMove.getAmount() - adding);
                if (itemToMove.getAmount() <= 0) {
                    event.getClickedInventory().setItem(event.getSlot(), null);
                    player.updateInventory();
                    schedulePlaceholderRefresh(player);
                    return;
                }
            }
        }

        for (int i = 0; i <= 35; i++) {
            if (i >= 9 && reservedAccessorySlots.contains(i)) continue;
            if (playerInv.getItem(i) == null || playerInv.getItem(i).getType() == Material.AIR) {
                playerInv.setItem(i, itemToMove.clone());
                event.getClickedInventory().setItem(event.getSlot(), null);
                player.updateInventory();
                schedulePlaceholderRefresh(player);
                return;
            }
        }

        player.updateInventory();
        schedulePlaceholderRefresh(player);
    }

    private boolean tryMoveToArmorSlot(Player player, ItemStack itemToMove, Inventory sourceInventory, int sourceSlot) {
        PlayerInventory playerInv = player.getInventory();
        Material itemType = itemToMove.getType();

        if (itemModule.isRPGItem(itemToMove)) {
            Optional<Integer> levelReqOpt = itemModule.getItemLevelRequirement(itemToMove);
            if (levelReqOpt.isPresent() && characterAPI.getPlayerLevel(player) < levelReqOpt.get()) {
                return false;
            }
        }

        if (isHelmet(itemType) && (playerInv.getHelmet() == null || playerInv.getHelmet().getType() == Material.AIR)) {
            playerInv.setHelmet(itemToMove.clone());
            if (sourceInventory != null) sourceInventory.setItem(sourceSlot, null);
            return true;
        } else if (isChestplate(itemType) && (playerInv.getChestplate() == null || playerInv.getChestplate().getType() == Material.AIR)) {
            playerInv.setChestplate(itemToMove.clone());
            if (sourceInventory != null) sourceInventory.setItem(sourceSlot, null);
            return true;
        } else if (isLeggings(itemType) && (playerInv.getLeggings() == null || playerInv.getLeggings().getType() == Material.AIR)) {
            playerInv.setLeggings(itemToMove.clone());
            if (sourceInventory != null) sourceInventory.setItem(sourceSlot, null);
            return true;
        } else if (isBoots(itemType) && (playerInv.getBoots() == null || playerInv.getBoots().getType() == Material.AIR)) {
            playerInv.setBoots(itemToMove.clone());
            if (sourceInventory != null) sourceInventory.setItem(sourceSlot, null);
            return true;
        }
        return false;
    }

    private boolean isHelmet(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.equals("TURTLE_HELMET");
    }

    private boolean isChestplate(Material material) {
        return material.name().endsWith("_CHESTPLATE") || material.name().equals("ELYTRA");
    }

    private boolean isLeggings(Material material) {
        return material.name().endsWith("_LEGGINGS");
    }

    private boolean isBoots(Material material) {
        return material.name().endsWith("_BOOTS");
    }
}