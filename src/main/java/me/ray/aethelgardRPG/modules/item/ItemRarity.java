package me.ray.aethelgardRPG.modules.item;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.configs.LanguageManager;

public enum ItemRarity {
    COMMON(ChatColor.WHITE, "Comum", "item.rarity.common"),
    UNCOMMON(ChatColor.GREEN, "Incomum", "item.rarity.uncommon"),
    RARE(ChatColor.BLUE, "Raro", "item.rarity.rare"),
    EPIC(ChatColor.DARK_PURPLE, "Épico", "item.rarity.epic"),
    LEGENDARY(ChatColor.GOLD, "Lendário", "item.rarity.legendary"),
    MYTHIC(ChatColor.LIGHT_PURPLE, "Mítico", "item.rarity.mythic"),
    ARTIFACT(ChatColor.RED, "Artefato", "item.rarity.artifact");


    private final String displayName;
    private final ChatColor displayColor;
    private final String translationKey;

    ItemRarity(ChatColor displayColor, String defaultDisplayName, String translationKey) {
        this.displayName = defaultDisplayName;
        this.displayColor = displayColor;
        this.translationKey = translationKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTranslatedDisplayName(Player playerContext) {
        LanguageManager langManager = AethelgardRPG.getInstance().getLanguageManager();
        if (playerContext == null || langManager == null) {
            return getDisplayColor() + getDisplayName();
        }
        String translatedName = langManager.getMessage(playerContext, translationKey);
        return getDisplayColor() + translatedName;
    }

    public static ItemRarity fromString(String rarityString) {
        if (rarityString == null) return COMMON;
        try {
            return ItemRarity.valueOf(rarityString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }

    public ChatColor getDisplayColor() {
        return displayColor;
    }
}