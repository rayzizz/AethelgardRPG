package me.ray.aethelgardRPG.modules.item;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.item.api.ItemAPI;
import me.ray.aethelgardRPG.modules.item.listeners.PlayerItemUseListener;
import me.ray.aethelgardRPG.modules.item.listeners.PlayerPickupItemListener;
import me.ray.aethelgardRPG.modules.item.items.ItemManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

public class ItemModule implements RPGModule, ItemAPI {

    private final AethelgardRPG plugin;
    private ItemManager itemManager;

    public ItemModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Item";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Item...");
        this.itemManager = new ItemManager(plugin, this);
        plugin.getServer().getPluginManager().registerEvents(new PlayerItemUseListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerPickupItemListener(plugin, this), plugin);
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo Item habilitado.");
        itemManager.loadItemConfigurations();
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo Item desabilitado.");
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    @Override
    public boolean isRPGItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(PDCKeys.IS_RPG_ITEM_KEY, PersistentDataType.BYTE);
    }

    @Override
    public Optional<ItemRarity> getItemRarity(ItemStack item) {
        if (!isRPGItem(item)) return Optional.empty();
        String rarityName = item.getItemMeta().getPersistentDataContainer().get(PDCKeys.RARITY_KEY, PersistentDataType.STRING);
        if (rarityName == null) return Optional.empty();
        try {
            return Optional.of(ItemRarity.valueOf(rarityName));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PersistentDataContainer> getItemData(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return Optional.empty();
        }
        return Optional.of(item.getItemMeta().getPersistentDataContainer());
    }

    @Override
    public Optional<ItemStack> getRPGItemById(String id, Player playerContext) {
        return itemManager.getItem(id, playerContext);
    }

    @Override
    public boolean isAccessory(ItemStack item) {
        return itemManager.isAccessory(item);
    }

    @Override
    public List<String> getAccessoryIds() {
        return itemManager.getAccessoryIds();
    }

    @Override
    public Optional<Integer> getItemLevelRequirement(ItemStack item) {
        if (!isRPGItem(item)) return Optional.empty();
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        if (container.has(PDCKeys.LEVEL_REQ_KEY, PersistentDataType.INTEGER)) {
            return Optional.of(container.get(PDCKeys.LEVEL_REQ_KEY, PersistentDataType.INTEGER));
        }
        return Optional.empty();
    }

    @Override
    public List<String> getArmorIds() {
        return itemManager.getArmorIds();
    }

    @Override
    public Optional<ItemStack> identifyItem(ItemStack itemStack, Player playerContext) {
        return itemManager.identifyItem(itemStack, playerContext);
    }

    @Override
    public Optional<ItemStack> getUnidentifiedVersion(String id, Player playerContext) {
        return itemManager.getUnidentifiedVersion(id, playerContext);
    }

    @Override
    public List<String> getAllItemIds(Player playerContext) {
        return itemManager.getAllLoadedItemIds();
    }
}