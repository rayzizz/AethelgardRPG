package me.ray.aethelgardRPG.modules.admin.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.commands.utils.ChatInputHandler;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class RPGAdminMainGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private Inventory inv;

    public RPGAdminMainGUI(AethelgardRPG plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        // 3 linhas (27 slots) para o painel principal
        inv = Bukkit.createInventory(this, 27, plugin.getMessage(player, "admin.gui_main.title"));

        initializeItems(player);

        player.openInventory(inv);
    }
    
    private void initializeItems(Player player) {
        // Botão de Monitoramento de Performance
        inv.setItem(10, GUIUtils.createGuiItem(Material.CLOCK,
                plugin.getMessage(player, "admin.gui_main.item.performance.name"),
                plugin.getMessage(player, "admin.gui_main.item.performance.lore1"),
                plugin.getMessage(player, "admin.gui_main.item.performance.lore2")
        ));

        // Botão de Inspeção de Jogador
        inv.setItem(12, GUIUtils.createGuiItem(Material.PLAYER_HEAD,
                plugin.getMessage(player, "admin.gui_main.item.inspect.name"),
                plugin.getMessage(player, "admin.gui_main.item.inspect.lore1"),
                plugin.getMessage(player, "admin.gui_main.item.inspect.lore2")
        ));

        // Botão de Gerenciamento de Economia (Placeholder)
        inv.setItem(14, GUIUtils.createGuiItem(Material.GOLD_INGOT,
                plugin.getMessage(player, "admin.gui_main.item.economy.name"),
                plugin.getMessage(player, "admin.gui_main.item.economy.lore1"),
                plugin.getMessage(player, "admin.gui_main.item.economy.lore2")
        ));

        // Botão para Recarregar Configurações
        inv.setItem(16, GUIUtils.createGuiItem(Material.WRITABLE_BOOK,
                plugin.getMessage(player, "admin.gui_main.item.reload.name"),
                plugin.getMessage(player, "admin.gui_main.item.reload.lore1"),
                plugin.getMessage(player, "admin.gui_main.item.reload.lore2")
        ));

        // TODO: Adicionar mais botões para outras funcionalidades administrativas
        // Ex: Gerenciamento de Módulos, Configurações de Mobs, etc.

        // Item de Fechar (opcional, mas bom para consistência)
        // inv.setItem(26, GUIUtils.createGuiItem(Material.BARRIER, plugin.getMessage("admin_gui.main.item.close.name")));
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

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Lógica para cada botão
        if (event.getSlot() == 10) { // Performance
            new PerformanceMonitorGUI(plugin).open(player);
            // player.performCommand("rpgadmin performance"); // Não executa mais o comando, abre a GUI
            // player.closeInventory(); // REMOVA ESTA LINHA
        } else if (event.getSlot() == 12) { // Inspect Player
            player.closeInventory();
            TextComponent message = new TextComponent(plugin.getMessage(player, "admin.gui_main.action.inspect-prompt-json-click"));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rpgadmin inspect ")); // Sugere o comando
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getMessage(player, "admin.gui_main.action.inspect-prompt-json-hover"))));
            player.spigot().sendMessage(message);

            player.sendMessage(plugin.getMessage(player, "admin.gui_main.action.inspect-prompt-chat-instruction"));

            ChatInputHandler.requestPlayerInput(player, (playerName) -> {
                if (playerName.equalsIgnoreCase("cancelar")) {
                    player.sendMessage(plugin.getMessage(player, "admin.gui_main.action.inspect-cancelled"));
                    return;
                }
                player.performCommand("rpgadmin inspect " + playerName);
            }, plugin.getMessage(player, "admin.gui_main.action.inspect-timeout"));

        } else if (event.getSlot() == 14) { // Economy
            player.performCommand("rpgadmin economy");
            player.closeInventory();
        } else if (event.getSlot() == 16) { // Reload
            player.performCommand("rpg reload"); // Usa o comando principal do plugin para reload
            player.closeInventory();
        }
        // Adicionar mais else if para outros botões
    }
}