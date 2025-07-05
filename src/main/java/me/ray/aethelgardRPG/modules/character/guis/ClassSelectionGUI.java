package me.ray.aethelgardRPG.modules.character.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.exceptions.CharacterAlreadyExistsException; // Importe a nova exceção
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ClassSelectionGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final CharacterSlotSelectionGUI parentGUI; // Referência à GUI anterior
    private Inventory inv;
    private final Player player;

    private final Map<Integer, RPGClass> classSlotMap = new HashMap<>();
    private boolean isTransitioning = false;

    public ClassSelectionGUI(AethelgardRPG plugin, CharacterModule characterModule, CharacterSlotSelectionGUI parentGUI, Player player) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.parentGUI = parentGUI;
        this.player = player;
    }

    public void open() {
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        inv = Bukkit.createInventory(this, 36, plugin.getMessage(player, "character.gui.class_selection.title"));
        initializeItems();
        player.openInventory(inv);
    }

    private void initializeItems() {
        // Fundo mais bonito
        ItemStack outerBorder = GUIUtils.createGuiItem(Material.BLUE_STAINED_GLASS_PANE, " ");
        ItemStack innerBackground = GUIUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i > 26 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, outerBorder);
            } else {
                inv.setItem(i, innerBackground);
            }
        }

        // Posicionamento centralizado das classes
        int[] classSlots = {11, 12, 13, 14, 15, 22}; // Slots para até 6 classes
        int slotIndex = 0;

        for (RPGClass rpgClass : RPGClass.values()) {
            if (rpgClass == RPGClass.NONE || slotIndex >= classSlots.length) continue;

            Material classIcon;
            switch (rpgClass) {
                case GUERREIRO -> classIcon = Material.IRON_SWORD;
                case MAGO -> classIcon = Material.BLAZE_ROD;
                case ARQUEIRO -> classIcon = Material.BOW;
                case NECROMANTE -> classIcon = Material.BONE;
                case CLERIGO -> classIcon = Material.GOLDEN_AXE;
                case ASSASSINO -> classIcon = Material.IRON_SWORD;
                default -> classIcon = Material.PAPER;
            }

            int currentSlot = classSlots[slotIndex];
            inv.setItem(currentSlot, new GUIUtils.Builder(classIcon)
                    .withName(rpgClass.getDisplayName(player))
                    .withLore(
                            plugin.getMessages(player, "character.gui.class_selection.class_lore",
                                    rpgClass.getDisplayDescription(player),
                                    String.valueOf(rpgClass.getStartingHealth()),
                                    String.valueOf(rpgClass.getStartingMana())
                            )
                    )
                    .withGlow() // Adiciona efeito de brilho
                    .build()
            );
            classSlotMap.put(currentSlot, rpgClass);
            slotIndex++;
        }

        // Botão de voltar
        inv.setItem(27, GUIUtils.createGuiItem(Material.ARROW,
                plugin.getMessage(player, "character.gui.class_selection.back_button.name"),
                // CORREÇÃO APLICADA AQUI:
                plugin.getMessages(player, "character.gui.class_selection.back_button.lore").toArray(new String[0])
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

        if (!(event.getWhoClicked() instanceof Player clickedPlayer)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        int slot = event.getSlot();

        if (classSlotMap.containsKey(slot)) {
            RPGClass selectedClass = classSlotMap.get(slot);
            clickedPlayer.playSound(clickedPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            String characterName = clickedPlayer.getName() + "_" + selectedClass.name();
            this.isTransitioning = true;
            clickedPlayer.closeInventory();

            characterModule.createNewCharacter(clickedPlayer, characterName, selectedClass)
                    .thenAcceptAsync(newCharId -> {
                        if (newCharId != -1) {
                            clickedPlayer.sendMessage(plugin.getMessage(clickedPlayer, "character.creation.success", characterName, selectedClass.getDisplayName(clickedPlayer)));
                        } else {
                            // Este bloco pode ser atingido se createCharacter retornar -1 por outros erros SQL
                            clickedPlayer.sendMessage(plugin.getMessage(clickedPlayer, "character.creation.error"));
                        }
                        // Em vez de chamar o CharacterModule, simplesmente dizemos à GUI pai para se reabrir.
                        // O método open() da parentGUI já busca a lista de personagens mais recente.
                        parentGUI.open();
                    }, Bukkit.getScheduler().getMainThreadExecutor(plugin))
                    .exceptionally(e -> {
                        Throwable actualCause = e.getCause(); // Desembrulha a CompletionException
                        if (actualCause instanceof CharacterAlreadyExistsException) {
                            clickedPlayer.sendMessage(plugin.getMessage(clickedPlayer, "character.creation.error.already_exists")); // Nova chave de tradução
                            plugin.getLogger().log(Level.WARNING, "Tentativa de criar personagem duplicado para " + clickedPlayer.getName() + ": " + actualCause.getMessage());
                        } else {
                            plugin.getLogger().log(Level.SEVERE, "[ClassSelectionGUI] Erro durante a criação do personagem para " + clickedPlayer.getName(), e);
                            clickedPlayer.sendMessage(plugin.getMessage(clickedPlayer, "character.creation.error"));
                        }
                        parentGUI.open(); // Mesmo em caso de erro, tenta reabrir a tela de seleção
                        return null;
                    });

        } else if (slot == 27 && clickedItem.getType() == Material.ARROW) {
            this.isTransitioning = true;
            parentGUI.open();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
            if (!isTransitioning) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && plugin.getCharacterAPI().getCharacterData(player).isEmpty()) {
                        parentGUI.open();
                    }
                }, 1L);
            }
            this.isTransitioning = false;
        }
    }
}