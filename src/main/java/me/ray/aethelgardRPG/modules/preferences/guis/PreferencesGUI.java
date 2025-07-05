package me.ray.aethelgardRPG.modules.preferences.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.preferences.PlayerPreferences;
import me.ray.aethelgardRPG.modules.preferences.PlayerPreferencesManager;

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
import org.jetbrains.annotations.NotNull;

public class PreferencesGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final PlayerPreferencesManager preferencesManager;
    private Inventory inv;
    private Player viewer;

    public PreferencesGUI(AethelgardRPG plugin, PlayerPreferencesManager preferencesManager) {
        this.plugin = plugin;
        this.preferencesManager = preferencesManager;
    }

    public void open(Player player) {
        this.viewer = player;
        // Registra os eventos desta instância da GUI
        Bukkit.getPluginManager().registerEvents(this, plugin);

        String title = plugin.getMessage(player, "preferences.gui.title");
        inv = Bukkit.createInventory(this, 27, title);

        initializeItems();
        player.openInventory(inv);
    }

    private void initializeItems() {
        inv.clear(); // Limpa o inventário para redesenhar

        PlayerPreferences prefs = preferencesManager.getPreferences(viewer);

        // --- Item para Partículas de Magias ---
        boolean particlesEnabled = prefs.canShowSpellParticles();
        GUIUtils.Builder particleItemBuilder = new GUIUtils.Builder(particlesEnabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .withName(plugin.getMessage(viewer, "preferences.gui.particles.name"))
                .withLore(plugin.getMessages(viewer, "preferences.gui.particles.lore",
                        getStatusString(particlesEnabled, viewer)))
                .hideAllAttributes();
        if (particlesEnabled) {
            particleItemBuilder.withGlow();
        }
        inv.setItem(11, particleItemBuilder.build());


        // --- Item para Efeitos Visuais de Mapa ---
        boolean mapEffectVisualsEnabled = prefs.canShowMapEffectVisuals(); // Usando o novo getter
        GUIUtils.Builder mapEffectVisualsItemBuilder = new GUIUtils.Builder(mapEffectVisualsEnabled ? Material.DIAMOND_PICKAXE : Material.WOODEN_PICKAXE)
                .withName(plugin.getMessage(viewer, "preferences.gui.map_effect_visuals.name")) // Nova chave de idioma
                .withLore(plugin.getMessages(viewer, "preferences.gui.map_effect_visuals.lore", // Nova chave de idioma
                        getStatusString(mapEffectVisualsEnabled, viewer)))
                .hideAllAttributes();
        if (mapEffectVisualsEnabled) {
            mapEffectVisualsItemBuilder.withGlow();
        }
        inv.setItem(15, mapEffectVisualsItemBuilder.build());

        // Item de fechar
        inv.setItem(26, GUIUtils.createGuiItem(Material.BARRIER, plugin.getMessage(viewer, "general.gui.close")));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.getUniqueId().equals(viewer.getUniqueId())) return;

        PlayerPreferences prefs = preferencesManager.getPreferences(player);

        switch (event.getSlot()) {
            case 11: // Partículas
                prefs.setShowSpellParticles(!prefs.canShowSpellParticles());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, prefs.canShowSpellParticles() ? 1.2f : 0.8f);
                break;
            case 15: // Efeitos Visuais de Mapa
                prefs.setShowMapEffectVisuals(!prefs.canShowMapEffectVisuals()); // Usando o novo setter
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, prefs.canShowMapEffectVisuals() ? 1.2f : 0.8f); // Usando o novo getter
                break;
            case 26: // Fechar
                player.closeInventory();
                return;
            default:
                return; // Sai se clicou em um slot vazio ou não funcional
        }

        // Salva e atualiza a GUI
        preferencesManager.savePlayerPreferences(player);
        initializeItems(); // Redesenha os itens para refletir a mudança
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && event.getPlayer().getUniqueId().equals(viewer.getUniqueId())) {
            // Desregistra o listener para evitar memory leaks
            HandlerList.unregisterAll(this);
        }
    }

    private String getStatusString(boolean status, Player player) {
        return status
                ? plugin.getMessage(player, "general.status.enabled")
                : plugin.getMessage(player, "general.status.disabled");
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inv;
    }
}