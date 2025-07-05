package me.ray.aethelgardRPG.modules.item.items;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.configs.LanguageManager;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.item.ItemData;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.ItemRarity;
import me.ray.aethelgardRPG.modules.item.ItemRequirements;
import me.ray.aethelgardRPG.modules.item.ItemStats;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Collectors;

public class ItemManager {

    private final AethelgardRPG plugin;
    private final ItemModule itemModule;

    // Stores loaded ItemData objects
    private final Map<String, ItemData> loadedItemData = new HashMap<>();

    // Cached lists for performance
    private List<String> cachedAccessoryIds;
    private List<String> cachedArmorIds;

    public ItemManager(AethelgardRPG plugin, ItemModule itemModule) {
        this.plugin = plugin;
        this.itemModule = itemModule;
    }

    public void loadItemConfigurations() {
        loadedItemData.clear();
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            if (!itemsFolder.mkdirs()) {
                plugin.getLogger().severe("Could not create 'items' folder. Item loading will be skipped.");
                return;
            }
            plugin.getLogger().info("'items' folder created.");
        }

        extractDefaultItemFiles();
        loadItemsFromFolderRecursive(itemsFolder, itemsFolder);
        plugin.getLogger().info(loadedItemData.size() + " item data templates loaded from configuration files.");

        // Cache accessory and armor IDs after all items are loaded
        this.cachedAccessoryIds = loadedItemData.values().stream()
                .filter(ItemData::isAccessory)
                .map(ItemData::getId)
                .collect(Collectors.toUnmodifiableList());

        this.cachedArmorIds = loadedItemData.values().stream()
                .filter(data -> {
                    Material mat = Material.matchMaterial(data.getMaterial());
                    if (mat == null) return false;
                    String matName = mat.name();
                    return (matName.endsWith("_HELMET") || matName.endsWith("_CHESTPLATE") ||
                            matName.endsWith("_LEGGINGS") || matName.endsWith("_BOOTS")) && !data.isAccessory();
                })
                .map(ItemData::getId)
                .collect(Collectors.toUnmodifiableList());
    }

    private void loadItemsFromFolderRecursive(File folder, File rootItemsFolder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadItemsFromFolderRecursive(file, rootItemsFolder);
            } else if (file.getName().endsWith(".yml")) {
                FileConfiguration itemConfig = YamlConfiguration.loadConfiguration(file);
                try {
                    String relativePath = rootItemsFolder.toURI().relativize(file.toURI()).getPath().replace(".yml", "");
                    String itemIdFromFilePath = relativePath.replace(File.separatorChar, '/');
                    String configId = itemConfig.getString("id", itemIdFromFilePath);
                    String normalizedId = normalizeId(configId);

                    ItemData data = new ItemData(normalizedId);
                    data.setMaterial(itemConfig.getString("material", "STONE"));

                    data.setNameKey(itemConfig.getString("name-key"));
                    data.setName(itemConfig.getString("display-name", "Unknown Item"));
                    data.setLoreKey(itemConfig.getString("lore-key"));
                    data.setLore(itemConfig.getStringList("lore"));

                    data.setCustomModelData(itemConfig.getInt("visual.custom-model-data", 0));
                    data.setRarity(ItemRarity.fromString(itemConfig.getString("rarity", "COMMON")));
                    data.setUnidentified(itemConfig.getBoolean("identified", true));
                    data.setAccessory(itemConfig.getBoolean("is-accessory", false));
                    data.setRPGItem(itemConfig.getBoolean("is-rpg-item", true));

                    ItemRequirements requirements = new ItemRequirements();
                    if (itemConfig.isConfigurationSection("requirements")) {
                        ConfigurationSection reqSection = itemConfig.getConfigurationSection("requirements");
                        requirements.setLevel(reqSection.getInt("level", 0));
                        requirements.setStrength(reqSection.getInt("strength", 0));
                        requirements.setRpgClass(RPGClass.fromString(reqSection.getString("class", "NONE")));
                    }
                    data.setRequirements(requirements);

                    ItemStats stats = new ItemStats();
                    if (itemConfig.isConfigurationSection("stats")) {
                        ConfigurationSection statsSection = itemConfig.getConfigurationSection("stats");
                        stats.setPhysicalDamage(statsSection.getDouble("physical-damage", 0));
                        stats.setMagicalDamage(statsSection.getDouble("magical-damage", 0));
                        stats.setHealthBonus(statsSection.getDouble("health-bonus", 0));
                        stats.setManaBonus(statsSection.getDouble("mana-bonus", 0));
                        stats.setStrengthBonus(statsSection.getDouble("strength-bonus", 0));
                        stats.setPhysicalDefenseBonus(statsSection.getDouble("physical-defense-bonus", 0));
                        stats.setMagicalDefenseBonus(statsSection.getDouble("magical-defense-bonus", 0));
                        stats.setStaminaBonus(statsSection.getDouble("stamina-bonus", 0));
                    }
                    data.setStats(stats);

                    data.setColor(itemConfig.getString("color"));
                    if (itemConfig.isConfigurationSection("armor-trim")) {
                        data.setTrimMaterial(itemConfig.getString("armor-trim.material"));
                        data.setTrimPattern(itemConfig.getString("armor-trim.pattern"));
                    }
                    data.setMaxStackSize(itemConfig.getInt("max-stack-size", -1));
                    data.setEnchantments(itemConfig.getStringList("enchantments"));

                    loadedItemData.put(normalizedId, data);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error loading item data from file " + file.getName(), e);
                }
            }
        }
    }

    /**
     * Retrieves an ItemStack for the given item ID, localized for the player.
     *
     * @param itemId        The ID of the item.
     * @param playerContext The player for whom the item is being built (for language).
     * @return An Optional containing the ItemStack, or empty if not found.
     */
    public Optional<ItemStack> getItem(String itemId, Player playerContext) {
        ItemData data = loadedItemData.get(normalizeId(itemId));
        if (data == null) {
            return Optional.empty();
        }
        return buildItemStackFromData(data, playerContext);
    }

    /**
     * Retrieves an ItemData object for the given ID.
     *
     * @param itemId The ID of the item.
     * @return An Optional containing the ItemData, or empty if not found.
     */
    public Optional<ItemData> getItemData(String itemId) {
        ItemData data = loadedItemData.get(normalizeId(itemId));
        return Optional.ofNullable(data);
    }

    /**
     * Creates an ItemStack from the given ItemData, localized for the player.
     * This is the primary method for constructing RPG items.
     *
     * @param data          The ItemData to build from.
     * @param playerContext The player for whom the item is being built (for language).
     * @return An ItemStack representing the ItemData.
     */
    public ItemStack createItemStack(ItemData data, Player playerContext) {
        Optional<ItemStack> itemStackOpt = buildItemStackFromData(data, playerContext);
        return itemStackOpt.orElseGet(() -> {
            plugin.getLogger().warning("Failed to build ItemStack for " + data.getId() + ", returning a STONE.");
            return new ItemStack(Material.STONE);
        });
    }

    private void extractDefaultItemFiles() {
        String resourceDirPrefix = "items/";
        try {
            CodeSource src = AethelgardRPG.class.getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jarUrl = src.getLocation();
                try (ZipInputStream zip = new ZipInputStream(jarUrl.openStream())) {
                    ZipEntry ze;
                    while ((ze = zip.getNextEntry()) != null) {
                        String entryName = ze.getName();
                        if (entryName.startsWith(resourceDirPrefix) && entryName.endsWith(".yml") && !ze.isDirectory()) {
                            File targetFile = new File(plugin.getDataFolder(), entryName);
                            if (!targetFile.getParentFile().exists()) {
                                targetFile.getParentFile().mkdirs();
                            }
                            if (!targetFile.exists()) {
                                plugin.saveResource(entryName, false);
                                plugin.getLogger().info("Default item config extracted: " + entryName);
                            }
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("Could not get JAR location to scan for default item files.");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error scanning JAR for default item files", e);
        }
    }

    public Optional<ItemStack> buildItemStackFromData(ItemData data, Player playerContext) {
        Material material = Material.matchMaterial(data.getMaterial());
        if (material == null) {
            plugin.getLogger().warning("Invalid material '" + data.getMaterial() + "' for item ID: " + data.getId() + ". Using STONE.");
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("ItemMeta is null for material: " + material.name() + ". Cannot create custom item: " + data.getId());
            if (data.isRPGItem() && material.getMaxStackSize() > 1) {
                item.setAmount(1);
            }
            return Optional.of(item);
        }

        if (data.getMaxStackSize() > 0) {
            meta.setMaxStackSize(data.getMaxStackSize());
        } else if (data.getMaxStackSize() == -1 && data.isRPGItem() && material.getMaxStackSize() > 1) {
            meta.setMaxStackSize(1);
        }

        LanguageManager langManager = plugin.getLanguageManager();

        String displayName;
        if (data.getNameKey() != null && !data.getNameKey().isEmpty()) {
            displayName = langManager.getMessage(playerContext, data.getNameKey());
        } else {
            displayName = ChatColor.translateAlternateColorCodes('&', data.getName());
        }
        meta.setDisplayName(data.getRarity().getDisplayColor() + displayName);

        List<String> finalLore = new ArrayList<>();
        if (data.getLoreKey() != null && !data.getLoreKey().isEmpty()) {
            List<String> translatedBaseLore = langManager.getMessages(playerContext, data.getLoreKey());
            finalLore.addAll(translatedBaseLore);
        } else if (data.getLore() != null && !data.getLore().isEmpty()) {
            data.getLore().forEach(line -> finalLore.add(ChatColor.translateAlternateColorCodes('&', line)));
        }

        if (!finalLore.isEmpty()) finalLore.add("");

        ItemRequirements req = data.getRequirements();
        if (req.getLevel() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.level-req", req.getLevel()));
        if (req.getStrength() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.strength-req", req.getStrength()));
        if (req.getRpgClass() != null && req.getRpgClass() != RPGClass.NONE) {
            finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.class-req", req.getRpgClass().getDisplayName(playerContext)));
        }

        ItemStats stats = data.getStats();
        if (stats.getPhysicalDamage() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.physical-damage", String.format("%.1f", stats.getPhysicalDamage())));
        if (stats.getMagicalDamage() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.magical-damage", String.format("%.1f", stats.getMagicalDamage())));
        if (stats.getHealthBonus() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.health-bonus", String.format("%.1f", stats.getHealthBonus())));
        if (stats.getManaBonus() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.mana-bonus", String.format("%.1f", stats.getManaBonus())));
        if (stats.getStrengthBonus() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.strength-bonus", String.format("%.1f", stats.getStrengthBonus())));
        if (stats.getPhysicalDefenseBonus() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.physical-defense-bonus", String.format("%.1f", stats.getPhysicalDefenseBonus())));
        if (stats.getMagicalDefenseBonus() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.magical-defense-bonus", String.format("%.1f", stats.getMagicalDefenseBonus())));
        if (stats.getStaminaBonus() > 0) finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.stamina-bonus", String.format("%.1f", stats.getStaminaBonus())));

        boolean hasRequirementsOrStats = req.getLevel() > 0 || req.getStrength() > 0 || (req.getRpgClass() != null && req.getRpgClass() != RPGClass.NONE) || stats.hasAnyStat();
        if (!finalLore.isEmpty() && finalLore.get(finalLore.size()-1).equals("") && hasRequirementsOrStats) {
            // If last line is already a separator, don't add another one unless it's just after base lore
        } else if (hasRequirementsOrStats) {
            finalLore.add("");
        }

        if (data.getEnchantments() != null) {
            for (String enchString : data.getEnchantments()) {
                String[] parts = enchString.split(":");
                if (parts.length == 2) {
                    Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                    if (ench != null) {
                        try {
                            int level = Integer.parseInt(parts[1]);
                            meta.addEnchant(ench, level, true);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid enchantment level for " + parts[0] + " on item " + data.getId());
                        }
                    } else {
                        plugin.getLogger().warning("Unknown enchantment " + parts[0] + " for item " + data.getId());
                    }
                }
            }
        }

        if (!data.isUnidentified()) {
            finalLore.add(data.getRarity().getTranslatedDisplayName(playerContext));
        } else {
            finalLore.add(langManager.getMessage(playerContext, "item.lore_formats.unidentified"));
        }

        meta.setLore(finalLore);
        if (data.getCustomModelData() > 0) {
            meta.setCustomModelData(data.getCustomModelData());
        }

        if (meta instanceof LeatherArmorMeta && data.getColor() != null && !data.getColor().isEmpty()) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
            try {
                leatherMeta.setColor(parseColor(data.getColor()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid color string '" + data.getColor() + "' for item " + data.getId() + ". Error: " + e.getMessage());
            }
        }

        if (meta instanceof ArmorMeta && data.getTrimMaterial() != null && !data.getTrimMaterial().isEmpty() &&
                data.getTrimPattern() != null && !data.getTrimPattern().isEmpty()) {
            ArmorMeta armorMeta = (ArmorMeta) meta;
            TrimMaterial bukkitTrimMaterial = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(data.getTrimMaterial().toLowerCase()));
            TrimPattern bukkitTrimPattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(data.getTrimPattern().toLowerCase()));

            if (bukkitTrimMaterial != null && bukkitTrimPattern != null) {
                armorMeta.setTrim(new ArmorTrim(bukkitTrimMaterial, bukkitTrimPattern));
            } else {
                if (bukkitTrimMaterial == null) plugin.getLogger().warning("Invalid Armor Trim Material '" + data.getTrimMaterial() + "' for item " + data.getId());
                if (bukkitTrimPattern == null) plugin.getLogger().warning("Invalid Armor Trim Pattern '" + data.getTrimPattern() + "' for item " + data.getId());
            }
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(PDCKeys.ITEM_DEFINITION_ID_KEY, PersistentDataType.STRING, data.getId());
        container.set(PDCKeys.IS_RPG_ITEM_KEY, PersistentDataType.BYTE, (byte) (data.isRPGItem() ? 1 : 0));
        container.set(PDCKeys.RARITY_KEY, PersistentDataType.STRING, data.getRarity().name());
        if (data.isAccessory()) {
            container.set(PDCKeys.IS_ACCESSORY_KEY, PersistentDataType.BYTE, (byte) 1);
        }
        container.set(PDCKeys.IS_IDENTIFIED_KEY, PersistentDataType.BYTE, (byte) (data.isUnidentified() ? 0 : 1));

        if (req.getLevel() > 0) container.set(PDCKeys.LEVEL_REQ_KEY, PersistentDataType.INTEGER, req.getLevel());
        if (req.getStrength() > 0) container.set(PDCKeys.STRENGTH_REQ_KEY, PersistentDataType.INTEGER, req.getStrength());
        if (req.getRpgClass() != null && req.getRpgClass() != RPGClass.NONE) {
            container.set(PDCKeys.REQUIRED_CLASS_KEY, PersistentDataType.STRING, req.getRpgClass().name());
        }

        if (stats.getHealthBonus() > 0) container.set(PDCKeys.HEALTH_BONUS_KEY, PersistentDataType.INTEGER, (int)stats.getHealthBonus());
        if (stats.getManaBonus() > 0) container.set(PDCKeys.MANA_BONUS_KEY, PersistentDataType.INTEGER, (int)stats.getManaBonus());
        if (stats.getStrengthBonus() > 0) container.set(PDCKeys.STRENGTH_BONUS_KEY, PersistentDataType.INTEGER, (int)stats.getStrengthBonus());
        if (stats.getPhysicalDamage() > 0) container.set(PDCKeys.PHYSICAL_DAMAGE_KEY, PersistentDataType.INTEGER, (int)stats.getPhysicalDamage());
        if (stats.getMagicalDamage() > 0) container.set(PDCKeys.MAGICAL_DAMAGE_KEY, PersistentDataType.INTEGER, (int)stats.getMagicalDamage());
        if (stats.getPhysicalDefenseBonus() > 0) container.set(PDCKeys.PHYSICAL_DEFENSE_BONUS_KEY, PersistentDataType.INTEGER, (int)stats.getPhysicalDefenseBonus());
        if (stats.getMagicalDefenseBonus() > 0) container.set(PDCKeys.MAGICAL_DEFENSE_BONUS_KEY, PersistentDataType.INTEGER, (int)stats.getMagicalDefenseBonus());
        if (stats.getStaminaBonus() > 0) container.set(PDCKeys.STAMINA_BONUS_KEY, PersistentDataType.INTEGER, (int)stats.getStaminaBonus());

        if (data.isUnidentified() && data.getId() != null) {
            container.set(PDCKeys.ORIGINAL_ID_KEY, PersistentDataType.STRING, data.getId());
        }

        item.setItemMeta(meta);
        return Optional.of(item);
    }

    public Optional<ItemStack> identifyItem(ItemStack unidentifiedItem, Player playerContext) {
        if (unidentifiedItem == null || !unidentifiedItem.hasItemMeta()) return Optional.empty();
        ItemMeta meta = unidentifiedItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(PDCKeys.IS_RPG_ITEM_KEY, PersistentDataType.BYTE) ||
                !container.has(PDCKeys.IS_IDENTIFIED_KEY, PersistentDataType.BYTE) ||
                container.get(PDCKeys.IS_IDENTIFIED_KEY, PersistentDataType.BYTE) == 1) {
            return Optional.of(unidentifiedItem);
        }

        String originalId = container.get(PDCKeys.ORIGINAL_ID_KEY, PersistentDataType.STRING);
        if (originalId == null) {
            originalId = container.get(PDCKeys.ITEM_DEFINITION_ID_KEY, PersistentDataType.STRING);
        }
        if (originalId == null) {
            plugin.getLogger().warning("Cannot identify item, original ID or definition ID NBT tag is missing.");
            return Optional.empty();
        }

        Optional<ItemData> originalDataOpt = getItemData(originalId);
        if (originalDataOpt.isEmpty()) {
            plugin.getLogger().warning("Cannot identify item, no ItemData found for original ID: " + originalId);
            return Optional.empty();
        }

        ItemData identifiedData = originalDataOpt.get().clone();
        identifiedData.setUnidentified(false);

        return buildItemStackFromData(identifiedData, playerContext);
    }

    public Optional<ItemStack> getUnidentifiedVersion(String itemId, Player playerContext) {
        Optional<ItemData> originalDataOpt = getItemData(normalizeId(itemId));
        if (originalDataOpt.isEmpty()) {
            return Optional.empty();
        }
        ItemData unidentifiedData = originalDataOpt.get().clone();
        unidentifiedData.setUnidentified(true);

        return buildItemStackFromData(unidentifiedData, playerContext);
    }

    public boolean isAccessory(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(PDCKeys.IS_ACCESSORY_KEY, PersistentDataType.BYTE);
    }

    public boolean isRPGItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        return meta.getPersistentDataContainer().has(PDCKeys.IS_RPG_ITEM_KEY, PersistentDataType.BYTE);
    }

    public String getItemId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(PDCKeys.ITEM_DEFINITION_ID_KEY, PersistentDataType.STRING)) {
            return null;
        }
        return meta.getPersistentDataContainer().get(PDCKeys.ITEM_DEFINITION_ID_KEY, PersistentDataType.STRING);
    }

    public List<String> getAccessoryIds() {
        return cachedAccessoryIds;
    }

    public List<String> getArmorIds() {
        return cachedArmorIds;
    }

    public List<String> getAllLoadedItemIds() {
        return new ArrayList<>(loadedItemData.keySet());
    }

    private String normalizeId(String id) {
        if (id == null) return null;
        return id.toLowerCase().replace("\\", "/");
    }

    private Color parseColor(String colorString) throws IllegalArgumentException {
        if (colorString == null || colorString.trim().isEmpty()) {
            throw new IllegalArgumentException("Color string cannot be null or empty.");
        }
        colorString = colorString.trim();

        if (colorString.startsWith("#")) {
            if (colorString.length() == 7) {
                return Color.fromRGB(
                        Integer.parseInt(colorString.substring(1, 3), 16),
                        Integer.parseInt(colorString.substring(3, 5), 16),
                        Integer.parseInt(colorString.substring(5, 7), 16)
                );
            } else {
                throw new IllegalArgumentException("Invalid hex color format. Use #RRGGBB (e.g., #FF0000).");
            }
        } else {
            String[] parts = colorString.split(",");
            if (parts.length == 3) {
                try {
                    return Color.fromRGB(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim())
                    );
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid RGB color values. Must be integers.", e);
                }
            } else {
                throw new IllegalArgumentException("Invalid RGB color format. Use 'R,G,B' (e.g., '255,0,0').");
            }
        }
    }
}