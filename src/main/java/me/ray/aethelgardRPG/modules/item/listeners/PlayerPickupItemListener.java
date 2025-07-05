package me.ray.aethelgardRPG.modules.item.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.item.ItemData;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.items.ItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class PlayerPickupItemListener implements Listener {

    private final AethelgardRPG plugin;
    private final ItemManager itemManager;

    public PlayerPickupItemListener(AethelgardRPG plugin, ItemModule itemModule) {
        this.plugin = plugin;
        this.itemManager = itemModule.getItemManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack pickedUpItemStack = event.getItem().getItemStack();

        if (itemManager == null) {
            plugin.getLogger().warning("ItemManager não está disponível para o evento EntityPickupItemEvent.");
            return;
        }

        if (itemManager.isRPGItem(pickedUpItemStack)) {
            String itemId = itemManager.getItemId(pickedUpItemStack);
            if (itemId == null) {
                plugin.getLogger().finer("Item pego por " + player.getName() + " é RPG mas não tem ID de definição.");
                return;
            }

            Optional<ItemData> itemDataOpt = itemManager.getItemData(itemId);
            if (itemDataOpt.isEmpty()) {
                plugin.getLogger().warning("Não foi possível encontrar ItemData para o ID '" + itemId + "' ao pegar o item para " + player.getName());
                return;
            }

            ItemData itemData = itemDataOpt.get();
            int amount = pickedUpItemStack.getAmount();

            ItemStack updatedItemStack = itemManager.createItemStack(itemData, player);
            updatedItemStack.setAmount(amount);

            event.getItem().setItemStack(updatedItemStack);
        }
    }
}