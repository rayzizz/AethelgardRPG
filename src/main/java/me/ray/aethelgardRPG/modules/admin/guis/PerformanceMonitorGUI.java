package me.ray.aethelgardRPG.modules.admin.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class PerformanceMonitorGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private Inventory inv;

    public PerformanceMonitorGUI(AethelgardRPG plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Map<String, Long> timings = plugin.getPerformanceMonitor().getExecutionTimes();
        int guiSize = Math.min(54, (int) (Math.ceil((timings.size() + 2) / 9.0) * 9)); // Calcula o tamanho da GUI dinamicamente (2 slots extras de borda)
        if (guiSize == 0 && timings.isEmpty()) guiSize = 9; // Se vazio, ainda abre uma GUI de 1 linha
        else if (guiSize < 9 && !timings.isEmpty()) guiSize = 9;
        else if (guiSize == 0) guiSize = 9; // Fallback

        inv = Bukkit.createInventory(this, guiSize, plugin.getMessage(player, "admin.gui_performance.title"));

        initializeItems(timings, player);

        fillBorders();

        player.openInventory(inv);
    }

    private void initializeItems(Map<String, Long> timings, Player playerContext) {
        if (timings.isEmpty()) {
            // CORREÇÃO: Usar playerContext para mensagens da GUI
            inv.setItem(inv.getSize() / 2, GUIUtils.createGuiItem(Material.BARRIER,
                    plugin.getMessage(playerContext, "admin.gui_performance.no_data.name"),
                    plugin.getMessage(playerContext, "admin.gui_performance.no_data.lore")
            ));
            return;
        }

        int slot = 0;
        // Ordenar as entradas alfabeticamente para consistência
        List<Map.Entry<String, Long>> sortedTimings = new ArrayList<>(timings.entrySet());
        sortedTimings.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, Long> entry : sortedTimings) {
            if (slot >= inv.getSize()) break; // Não exceder o tamanho da GUI

            String id = entry.getKey();
            double timeMs = entry.getValue() / 1_000_000.0;

            Material icon = Material.PAPER; // Ícone padrão
            String descriptionKey = "admin.gui_performance.item.description_default"; // Chave de descrição padrão

            String itemName = id; // Valor padrão
            if (id.startsWith("moduleEnable")) {
                icon = Material.GREEN_STAINED_GLASS_PANE;
                itemName = id.substring("moduleEnable".length()); // Remove "moduleEnable"
                descriptionKey = "admin.gui_performance.item.description_module_enable";
            } else if (id.startsWith("moduleDisable")) {
                icon = Material.RED_STAINED_GLASS_PANE;
                itemName = id.substring("moduleDisable".length()); // Remove "moduleDisable"
                descriptionKey = "admin.gui_performance.item.description_module_disable";
            } else if (id.startsWith("playerDataLoad.")) {
                icon = Material.CHEST;
                descriptionKey = "admin.gui_performance.item.description_player_load";
                itemName = id.substring("playerDataLoad.".length()); // Remove "playerDataLoad."
            } else if (id.startsWith("playerDataSave.")) {
                icon = Material.ENDER_CHEST;
                itemName = id.substring("playerDataSave.".length()); // Remove "playerDataSave."
                descriptionKey = "admin.gui_performance.item.description_player_save";
            } else {
                // Se não corresponder a nenhum tratamento especial, tenta remover o camelCase
                itemName = id.replaceAll("([A-Z])", " $1");
            }
            // Adicionar mais 'else if' para outros prefixos de ID que você usa

            List<String> lore = new ArrayList<>();
            lore.add(plugin.getMessage(playerContext, "admin.gui_performance.item.lore_time", String.format("%.3f", timeMs)));
            lore.add(""); // Linha em branco
            lore.add(plugin.getMessage(playerContext, descriptionKey, id.substring(id.indexOf('.') + 1))); // Passa a parte relevante do ID

            inv.setItem(slot++, GUIUtils.createGuiItem(icon,
                    plugin.getMessage(playerContext, "admin.gui_performance.item.name_prefix") + id,
                    lore
            ));
        }
    }

    private void fillBorders() {
        ItemStack borderItem = GUIUtils.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        int size = inv.getSize();

        // Preenche as bordas superior e inferior
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, borderItem); // Linha superior
            if (size > 9 && inv.getItem(size - 1 - i) == null) inv.setItem(size - 1 - i, borderItem); // Linha inferior
        }

        // Preenche as bordas laterais (exceto cantos já preenchidos)
        for (int i = 1; i < (size / 9) - 1; i++) {
            if (inv.getItem(i * 9) == null) inv.setItem(i * 9, borderItem); // Borda esquerda
            if (inv.getItem(i * 9 + 8) == null) inv.setItem(i * 9 + 8, borderItem); // Borda direita
        }
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
        event.setCancelled(true); // Apenas visualização
    }
}