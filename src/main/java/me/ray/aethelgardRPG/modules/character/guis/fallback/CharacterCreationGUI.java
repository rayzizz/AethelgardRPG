package me.ray.aethelgardRPG.modules.character.guis.fallback;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.commands.utils.ChatInputHandler;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import org.bukkit.Bukkit;
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
import java.util.Map;

public class CharacterCreationGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final Player player;
    private Inventory inv;
    private final Map<Integer, RPGClass> classSlots = new HashMap<>();

    public CharacterCreationGUI(AethelgardRPG plugin, CharacterModule characterModule, Player player) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.player = player;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inv = Bukkit.createInventory(this, 27, plugin.getMessage(player, "character.creation_gui.title"));
        initializeItems();
        player.openInventory(inv);
    }

    private void initializeItems() {
        // Mapeia slots para classes
        classSlots.put(11, RPGClass.GUERREIRO);
        classSlots.put(13, RPGClass.MAGO);
        classSlots.put(15, RPGClass.ARQUEIRO);

        // Adiciona itens para cada classe
        for (Map.Entry<Integer, RPGClass> entry : classSlots.entrySet()) {
            int slot = entry.getKey();
            RPGClass rpgClass = entry.getValue();
            ItemStack classItem = GUIUtils.createGuiItem(
                    rpgClass.getIcon(),
                    rpgClass.getDisplayName(player),
                    rpgClass.getDescription(player)
            );
            inv.setItem(slot, classItem);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        Player p = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (classSlots.containsKey(slot)) {
            RPGClass selectedClass = classSlots.get(slot);
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);

            p.sendMessage(plugin.getMessage(p, "character.creation.prompt_name"));
            ChatInputHandler.requestPlayerInput(p, characterName -> {
                if (characterName.equalsIgnoreCase("cancelar")) {
                    p.sendMessage(plugin.getMessage(p, "character.creation.cancelled"));
                    characterModule.openCharacterSelectionFor(p); // Volta para a seleção
                    return;
                }
                if (characterName.length() < 3 || characterName.length() > 16 || !characterName.matches("^[a-zA-Z0-9_]+$")) {
                    p.sendMessage(plugin.getMessage(p, "character.creation.invalid_name"));
                    characterModule.openCharacterSelectionFor(p); // Volta para a seleção
                    return;
                }

                // Cria o personagem no banco de dados
                characterModule.getCharacterRepository()
                        .createCharacter(p.getUniqueId(), characterName, selectedClass, p.getLocation())
                        .thenAccept(newCharacterId -> {
                            if (newCharacterId != -1) {
                                p.sendMessage(plugin.getMessage(p, "character.creation.success", characterName, selectedClass.getDisplayName(p)));
                            } else {
                                p.sendMessage(plugin.getMessage(p, "character.creation.error"));
                            }
                            // Sempre retorna para a tela de seleção para que o jogador possa ver seu novo personagem
                            Bukkit.getScheduler().runTask(plugin, () -> characterModule.openCharacterSelectionFor(p));
                        });

            }, plugin.getMessage(p, "character.creation.timeout"));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            // Se o jogador fechar a GUI de criação, ele deve ser enviado de volta para a seleção
            // para evitar ficar em um estado de limbo.
            if (!characterModule.getSessionManager().getActiveCharacter(player).isPresent()) {
                // Atraso de 1 tick para evitar conflito com a abertura de outra GUI
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.getOpenInventory().getTopInventory().getHolder() == null) {
                        characterModule.openCharacterSelectionFor(player);
                    }
                }, 1L);
            }
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inv;
    }
}