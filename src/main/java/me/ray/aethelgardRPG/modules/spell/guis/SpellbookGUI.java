package me.ray.aethelgardRPG.modules.spell.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.spell.PlayerSpellManager;
import me.ray.aethelgardRPG.modules.spell.SpellModule;
import me.ray.aethelgardRPG.modules.spell.combo.ClickType;
import me.ray.aethelgardRPG.modules.spell.combo.Combo;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.UUID;

public class SpellbookGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final SpellModule spellModule;
    private final PlayerSpellManager playerSpellManager;
    private Inventory inv;

    // Cooldown para cliques na GUI para prevenir double-clicks
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private static final long CLICK_COOLDOWN_MS = 300; // 300ms de cooldown

    public enum Mode { NORMAL, ASSIGNING, UNASSIGNING }
    private Mode currentMode = Mode.NORMAL;
    private String spellIdToAssign = null;

    private final Map<Integer, Combo> comboSlots = new HashMap<>();

    private static final List<Combo> AVAILABLE_COMBOS = List.of(
            new Combo(ClickType.LEFT, ClickType.LEFT, ClickType.RIGHT),
            new Combo(ClickType.LEFT, ClickType.RIGHT, ClickType.LEFT),
            new Combo(ClickType.RIGHT, ClickType.LEFT, ClickType.RIGHT),
            new Combo(ClickType.RIGHT, ClickType.RIGHT, ClickType.LEFT),
            new Combo(ClickType.LEFT, ClickType.RIGHT, ClickType.RIGHT),
            new Combo(ClickType.RIGHT, ClickType.LEFT, ClickType.LEFT),
            new Combo(ClickType.LEFT, ClickType.LEFT, ClickType.LEFT),
            new Combo(ClickType.RIGHT, ClickType.RIGHT, ClickType.RIGHT)
    );
    private static final int[] COMBO_DISPLAY_SLOTS = {1, 3, 5, 7, 10, 12, 14, 16};

    public SpellbookGUI(AethelgardRPG plugin, SpellModule spellModule) {
        this.plugin = plugin;
        this.spellModule = spellModule;
        this.playerSpellManager = spellModule.getPlayerSpellManager();
    }

    /**
     * Abre a GUI do grimório para o jogador.
     * Este método deve ser chamado APENAS para a abertura inicial da GUI.
     * Mudanças de modo internas devem usar changeModeAndRefresh().
     * @param player O jogador que abrirá a GUI.
     */
    public void open(Player player) {
        // Garante que qualquer listener antigo para esta instância seja removido
        // antes de registrar um novo. Isso previne o acúmulo de listeners.
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isEmpty() || playerDataOpt.get().getSelectedClass() == RPGClass.NONE) {
            player.sendMessage(plugin.getMessage(player, "spell.gui.no_class"));
            HandlerList.unregisterAll(this); // Garante a limpeza mesmo se a abertura falhar
            return;
        }

        String title = plugin.getMessage(player, "spell.gui.title");
        if (this.inv == null) { // Apenas cria o inventário uma vez
            this.inv = Bukkit.createInventory(this, 36, title);
        }

        // Define o modo inicial e atualiza os itens
        this.currentMode = Mode.NORMAL;
        this.spellIdToAssign = null;
        initializeItems(player, playerDataOpt.get());

        // Abre o inventário para o jogador
        player.openInventory(this.inv);
    }

    /**
     * Altera o modo da GUI e atualiza seu conteúdo sem fechar e reabrir o inventário.
     * @param player O jogador.
     * @param newMode O novo modo da GUI.
     * @param newSpellId O ID da magia a ser atribuída (se o modo for ASSIGNING), ou null.
     */
    public void changeModeAndRefresh(Player player, Mode newMode, @Nullable String newSpellId) {
        this.currentMode = newMode;
        this.spellIdToAssign = newSpellId;

        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "spell.error.player_data_missing"));
            player.closeInventory(); // Fecha se os dados estiverem faltando
            return;
        }

        initializeItems(player, playerDataOpt.get());
        player.updateInventory(); // Envia o conteúdo atualizado para o cliente

        // Envia mensagens específicas do modo
        if (newMode == Mode.ASSIGNING) {
            player.sendMessage(plugin.getMessage(player, "spell.gui.assign.prompt"));
        } else if (newMode == Mode.UNASSIGNING) {
            player.sendMessage(plugin.getMessage(player, "spell.gui.assign.prompt_unassign"));
        }
    }

    /**
     * Abre a GUI do grimório diretamente no modo de atribuição.
     * Ideal para ser chamado de outras GUIs, como a de magias conhecidas.
     * @param player O jogador.
     * @param spellId O ID da magia que será atribuída.
     */
    public void openForAssigning(Player player, @NotNull String spellId) {
        // Garante que os listeners estão corretamente registrados para esta instância.
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "spell.error.player_data_missing"));
            return; // Não abre a GUI se os dados do jogador não existirem.
        }

        // Define o modo e a magia para atribuição
        this.currentMode = Mode.ASSIGNING;
        this.spellIdToAssign = spellId;

        // Cria o inventário se ainda não existir
        if (this.inv == null) {
            String title = plugin.getMessage(player, "spell.gui.title");
            this.inv = Bukkit.createInventory(this, 36, title);
        }

        // Prepara os itens da GUI com o modo de atribuição
        initializeItems(player, playerDataOpt.get());

        // Envia a mensagem de instrução para o jogador
        player.sendMessage(plugin.getMessage(player, "spell.gui.assign.prompt"));

        // Abre a GUI para o jogador
        player.openInventory(this.inv);
    }


    private void initializeItems(Player player, CharacterData characterData) {
        inv.clear();
        comboSlots.clear();

        ItemStack background = GUIUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, background);

        // Botão: View Known Spells (slot 27)
        inv.setItem(27, GUIUtils.createGuiItem(Material.WRITABLE_BOOK,
                plugin.getMessage(player, "spell.gui.buttons.view_all_spells.name"),
                plugin.getMessages(player, "spell.gui.buttons.view_all_spells.lore")));

        // Botão: Unassign a Spell (slot 31) - Ativado por clique DIREITO
        inv.setItem(31, GUIUtils.createGuiItem(Material.GRINDSTONE,
                plugin.getMessage(player, "spell.gui.buttons.unassign_spell.name"),
                plugin.getMessages(player, "spell.gui.buttons.unassign_spell.lore_right_click"))); // Nova chave de lore

        // Botão: Cancel Action (slot 35) - Agora é o botão de SAIR
        inv.setItem(35, GUIUtils.createGuiItem(Material.BARRIER,
                plugin.getMessage(player, "spell.gui.buttons.exit_gui.name"), // Nova chave de lore
                plugin.getMessages(player, "spell.gui.buttons.exit_gui.lore"))); // Nova chave de lore

        for (int i = 0; i < AVAILABLE_COMBOS.size(); i++) {
            if (i >= COMBO_DISPLAY_SLOTS.length) break;

            Combo combo = AVAILABLE_COMBOS.get(i);
            int slot = COMBO_DISPLAY_SLOTS[i];
            comboSlots.put(slot, combo);

            Optional<Spell> assignedSpellOpt = playerSpellManager.getActiveSpellForCombo(player, combo);
            ItemStack comboItem;
            if (assignedSpellOpt.isPresent()) {
                Spell assignedSpell = assignedSpellOpt.get();
                comboItem = GUIUtils.createGuiItem(Material.ENCHANTED_BOOK,
                        "&a" + assignedSpell.getDisplayName(player),
                        "&7" + combo.getDisplayString(), "&r",
                        assignedSpell.getDisplayDescription(player), "&r",
                        plugin.getMessage(player, "spell.gui.item.mana_cost", assignedSpell.getManaCost()),
                        plugin.getMessage(player, "spell.gui.item.cooldown", (assignedSpell.getCooldownMillis() / 1000))
                );
            } else {
                comboItem = GUIUtils.createGuiItem(Material.MAP,
                        plugin.getMessage(player, "spell.gui.item.empty_combo.name", combo.getDisplayString()),
                        plugin.getMessages(player, "spell.gui.item.empty_combo.lore")
                );
            }

            if (currentMode != Mode.NORMAL) {
                comboItem.addUnsafeEnchantment(Enchantment.LURE, 1);
                comboItem.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            inv.setItem(slot, comboItem);
        }
    }

    @NotNull @Override public Inventory getInventory() { return inv; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Cooldown check
        long now = System.currentTimeMillis();
        if (lastClickTime.getOrDefault(player.getUniqueId(), 0L) + CLICK_COOLDOWN_MS > now) {
            event.setCancelled(true);
            return;
        }
        lastClickTime.put(player.getUniqueId(), now);

        event.setCancelled(true); // Always cancel GUI clicks

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return; // Ignore clicks in player's inventory
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        int slot = event.getSlot();
        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isEmpty()) return;

        // Pass the click type to the handler
        switch (currentMode) {
            case NORMAL -> handleNormalModeClick(player, slot, playerDataOpt.get(), event.getClick());
            case ASSIGNING -> handleAssigningModeClick(player, slot, playerDataOpt.get());
            case UNASSIGNING -> handleUnassigningModeClick(player, slot, playerDataOpt.get());
        }
    }

    private void handleNormalModeClick(Player player, int slot, CharacterData characterData, org.bukkit.event.inventory.ClickType clickType) {
        if (slot == 27) { // Botão "Ver Magias Conhecidas"
            new KnownSpellsGUI(plugin, spellModule, this, player, 0).open();
        } else if (slot == 31) { // Botão "Desatribuir"
            if (clickType == org.bukkit.event.inventory.ClickType.RIGHT) { // Apenas clique direito
                player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.0f);
                changeModeAndRefresh(player, Mode.UNASSIGNING, null);
            } else {
                player.sendMessage(plugin.getMessage(player, "spell.gui.assign.unassign_right_click_hint"));
            }
        } else if (slot == 35) { // Botão de SAIR
            player.closeInventory();
        }
    }

    private void handleAssigningModeClick(Player player, int slot, CharacterData characterData) {
        if (comboSlots.containsKey(slot)) {
            Combo targetCombo = comboSlots.get(slot);
            if (playerSpellManager.assignSpellToCombo(player, targetCombo, spellIdToAssign)) {
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            }
            changeModeAndRefresh(player, Mode.NORMAL, null); // Usa o novo método
        } else if (slot == 35) { // Botão de SAIR (ou cancelar atribuição)
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            player.sendMessage(plugin.getMessage(player, "spell.gui.assign.cancel"));
            changeModeAndRefresh(player, Mode.NORMAL, null); // Usa o novo método
        }
    }

    private void handleUnassigningModeClick(Player player, int slot, CharacterData characterData) {
        if (comboSlots.containsKey(slot)) {
            Combo comboToUnassign = comboSlots.get(slot);
            if (playerSpellManager.getActiveSpellForCombo(player, comboToUnassign).isPresent()) {
                if (playerSpellManager.unassignSpellFromCombo(player, comboToUnassign)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.0f);
                }
                changeModeAndRefresh(player, Mode.NORMAL, null); // Usa o novo método
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.2f);
                player.sendMessage(plugin.getMessage(player, "spell.gui.assign.fail_unassign_empty"));
            }
        } else if (slot == 35) { // Botão de SAIR (ou cancelar desatribuição)
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            player.sendMessage(plugin.getMessage(player, "spell.gui.assign.cancel_unassign"));
            changeModeAndRefresh(player, Mode.NORMAL, null); // Usa o novo método
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            // Limpeza do cooldown
            if (event.getPlayer() instanceof Player) {
                lastClickTime.remove(event.getPlayer().getUniqueId());
            }
            HandlerList.unregisterAll(this);
        }
    }
}