package me.ray.aethelgardRPG.modules.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemData implements Cloneable {
    private String id;
    private String material;
    private String name;
    private List<String> lore;
    private String nameKey;
    private String loreKey;
    private int customModelData;
    private ItemRarity rarity;
    private ItemRequirements requirements;
    private ItemStats stats;
    private boolean unidentified;
    private String color;
    private String trimMaterial;
    private String trimPattern;
    private boolean isAccessory;
    private boolean rpgItem;
    private List<String> enchantments;
    private int maxStackSize;
    private Map<String, Object> extraData;

    public ItemData(String id) {
        this.id = id;
        this.rarity = ItemRarity.COMMON;
        this.requirements = new ItemRequirements();
        this.maxStackSize = -1;
        this.stats = new ItemStats();
        this.lore = Collections.emptyList();
        this.rpgItem = true;
        this.enchantments = Collections.emptyList();
        this.isAccessory = false;
        this.extraData = Collections.emptyMap();
    }

    // Getters
    public String getId() { return id; }
    public String getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return lore == null ? Collections.emptyList() : lore; }
    public String getNameKey() { return nameKey; }
    public String getLoreKey() { return loreKey; }
    public int getCustomModelData() { return customModelData; }
    public ItemRarity getRarity() { return rarity; }
    public ItemRequirements getRequirements() { return requirements; }
    public ItemStats getStats() { return stats; }
    public boolean isUnidentified() { return unidentified; }
    public boolean isRPGItem() { return rpgItem; }
    public List<String> getEnchantments() { return enchantments == null ? Collections.emptyList() : enchantments; }
    public String getColor() { return color; }
    public String getTrimMaterial() { return trimMaterial; }
    public String getTrimPattern() { return trimPattern; }
    public boolean isAccessory() { return isAccessory; }
    public Map<String, Object> getExtraData() { return extraData == null ? Collections.emptyMap() : extraData; }
    public int getMaxStackSize() { return maxStackSize; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setMaterial(String material) { this.material = material; }
    public void setName(String name) { this.name = name; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public void setNameKey(String nameKey) { this.nameKey = nameKey; }
    public void setLoreKey(String loreKey) { this.loreKey = loreKey; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
    public void setRarity(ItemRarity rarity) { this.rarity = rarity; }
    public void setRequirements(ItemRequirements requirements) { this.requirements = requirements; }
    public void setStats(ItemStats stats) { this.stats = stats; }
    public void setUnidentified(boolean unidentified) { this.unidentified = unidentified; }
    public void setRPGItem(boolean rpgItem) { this.rpgItem = rpgItem; }
    public void setEnchantments(List<String> enchantments) { this.enchantments = enchantments; }
    public void setColor(String color) { this.color = color; }
    public void setTrimMaterial(String trimMaterial) { this.trimMaterial = trimMaterial; }
    public void setTrimPattern(String trimPattern) { this.trimPattern = trimPattern; }
    public void setAccessory(boolean accessory) { isAccessory = accessory; }
    public void setExtraData(Map<String, Object> extraData) { this.extraData = extraData; }
    public void setMaxStackSize(int maxStackSize) { this.maxStackSize = maxStackSize; }

    @Override
    public ItemData clone() {
        try {
            ItemData cloned = (ItemData) super.clone();
            cloned.requirements = this.requirements != null ? this.requirements.clone() : null;
            cloned.stats = this.stats != null ? this.stats.clone() : null;
            cloned.lore = this.lore != null ? new ArrayList<>(this.lore) : null;
            cloned.enchantments = this.enchantments != null ? new ArrayList<>(this.enchantments) : null;
            cloned.extraData = this.extraData != null ? new HashMap<>(this.extraData) : null;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}