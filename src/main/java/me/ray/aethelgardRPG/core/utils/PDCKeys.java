package me.ray.aethelgardRPG.core.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Classe utilitária para armazenar e inicializar todas as NamespacedKeys do plugin.
 * Isso centraliza a criação de chaves e evita erros de inicialização estática.
 */
public final class PDCKeys {

    private PDCKeys() {}

    // --- Item Keys ---
    public static NamespacedKey IS_RPG_ITEM_KEY;
    public static NamespacedKey ITEM_DEFINITION_ID_KEY;
    public static NamespacedKey ORIGINAL_ID_KEY;
    public static NamespacedKey RARITY_KEY;
    public static NamespacedKey IS_IDENTIFIED_KEY;
    public static NamespacedKey IS_ACCESSORY_KEY;

    // --- Item Requirement Keys ---
    public static NamespacedKey LEVEL_REQ_KEY;
    public static NamespacedKey STRENGTH_REQ_KEY;
    public static NamespacedKey REQUIRED_CLASS_KEY;

    // --- Item Stat Keys ---
    public static NamespacedKey HEALTH_BONUS_KEY;
    public static NamespacedKey MANA_BONUS_KEY;
    public static NamespacedKey STAMINA_BONUS_KEY;
    public static NamespacedKey STRENGTH_BONUS_KEY;
    public static NamespacedKey PHYSICAL_DAMAGE_KEY;
    public static NamespacedKey MAGICAL_DAMAGE_KEY;
    public static NamespacedKey PHYSICAL_DEFENSE_BONUS_KEY;
    public static NamespacedKey MAGICAL_DEFENSE_BONUS_KEY;

    // --- Custom Mob & Minion Keys ---
    public static NamespacedKey CUSTOM_MOB_ID_KEY;
    public static NamespacedKey SUMMON_OWNER_KEY;
    public static NamespacedKey SUMMON_OWNER_NAME_KEY;
    public static NamespacedKey CAN_DAMAGE_OWNER_KEY;

    // --- Quest & NPC Keys ---
    public static NamespacedKey NPC_ID_KEY;

    // --- GUI Keys ---
    public static NamespacedKey GUI_SPELL_ID_KEY;

    /**
     * Inicializa todas as chaves estáticas com a instância do plugin.
     * Deve ser chamado no método onLoad() do plugin.
     * @param plugin A instância principal do plugin.
     */
    public static void initialize(Plugin plugin) {
        // Item
        IS_RPG_ITEM_KEY = new NamespacedKey(plugin, "is_rpg_item");
        ITEM_DEFINITION_ID_KEY = new NamespacedKey(plugin, "item_definition_id");
        ORIGINAL_ID_KEY = new NamespacedKey(plugin, "original_item_id");
        RARITY_KEY = new NamespacedKey(plugin, "item_rarity");
        IS_IDENTIFIED_KEY = new NamespacedKey(plugin, "is_identified");
        IS_ACCESSORY_KEY = new NamespacedKey(plugin, "is_accessory");

        // Requirements
        LEVEL_REQ_KEY = new NamespacedKey(plugin, "level_req");
        STRENGTH_REQ_KEY = new NamespacedKey(plugin, "strength_req");
        REQUIRED_CLASS_KEY = new NamespacedKey(plugin, "item_rpg_class_requirement");

        // Stats
        HEALTH_BONUS_KEY = new NamespacedKey(plugin, "health_bonus");
        MANA_BONUS_KEY = new NamespacedKey(plugin, "mana_bonus");
        STAMINA_BONUS_KEY = new NamespacedKey(plugin, "stamina_bonus");
        STRENGTH_BONUS_KEY = new NamespacedKey(plugin, "strength_bonus");
        PHYSICAL_DAMAGE_KEY = new NamespacedKey(plugin, "physical_damage");
        MAGICAL_DAMAGE_KEY = new NamespacedKey(plugin, "magical_damage");
        PHYSICAL_DEFENSE_BONUS_KEY = new NamespacedKey(plugin, "physical_defense_bonus");
        MAGICAL_DEFENSE_BONUS_KEY = new NamespacedKey(plugin, "magical_defense_bonus");

        // Mobs & Minions
        CUSTOM_MOB_ID_KEY = new NamespacedKey(plugin, "custom_mob_id");
        SUMMON_OWNER_KEY = new NamespacedKey(plugin, "summon_owner_uuid");
        SUMMON_OWNER_NAME_KEY = new NamespacedKey(plugin, "summon_owner_name");
        CAN_DAMAGE_OWNER_KEY = new NamespacedKey(plugin, "summon_can_damage_owner");

        // Quests & NPCs
        NPC_ID_KEY = new NamespacedKey(plugin, "rpg_npc_id");

        // GUIs
        GUI_SPELL_ID_KEY = new NamespacedKey(plugin, "gui_spell_id");
    }
}