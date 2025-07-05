package me.ray.aethelgardRPG.modules.character.guis.fallback;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.guis.CharacterDeletionConfirmGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Importação adicionada

public class CharacterSelectionGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final Player player;
    private final List<CharacterData> characters;
    private final Map<Integer, Integer> slotToCharacterIdMap = new HashMap<>();
    private Inventory inv;

    public CharacterSelectionGUI(AethelgardRPG plugin, CharacterModule characterModule, Player player, List<CharacterData> characters) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.player = player;
        this.characters = characters;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        int size = 27; // 3 linhas
        inv = Bukkit.createInventory(this, size, plugin.getMessage(player, "character.selection_gui.title"));
        initializeItems();
        player.openInventory(inv);
    }

    private void initializeItems() {
        // Slots para personagens: 10, 12, 14, 16
        int[] characterSlots = {10, 12, 14, 16};

        for (int i = 0; i < characterSlots.length; i++) {
            int slot = characterSlots[i];
            if (i < characters.size()) {
                // Exibir personagem existente
                CharacterData data = characters.get(i);
                slotToCharacterIdMap.put(slot, data.getCharacterId());

                ItemStack charItem = GUIUtils.createGuiItem(
                        Material.PLAYER_HEAD,
                        plugin.getMessage(player, "character.selection_gui.character.name", data.getCharacterName()),
                        plugin.getMessage(player, "character.selection_gui.character.lore_class", data.getSelectedClass().getDisplayName(player)),
                        plugin.getMessage(player, "character.selection_gui.character.lore_level", data.getLevel()),
                        "",
                        plugin.getMessage(player, "character.selection_gui.character.lore_play"),
                        plugin.getMessage(player, "character.selection_gui.character.lore_delete")
                );
                inv.setItem(slot, charItem);
            } else {
                // Exibir slot de criação de personagem
                ItemStack createItem = GUIUtils.createGuiItem(
                        Material.GREEN_STAINED_GLASS_PANE,
                        plugin.getMessage(player, "character.selection_gui.create.name"),
                        plugin.getMessage(player, "character.selection_gui.create.lore")
                );
                inv.setItem(slot, createItem);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        Player p = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.getType() == Material.GREEN_STAINED_GLASS_PANE) {
            // Clicou para criar um novo personagem
            p.closeInventory();
            characterModule.openCharacterCreationGUI(p);
            player.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        } else if (slotToCharacterIdMap.containsKey(slot)) {
            int characterId = slotToCharacterIdMap.get(slot);
            if (event.isLeftClick()) {
                // Clicou para jogar com o personagem
                p.closeInventory();
                p.sendMessage(plugin.getMessage(p, "character.selection_gui.loading", clickedItem.getItemMeta().getDisplayName()));
                characterModule.selectAndLoadCharacter(p, characterId);
                player.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            } else if (event.isRightClick()) {
                // Clicou para deletar o personagem
                p.closeInventory();

                // --- CORREÇÃO AQUI ---
                // Encontra o objeto CharacterData correspondente ao ID do slot clicado
                Optional<CharacterData> charDataOpt = characters.stream()
                        .filter(cd -> cd.getCharacterId() == characterId)
                        .findFirst();

                if (charDataOpt.isPresent()) {
                    // Passa o objeto CharacterData para a GUI de confirmação
                    new CharacterDeletionConfirmGUI(plugin, characterModule, p, charDataOpt.get()).open();
                    player.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    // Caso de erro onde os dados não são encontrados (improvável, mas seguro)
                    p.sendMessage(plugin.getMessage(p, "character.error.load_failed"));
                }
                // --- FIM DA CORREÇÃO ---
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            // Se o jogador fechar a GUI sem selecionar um personagem, ele é kickado.
            // Isso previne que ele fique em um estado "limbo" sem dados carregados.
            if (!characterModule.getSessionManager().getActiveCharacter(player).isPresent()) {
                player.kickPlayer(plugin.getMessage(player, "character.selection_gui.kick_on_close"));
            }
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inv;
    }
}