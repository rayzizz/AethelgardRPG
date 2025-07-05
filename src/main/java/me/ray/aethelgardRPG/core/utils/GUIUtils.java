package me.ray.aethelgardRPG.core.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GUIUtils {

    /**
     * Cria um ItemStack básico para ser usado em uma GUI.
     *
     * @param material O material do item.
     * @param displayName O nome de exibição do item (cores com '&' serão traduzidas).
     * @param lore As linhas da lore do item (cores com '&' serão traduzidas).
     * @return O ItemStack criado.
     */
    public static ItemStack createGuiItem(Material material, String displayName, String... lore) {
        return new Builder(material)
                .withName(displayName)
                .withLore(lore)
                .build();
    }

    public static ItemStack createGuiItem(Material material, String displayName, List<String> lore) {
        return new Builder(material)
                .withName(displayName)
                .withLore(lore)
                .build();
    }

    /**
     * Cria uma cabeça de jogador para ser usada em uma GUI.
     *
     * @param owner O jogador (OfflinePlayer) dono da cabeça.
     * @param displayName O nome de exibição do item.
     * @param lore As linhas da lore.
     * @return O ItemStack da cabeça do jogador.
     */
    public static ItemStack createPlayerHead(OfflinePlayer owner, String displayName, String... lore) {
        return new Builder(Material.PLAYER_HEAD)
                .withName(displayName)
                .withLore(lore)
                .withSkullOwner(owner)
                .build();
    }

    public static class Builder {
        private final ItemStack item;
        ItemMeta meta;

        public Builder(Material material) {
            this(material, 1);
        }

        public Builder(Material material, int amount) {
            this.item = new ItemStack(material, amount);
            this.meta = item.getItemMeta();
        }

        public Builder(ItemStack itemStack) {
            this.item = itemStack.clone(); // Trabalhar com uma cópia
            this.meta = item.getItemMeta();
        }

        public Builder withName(String name) {
            if (meta != null && name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            return this;
        }

        public Builder withLore(List<String> lore) {
            if (meta != null && lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList()));
            }
            return this;
        }

        public Builder withLore(String... lore) {
            return withLore(Arrays.asList(lore));
        }

        public Builder withCustomModelData(int data) {
            if (meta != null) {
                meta.setCustomModelData(data);
            }
            return this;
        }

        public Builder withItemFlags(ItemFlag... flags) {
            if (meta != null && flags != null) {
                meta.addItemFlags(flags);
            }
            return this;
        }

        public Builder hideAllAttributes() {
            return withItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                    ItemFlag.HIDE_DYE);
        }

        public Builder setUnbreakable(boolean unbreakable) {
            if (meta != null) {
                meta.setUnbreakable(unbreakable);
            }
            return this;
        }

        public Builder withEnchantment(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
            if (meta != null && enchantment != null) {
                meta.addEnchant(enchantment, level, ignoreLevelRestriction);
            }
            return this;
        }

        public Builder withGlow() {
            if (meta != null) {
                meta.addEnchant(Enchantment.LOYALTY, 1, true); // Encantamento comum para brilho
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Esconde o encantamento "Sorte"
            }
            return this;
        }

        public Builder withSkullOwner(OfflinePlayer owner) {
            if (meta instanceof SkullMeta && owner != null) {
                ((SkullMeta) meta).setOwningPlayer(owner);
            }
            return this;
        }

        public ItemStack build() {
            item.setItemMeta(meta);
            return item;
        }
    }
}