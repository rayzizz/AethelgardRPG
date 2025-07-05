package me.ray.aethelgardRPG.modules.character.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
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
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AttributeResetConfirmationGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final PlayerProfileGUI previousGui;
    private final Player player;
    private Inventory inv;

    public AttributeResetConfirmationGUI(AethelgardRPG plugin, CharacterModule characterModule, PlayerProfileGUI previousGui, Player player) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.previousGui = previousGui;
        this.player = player;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inv = Bukkit.createInventory(this, 27, plugin.getMessage(player, "character.gui.attribute_reset_confirm.title"));
        initializeItems();
        player.openInventory(inv);
    }

    private void initializeItems() {
        // Background
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, GUIUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
        int totalPoints = 0;
        if (playerDataOpt.isPresent()) {
            // Calcula o total de pontos que o jogador terá após o reset
            int currentPoints = playerDataOpt.get().getAttributePoints();
            int refundedPoints = characterModule.getRefundableAttributePoints(playerDataOpt.get());
            totalPoints = currentPoints + refundedPoints;
        }

        // Info item
        inv.setItem(13, GUIUtils.createGuiItem(Material.EXPERIENCE_BOTTLE,
                plugin.getMessage(player, "character.gui.attribute_reset_confirm.info.name"),
                plugin.getMessage(player, "character.gui.attribute_reset_confirm.info.lore1"),
                plugin.getMessage(player, "character.gui.attribute_reset_confirm.info.lore2"),
                plugin.getMessage(player, "character.gui.attribute_reset_confirm.info.lore3", totalPoints)
        ));

        // Confirm button
        inv.setItem(11, GUIUtils.createGuiItem(Material.LIME_STAINED_GLASS_PANE,
                plugin.getMessage(player, "character.gui.attribute_reset_confirm.confirm.name"),
                plugin.getMessage(player, "character.gui.attribute_reset_confirm.confirm.lore")
        ));

        // Cancel button
        inv.setItem(15, GUIUtils.createGuiItem(Material.RED_STAINED_GLASS_PANE,
                plugin.getMessage(player, "character.gui.attribute_reset_confirm.cancel.name"),
                plugin.getMessage(player, "character.gui.attribute_reset_confirm.cancel.lore")
        ));
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

        if (!(event.getWhoClicked() instanceof Player p) || !p.equals(player)) return;

        int clickedSlot = event.getSlot();

        if (clickedSlot == 11) { // Confirm
            characterModule.resetAttributesAndRefundPoints(player);
            player.sendMessage(plugin.getMessage(player, "character.gui.attribute_reset_confirm.success"));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.0f);
            player.closeInventory(); // This will trigger onInventoryClose to reopen the profile
        } else if (clickedSlot == 15) { // Cancel
            player.sendMessage(plugin.getMessage(player, "character.gui.attribute_reset_confirm.cancelled"));
            player.closeInventory(); // This will trigger onInventoryClose to reopen the profile
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
            // Reopen the main profile GUI for the player
            // *** CORRECTION APPLIED HERE ***
            // The open() method on PlayerProfileGUI no longer takes an argument.
            Bukkit.getScheduler().runTask(plugin, () -> previousGui.open());
        }
    }
}