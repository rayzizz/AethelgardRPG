package me.ray.aethelgardRPG.modules.admin.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.api.AethelgardAPI;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import me.ray.aethelgardRPG.modules.character.leveling.LevelingManager;
import me.ray.aethelgardRPG.modules.skill.PlayerSkillProgress;
import me.ray.aethelgardRPG.modules.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PlayerInspectGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final AethelgardAPI api;
    private final Player targetPlayer;
    private Inventory inv;

    public PlayerInspectGUI(AethelgardRPG plugin, Player targetPlayer) {
        this.plugin = plugin;
        this.api = plugin;
        this.targetPlayer = targetPlayer;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player opener) {
        Optional<CharacterData> characterDataOpt = api.getCharacterData(targetPlayer);
        if (characterDataOpt.isEmpty()) {
            opener.sendMessage(plugin.getMessage(opener, "admin.gui_inspect.no-data", targetPlayer.getName()));
            return;
        }
        CharacterData characterData = characterDataOpt.get();
        PlayerAttributes attributes = characterData.getAttributes();

        inv = Bukkit.createInventory(this, 27, plugin.getMessage(opener, "admin.gui_inspect.title", targetPlayer.getName()));

        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        LevelingManager levelingManager = null;
        if (characterModule != null) {
            levelingManager = characterModule.getLevelingManager();
        }

        double xpForNext = 0;
        if (levelingManager != null) {
            xpForNext = levelingManager.getXpForNextLevel(characterData.getLevel());
        }
        String xpForNextLevelDisplay = (characterData.getLevel() < (levelingManager != null ? levelingManager.getMaxLevel() : Integer.MAX_VALUE)) ? String.format("%.0f", xpForNext) : "MAX";

        // Player Info (Slot 0) - Using a different layout from profile
        inv.setItem(0, GUIUtils.createPlayerHead(targetPlayer,
                plugin.getMessage(opener, "admin.gui_inspect.item_compass.name", targetPlayer.getName()),
                plugin.getMessage(opener, "admin.gui_inspect.item_compass.lore_player", targetPlayer.getName()),
                plugin.getMessage(opener, "admin.gui_inspect.item_compass.lore_class", characterData.getSelectedClass().getDisplayName(opener)),
                plugin.getMessage(opener, "admin.gui_inspect.item_compass.lore_level_xp", characterData.getLevel(), String.format("%.0f", characterData.getExperience()), xpForNextLevelDisplay),
                plugin.getMessage(opener, "admin.gui_inspect.item_compass.lore_money", String.format("%.2f", characterData.getMoney())),
                plugin.getMessage(opener, "admin.gui_inspect.item_compass.lore_attr_points", characterData.getAttributePoints())
        ));

        // Attributes (Slots 2 & 3)
        inv.setItem(2, GUIUtils.createGuiItem(Material.NETHERITE_SWORD,
                plugin.getMessage(opener, "admin.gui_inspect.item.primary_attributes.name"),
                plugin.getMessage(opener, "admin.gui_inspect.item.primary_attributes.lore.strength", attributes.getStrength()),
                plugin.getMessage(opener, "admin.gui_inspect.item.primary_attributes.lore.intelligence", attributes.getIntelligence()),
                plugin.getMessage(opener, "admin.gui_inspect.item.primary_attributes.lore.faith", attributes.getFaith()),
                plugin.getMessage(opener, "admin.gui_inspect.item.primary_attributes.lore.dexterity", attributes.getDexterity()),
                plugin.getMessage(opener, "admin.gui_inspect.item.primary_attributes.lore.agility", attributes.getAgility())
        ));
        inv.setItem(3, GUIUtils.createGuiItem(Material.SHIELD,
                plugin.getMessage(opener, "admin.gui_inspect.item.secondary_attributes.name"),
                plugin.getMessage(opener, "admin.gui_inspect.item.secondary_attributes.lore.health", String.format("%.1f", attributes.getCurrentHealth()), String.format("%.1f", attributes.getMaxHealth())),
                plugin.getMessage(opener, "admin.gui_inspect.item.secondary_attributes.lore.mana", String.format("%.1f", attributes.getCurrentMana()), String.format("%.1f", attributes.getMaxMana()))
        ));

        // Skills (Slot 4)
        List<String> skillLore = new ArrayList<>();
        skillLore.add(plugin.getMessage(opener, "admin.gui_inspect.item.skills.lore.header"));
        for (Map.Entry<SkillType, PlayerSkillProgress> entry : characterData.getSkillProgressMap().entrySet()) {
            skillLore.add(plugin.getMessage(opener, "admin.gui_inspect.item.skills.lore.entry",
                    entry.getKey().getDisplayName(opener),
                    entry.getValue().getLevel(),
                    String.format("%.0f", entry.getValue().getExperience())
            ));
        }
        inv.setItem(4, GUIUtils.createGuiItem(Material.DIAMOND_PICKAXE, plugin.getMessage(opener, "admin.gui_inspect.item.skills.name"), skillLore));

        // TODO: Add Active Quests, Inventory view, etc.

        opener.openInventory(inv);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inv;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
    }
}