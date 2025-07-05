package me.ray.aethelgardRPG.modules.spell.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.spell.PlayerSpellManager;
import me.ray.aethelgardRPG.modules.spell.SpellModule;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class KnownSpellsGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final SpellModule spellModule;
    private final PlayerSpellManager playerSpellManager;
    private final SpellbookGUI parentGUI; // Referência à GUI pai
    private Inventory inv;
    private final Player viewer;
    private final int page;

    private static final int SPELLS_PER_PAGE = 28;

    public KnownSpellsGUI(AethelgardRPG plugin, SpellModule spellModule, SpellbookGUI parentGUI, Player viewer, int page) {
        this.plugin = plugin;
        this.spellModule = spellModule;
        this.playerSpellManager = spellModule.getPlayerSpellManager();
        this.parentGUI = parentGUI;
        this.viewer = viewer;
        this.page = page;
    }

    public void open() {
        // Garante que qualquer listener antigo para esta instância seja removido
        // antes de registrar um novo. Isso previne o acúmulo de listeners.
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        String title = plugin.getMessage(viewer, "spell.gui.known_spells.title");
        inv = Bukkit.createInventory(this, 45, title + " (Pág. " + (page + 1) + ")");
        initializeItems();
        viewer.openInventory(inv);
    }

    private void initializeItems() {
        // Pega apenas as magias que NÃO estão atribuídas a um combo.
        List<Spell> knownSpells = playerSpellManager.getUnassignedKnownSpellsForClass(viewer);

        ItemStack background = GUIUtils.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 45; i++) inv.setItem(i, background);

        inv.setItem(36, GUIUtils.createGuiItem(Material.ARROW, plugin.getMessage(viewer, "spell.gui.known_spells.back_button")));
        if (page > 0) {
            inv.setItem(39, GUIUtils.createGuiItem(Material.SPECTRAL_ARROW, plugin.getMessage(viewer, "spell.gui.known_spells.previous_page")));
        }
        if ((page + 1) * SPELLS_PER_PAGE < knownSpells.size()) {
            inv.setItem(41, GUIUtils.createGuiItem(Material.SPECTRAL_ARROW, plugin.getMessage(viewer, "spell.gui.known_spells.next_page")));
        }

        int startIndex = page * SPELLS_PER_PAGE;
        for (int i = 0; i < SPELLS_PER_PAGE; i++) {
            int spellIndex = startIndex + i;
            if (spellIndex >= knownSpells.size()) break;
            Spell spell = knownSpells.get(spellIndex);

            ItemStack spellItem = new GUIUtils.Builder(Material.WRITABLE_BOOK)
                    .withName("&b" + spell.getDisplayName(viewer))
                    .withLore(
                            "&r",
                            spell.getDisplayDescription(viewer), // Adiciona a descrição da magia
                            "&r",
                            plugin.getMessage(viewer, "spell.gui.item.mana_cost", spell.getManaCost()),
                            plugin.getMessage(viewer, "spell.gui.item.cooldown", (spell.getCooldownMillis() / 1000)),
                            "&7Nível Requerido: &f" + spell.getRequiredLevel(),
                            "&r",
                            plugin.getMessage(viewer, "spell.gui.item.assign_lore")
                    ).build();

            ItemMeta meta = spellItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(PDCKeys.GUI_SPELL_ID_KEY, PersistentDataType.STRING, spell.getId());
                spellItem.setItemMeta(meta);
            }

            inv.setItem(i, spellItem);
        }
    }

    @NotNull @Override public Inventory getInventory() { return inv; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        int slot = event.getSlot();

        if (slot == 36) { // Botão "Voltar ao Grimório"
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            // Reabre a GUI principal. O método open() já a redefine para o estado padrão.
            parentGUI.open(player);
            return;
        }

        if (slot == 39 && clickedItem.getType() == Material.SPECTRAL_ARROW) { // Página Anterior
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
            new KnownSpellsGUI(plugin, this.spellModule, parentGUI, player, page - 1).open();
            return;
        }

        if (slot == 41 && clickedItem.getType() == Material.SPECTRAL_ARROW) { // Próxima Página
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
            new KnownSpellsGUI(plugin, this.spellModule, parentGUI, player, page + 1).open();
            return;
        }

        if (clickedItem.getType() == Material.WRITABLE_BOOK && clickedItem.hasItemMeta()) { // Clique em uma magia
            ItemMeta meta = clickedItem.getItemMeta();
            String spellId = meta.getPersistentDataContainer().get(PDCKeys.GUI_SPELL_ID_KEY, PersistentDataType.STRING);

            if (spellId != null) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                // Fecha a GUI atual e abre a GUI pai diretamente no modo de atribuição.
                parentGUI.openForAssigning(player, spellId);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
    }
}