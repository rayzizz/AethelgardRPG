package me.ray.aethelgardRPG.modules.character.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.leveling.LevelingManager;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import me.ray.aethelgardRPG.modules.skill.PlayerSkillProgress;
import me.ray.aethelgardRPG.modules.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayerProfileGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final Player player; // O jogador para quem esta GUI foi aberta
    private Inventory inv;

    // Definições de Slot
    private static final int PLAYER_INFO_SLOT = 4;
    private static final int COMBAT_INFO_SLOT = 13;
    private static final int PROFESSIONS_SLOT = 31;
    private static final int CRYSTAL_SKILL_SLOT = 40;
    private static final int STRENGTH_ATTRIBUTE_SLOT = 20;
    private static final int INTELLIGENCE_ATTRIBUTE_SLOT = 21;
    private static final int FAITH_ATTRIBUTE_SLOT = 22;
    private static final int DEXTERITY_ATTRIBUTE_SLOT = 23;
    private static final int AGILITY_ATTRIBUTE_SLOT = 24;
    private static final int TOME_SLOT = 49; // Exemplo: Canto inferior direito

    // Mapa para controlar o tempo do último clique de cada jogador
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private static final long CLICK_COOLDOWN = 250; // 0.25 segundos

    // *** ALTERAÇÃO NO CONSTRUTOR ***
    public PlayerProfileGUI(AethelgardRPG plugin, CharacterModule characterModule, Player player) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.player = player;
    }

    // *** ALTERAÇÃO NO MÉTODO OPEN ***
    public void open() {
        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
        if (playerDataOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "character.gui.profile.no-data", player.getName()));
            return;
        }

        // Garante que o listener seja registrado apenas uma vez por abertura
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        inv = Bukkit.createInventory(this, 54, plugin.getMessage(player, "character.gui.profile.title", player.getName()));
        initializeItems(playerDataOpt.get());
        player.openInventory(inv);
    }

    private void initializeItems(CharacterData data) {
        inv.clear();

        // Preenche o fundo com painéis de vidro
        ItemStack background = GUIUtils.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }

        PlayerAttributes attributes = data.getAttributes();
        LevelingManager levelingManager = characterModule.getLevelingManager();
        double xpForNext = levelingManager.getXpForNextLevel(data.getLevel());
        String xpDisplay = (data.getLevel() >= levelingManager.getMaxLevel()) ? "MAX" : String.format("%.0f", xpForNext);

        // Informações do Jogador
        inv.setItem(PLAYER_INFO_SLOT, GUIUtils.createPlayerHead(player,
                plugin.getMessage(player, "character.gui.profile.item.player-info.name", player.getName()),
                plugin.getMessage(player, "character.gui.profile.item.player-info.lore.class", data.getSelectedClass().getDisplayName(player)),
                plugin.getMessage(player, "character.gui.profile.item.player-info.lore.level", data.getLevel()),
                plugin.getMessage(player, "character.gui.profile.item.player-info.lore.experience", String.format("%.0f", data.getExperience()), xpDisplay),
                plugin.getMessage(player, "character.gui.profile.item.player-info.lore.money", String.format("%.2f", data.getMoney())),
                plugin.getMessage(player, "character.gui.profile.item.player-info.lore.attribute-points", data.getAttributePoints())
        ));

        // Atributos Primários
        String clickToIncrease = plugin.getMessage(player, "character.gui.profile.item.attribute.lore.click-to-increase");
        inv.setItem(STRENGTH_ATTRIBUTE_SLOT, createAttributeItem(Material.REDSTONE, "strength", attributes.getStrength(), clickToIncrease));
        inv.setItem(INTELLIGENCE_ATTRIBUTE_SLOT, createAttributeItem(Material.LAPIS_LAZULI, "intelligence", attributes.getIntelligence(), clickToIncrease));
        inv.setItem(FAITH_ATTRIBUTE_SLOT, createAttributeItem(Material.GLOWSTONE_DUST, "faith", attributes.getFaith(), clickToIncrease));
        inv.setItem(DEXTERITY_ATTRIBUTE_SLOT, createAttributeItem(Material.EMERALD, "dexterity", attributes.getDexterity(), clickToIncrease));
        inv.setItem(AGILITY_ATTRIBUTE_SLOT, createAttributeItem(Material.FEATHER, "agility", attributes.getAgility(), clickToIncrease));

        // Informações de Combate
        inv.setItem(COMBAT_INFO_SLOT, GUIUtils.createGuiItem(Material.IRON_SWORD,
                plugin.getMessage(player, "character.gui.profile.item.combat-info.name"),
                plugin.getMessage(player, "character.gui.profile.item.combat-info.lore.health", String.format("%.1f", attributes.getCurrentHealth()), String.format("%.1f", attributes.getMaxHealth())),
                plugin.getMessage(player, "character.gui.profile.item.combat-info.lore.mana", String.format("%.1f", attributes.getCurrentMana()), String.format("%.1f", attributes.getMaxMana())),
                plugin.getMessage(player, "character.gui.profile.item.combat-info.lore.stamina", String.format("%.1f", attributes.getCurrentStamina()), String.format("%.1f", attributes.getMaxStamina())),
                "",
                plugin.getMessage(player, "character.gui.profile.item.combat-info.lore.physical-defense", String.format("%.1f", attributes.getPhysicalDefense())),
                plugin.getMessage(player, "character.gui.profile.item.combat-info.lore.magical-defense", String.format("%.1f", attributes.getMagicalDefense()))
        ));

        // Profissões
        List<String> skillLore = new ArrayList<>();
        skillLore.add(plugin.getMessage(player, "character.gui.profile.item.professions.lore.header"));
        for (Map.Entry<SkillType, PlayerSkillProgress> entry : data.getSkillProgressMap().entrySet()) {
            skillLore.add(plugin.getMessage(player, "character.gui.profile.item.professions.lore.entry",
                    entry.getKey().getDisplayName(player),
                    entry.getValue().getLevel(),
                    String.format("%.0f", entry.getValue().getExperience())
            ));
        }
        inv.setItem(PROFESSIONS_SLOT, GUIUtils.createGuiItem(Material.DIAMOND_PICKAXE,
                plugin.getMessage(player, "character.gui.profile.item.professions.name"),
                skillLore
        ));

        // Outros botões...
        inv.setItem(CRYSTAL_SKILL_SLOT, GUIUtils.createGuiItem(Material.NETHER_STAR,
                plugin.getMessage(player, "character.gui.profile.item.crystal-skill.name"),
                plugin.getMessage(player, "character.gui.profile.item.crystal-skill.lore.description"),
                plugin.getMessage(player, "character.gui.profile.item.crystal-skill.lore.right-click-reset")
        ));

        inv.setItem(TOME_SLOT, GUIUtils.createGuiItem(Material.WRITABLE_BOOK,
                plugin.getMessage(player, "character.gui.profile.item.tome.name"),
                plugin.getMessage(player, "character.gui.profile.item.tome.lore")
        ));
    }

    private ItemStack createAttributeItem(Material material, String attributeKey, int value, String clickToIncrease) {
        String name = plugin.getMessage(player, "character.gui.profile.item.attribute." + attributeKey + ".name");
        String loreValue = plugin.getMessage(player, "character.gui.profile.item.attribute." + attributeKey + ".lore.value", value);
        return GUIUtils.createGuiItem(material, name, loreValue, "", clickToIncrease);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inv;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        // *** CORREÇÃO PRINCIPAL AQUI ***
        // Ignora o evento se o clique não foi no inventário da GUI (o de cima).
        if (!Objects.equals(event.getClickedInventory(), inv)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player p)) return;

        // Cooldown para evitar cliques duplos
        long now = System.currentTimeMillis();
        if (now - lastClickTime.getOrDefault(p.getUniqueId(), 0L) < CLICK_COOLDOWN) {
            return;
        }
        lastClickTime.put(p.getUniqueId(), now);

        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(p);
        if (playerDataOpt.isEmpty()) return;

        CharacterData characterData = playerDataOpt.get();
        PlayerAttributes attributes = characterData.getAttributes();
        int clickedSlot = event.getSlot();

        // Lógica para o Tomo de Conhecimento (Grimório)
        if (clickedSlot == TOME_SLOT) {
            p.performCommand("spellbook");
            return;
        }

        // Lógica para o Cristal de Reset
        if (clickedSlot == CRYSTAL_SKILL_SLOT && event.getClick() == ClickType.RIGHT) {
            new AttributeResetConfirmationGUI(plugin, characterModule, this, p).open();
            return;
        }

        // Lógica para adicionar pontos de atributo
        if (isAttributeSlot(clickedSlot)) {
            if (characterData.getAttributePoints() <= 0) {
                p.sendMessage(plugin.getMessage(p, "character.gui.profile.no-attribute-points"));
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
                return;
            }

            String attributeName = "";
            Runnable updateAttribute = null;

            if (clickedSlot == STRENGTH_ATTRIBUTE_SLOT) {
                attributeName = plugin.getMessage(p, "character.gui.profile.item.attribute.strength.name-raw");
                updateAttribute = () -> attributes.setBaseStrength(attributes.getBaseStrength() + 1);
            } else if (clickedSlot == INTELLIGENCE_ATTRIBUTE_SLOT) {
                attributeName = plugin.getMessage(p, "character.gui.profile.item.attribute.intelligence.name-raw");
                updateAttribute = () -> attributes.setBaseIntelligence(attributes.getBaseIntelligence() + 1);
            } else if (clickedSlot == FAITH_ATTRIBUTE_SLOT) {
                attributeName = plugin.getMessage(p, "character.gui.profile.item.attribute.faith.name-raw");
                updateAttribute = () -> attributes.setBaseFaith(attributes.getBaseFaith() + 1);
            } else if (clickedSlot == DEXTERITY_ATTRIBUTE_SLOT) {
                attributeName = plugin.getMessage(p, "character.gui.profile.item.attribute.dexterity.name-raw");
                updateAttribute = () -> attributes.setBaseDexterity(attributes.getBaseDexterity() + 1);
            } else if (clickedSlot == AGILITY_ATTRIBUTE_SLOT) {
                attributeName = plugin.getMessage(p, "character.gui.profile.item.attribute.agility.name-raw");
                updateAttribute = () -> attributes.setBaseAgility(attributes.getBaseAgility() + 1);
            }

            if (updateAttribute != null) {
                characterData.setAttributePoints(characterData.getAttributePoints() - 1);
                updateAttribute.run();
                attributes.recalculateStats(); // Recalcula vida/mana/etc.
                p.sendMessage(plugin.getMessage(p, "character.gui.profile.attribute-increased", attributeName));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

                // Atualiza a GUI com os novos valores
                initializeItems(characterData);
            }
        }
    }

    private boolean isAttributeSlot(int slot) {
        return slot >= STRENGTH_ATTRIBUTE_SLOT && slot <= AGILITY_ATTRIBUTE_SLOT;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            lastClickTime.remove(event.getPlayer().getUniqueId());
            HandlerList.unregisterAll(this);
        }
    }
}