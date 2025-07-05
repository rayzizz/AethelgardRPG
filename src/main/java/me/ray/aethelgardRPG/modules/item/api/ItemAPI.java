package me.ray.aethelgardRPG.modules.item.api;

import me.ray.aethelgardRPG.modules.item.ItemRarity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;
import java.util.Optional;

public interface ItemAPI {

    boolean isRPGItem(ItemStack item);

    Optional<ItemRarity> getItemRarity(ItemStack item);

    Optional<PersistentDataContainer> getItemData(ItemStack item);

    /**
     * Obtém uma instância de um item RPG definido em configuração pelo seu ID.
     * @param itemId O ID do item (geralmente o nome do arquivo .yml).
     * @return Um Optional contendo o ItemStack se encontrado, caso contrário Optional.empty().
     */
    Optional<ItemStack> getRPGItemById(String id, Player playerContext);

    /**
     * Checks if an ItemStack is an accessory.
     * @param item The ItemStack to check.
     * @return true if it's an accessory, false otherwise.
     */
    boolean isAccessory(ItemStack item);

    List<String> getAccessoryIds();

    Optional<Integer> getItemLevelRequirement(ItemStack item);

    List<String> getArmorIds();

    Optional<ItemStack> identifyItem(ItemStack itemStack, Player playerContext);

    Optional<ItemStack> getUnidentifiedVersion(String id, Player playerContext);

    List<String> getAllItemIds(Player playerContext);
}
