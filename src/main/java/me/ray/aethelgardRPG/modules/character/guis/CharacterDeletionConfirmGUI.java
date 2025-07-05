package me.ray.aethelgardRPG.modules.character.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
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

import java.util.concurrent.TimeUnit;

public class CharacterDeletionConfirmGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final Player player;
    private final CharacterData characterDataToDelete;
    private Inventory inv;

    // NOVO: Flag para controlar o fechamento da GUI
    private boolean isClosingProgrammatically = false;

    public CharacterDeletionConfirmGUI(AethelgardRPG plugin, CharacterModule characterModule, Player player, CharacterData characterDataToDelete) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.player = player;
        this.characterDataToDelete = characterDataToDelete;
        // Registra o listener apenas quando a GUI é criada
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        // Garante que qualquer listener antigo para esta instância seja removido
        // antes de registrar um novo. Isso previne o acúmulo de listeners.
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // CORREÇÃO: Adicionado "gui." ao caminho da chave
        inv = Bukkit.createInventory(this, 27, plugin.getMessage(player, "character.gui.delete_gui.title"));
        initializeItems();
        player.openInventory(inv);
    }

    private void initializeItems() {
        // Preenche o fundo
        ItemStack background = GUIUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }

        if (characterDataToDelete.isPendingDeletion()) {
            // Modo: Gerenciar exclusão pendente (cancelar ou manter)
            long remainingSeconds = characterDataToDelete.getRemainingDeletionTimeSeconds();
            long minutes = TimeUnit.SECONDS.toMinutes(remainingSeconds);
            long seconds = remainingSeconds % 60;

            // CORREÇÃO: Adicionado "gui." aos caminhos das chaves
            inv.setItem(13, GUIUtils.createGuiItem(
                    Material.ORANGE_STAINED_GLASS_PANE,
                    plugin.getMessage(player, "character.gui.delete_gui.pending.name", characterDataToDelete.getCharacterName()),
                    plugin.getMessage(player, "character.gui.delete_gui.pending.lore1"),
                    plugin.getMessage(player, "character.gui.delete_gui.pending.lore2", minutes, seconds)
            ));

            // Botão de Cancelar Exclusão
            inv.setItem(11, GUIUtils.createGuiItem(
                    Material.GREEN_WOOL,
                    plugin.getMessage(player, "character.gui.delete_gui.cancel_pending.name"),
                    plugin.getMessages(player, "character.gui.delete_gui.cancel_pending.lore")
            ));

            // Botão de Manter Exclusão Pendente (apenas fechar)
            inv.setItem(15, GUIUtils.createGuiItem(
                    Material.RED_WOOL,
                    plugin.getMessage(player, "character.gui.delete_gui.keep_pending.name"),
                    plugin.getMessages(player, "character.gui.delete_gui.keep_pending.lore")
            ));

        } else {
            // Modo: Confirmar nova exclusão (soft delete + agendamento)
            FileConfiguration charSettings = plugin.getConfigManager().getCharacterSettingsConfig();
            // A chave "character-deletion" não existe no seu character_settings.yml, adicionei um fallback.
            long delayInMinutes = charSettings.getLong("character-deletion.delay-in-minutes", 10);

            // CORREÇÃO: Adicionado "gui." aos caminhos das chaves
            inv.setItem(13, GUIUtils.createGuiItem(
                    Material.YELLOW_STAINED_GLASS_PANE,
                    plugin.getMessage(player, "character.gui.delete_gui.confirm_new.name", characterDataToDelete.getCharacterName()),
                    plugin.getMessage(player, "character.gui.delete_gui.confirm_new.lore1"),
                    plugin.getMessage(player, "character.gui.delete_gui.confirm_new.lore2", delayInMinutes)
            ));

            // Botão de Confirmar Exclusão
            inv.setItem(11, GUIUtils.createGuiItem(
                    Material.RED_WOOL,
                    plugin.getMessage(player, "character.gui.delete_gui.confirm.name"),
                    plugin.getMessages(player, "character.gui.delete_gui.confirm.lore")
            ));

            // Botão de Cancelar (voltar)
            inv.setItem(15, GUIUtils.createGuiItem(
                    Material.GREEN_WOOL,
                    plugin.getMessage(player, "character.gui.delete_gui.cancel.name"),
                    plugin.getMessages(player, "character.gui.delete_gui.cancel.lore")
            ));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        Player p = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (characterDataToDelete.isPendingDeletion()) {
            // Lógica para gerenciar exclusão pendente
            if (slot == 11) { // Cancelar Exclusão Pendente
                this.isClosingProgrammatically = true; // ALTERAÇÃO
                p.closeInventory();
                characterModule.cancelCharacterDeletion(p, characterDataToDelete);
                p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.5f, 1.2f);
            } else if (slot == 15) { // Manter Exclusão Pendente (apenas fechar)
                this.isClosingProgrammatically = true; // ALTERAÇÃO
                p.closeInventory();
                characterModule.openCharacterSelectionFor(p, true); // Reabre a tela de seleção
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        } else {
            // Lógica para confirmar nova exclusão
            if (slot == 11) { // Confirmar Nova Exclusão
                this.isClosingProgrammatically = true; // ALTERAÇÃO
                p.closeInventory();
                characterModule.scheduleCharacterDeletion(p, characterDataToDelete);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.2f);
            } else if (slot == 15) { // Cancelar (voltar)
                this.isClosingProgrammatically = true; // ALTERAÇÃO
                p.closeInventory();
                characterModule.openCharacterSelectionFor(p, true); // Reabre a tela de seleção
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            // ALTERAÇÃO: Lógica para lidar com o fechamento via ESC
            if (!isClosingProgrammatically) {
                // Se a GUI foi fechada pelo jogador (ESC), reabrimos a tela de seleção
                // para evitar que ele seja desconectado.
                // Usamos um delay de 1 tick para evitar conflitos com o evento de fechamento.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    characterModule.openCharacterSelectionFor(player, true);
                });
            }
            // Sempre desregistramos o listener para evitar memory leaks.
            HandlerList.unregisterAll(this);
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inv;
    }
}
