package me.ray.aethelgardRPG.modules.character.guis;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.guis.ProtectedGUI;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
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
import org.bukkit.scheduler.BukkitRunnable; // NOVO
import org.bukkit.scheduler.BukkitTask; // NOVO
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit; // NOVO

public class CharacterSlotSelectionGUI implements InventoryHolder, Listener, ProtectedGUI {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private Inventory inv;
    private final Player player;
    private List<CharacterData> characters;

    private static final int[] CHARACTER_SLOTS = {11, 12, 13, 14, 15};
    private static final int MAX_CHARACTERS = 5;
    private static final int CLOSE_BUTTON_SLOT = 31;

    private final Map<Integer, CharacterData> slotToCharacterMap = new HashMap<>();
    private final Set<Integer> createCharacterSlots = new HashSet<>();

    private boolean isTransitioning = false;
    private BukkitTask countdownTask; // NOVO: Tarefa para o contador regressivo

    public CharacterSlotSelectionGUI(AethelgardRPG plugin, CharacterModule characterModule, Player player, List<CharacterData> initialCharacters) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.player = player;
        this.characters = initialCharacters;
    }

    public void open() {
        // Garante que qualquer listener e tarefa antiga para esta instância seja removido
        HandlerList.unregisterAll(this);
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);

        characterModule.getCharacterRepository().findCharactersByAccount(player.getUniqueId())
                .thenAccept(latestCharacters -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        this.characters = latestCharacters;
                        inv = Bukkit.createInventory(this, 36, plugin.getMessage(player, "character.gui.slot_selection.title"));
                        initializeItems();
                        player.openInventory(inv);
                        startCountdownTask(); // NOVO: Inicia a tarefa de contagem regressiva
                    });
                })
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Falha ao carregar personagens para " + player.getName(), e);
                    player.kickPlayer(plugin.getMessage(player, "character.error.load_failed"));
                    return null;
                });
    }

    private void initializeItems() {
        slotToCharacterMap.clear();
        createCharacterSlots.clear();

        ItemStack background = GUIUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }

        for (int i = 0; i < MAX_CHARACTERS; i++) {
            int currentSlot = CHARACTER_SLOTS[i];

            if (i < characters.size()) {
                CharacterData charData = characters.get(i);
                slotToCharacterMap.put(currentSlot, charData);

                String classSymbol = charData.getSelectedClass().getSymbol();
                String classDisplayName = charData.getSelectedClass().getDisplayName(player);

                List<String> lore = new ArrayList<>();
                lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_class", classDisplayName));
                lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_level", String.valueOf(charData.getLevel())));
                lore.add(""); // Espaçador

                ItemStack charItem;
                if (charData.isPendingDeletion()) {
                    // Personagem pendente de exclusão
                    long remainingSeconds = charData.getRemainingDeletionTimeSeconds();
                    long minutes = TimeUnit.SECONDS.toMinutes(remainingSeconds);
                    long seconds = remainingSeconds % 60;

                    lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_pending_deletion"));
                    lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_time_left", minutes, seconds));
                    lore.add(""); // Espaçador
                    lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_manage_deletion"));

                    charItem = GUIUtils.createGuiItem(Material.RED_STAINED_GLASS_PANE, // Ícone de vidro vermelho
                            plugin.getMessage(player, "character.gui.slot_selection.existing_char.name",
                                    charData.getCharacterName(), classSymbol, classDisplayName),
                            lore
                    );
                } else {
                    // Personagem normal
                    lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_play"));
                    lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_delete"));

                    charItem = GUIUtils.createPlayerHead(player,
                            plugin.getMessage(player, "character.gui.slot_selection.existing_char.name",
                                    charData.getCharacterName(), classSymbol, classDisplayName),
                            lore.toArray(new String[0]) // CORREÇÃO AQUI: Converte List<String> para String[]
                    );
                }
                inv.setItem(currentSlot, charItem);
            } else {
                createCharacterSlots.add(currentSlot);
                inv.setItem(currentSlot, GUIUtils.createGuiItem(Material.LIME_STAINED_GLASS,
                        plugin.getMessage(player, "character.gui.slot_selection.new_char.name"),
                        plugin.getMessages(player, "character.gui.slot_selection.new_char.lore")
                ));
            }
        }

        inv.setItem(CLOSE_BUTTON_SLOT, GUIUtils.createGuiItem(Material.BARRIER,
                plugin.getMessage(player, "character.gui.slot_selection.close_button.name"),
                plugin.getMessages(player, "character.gui.slot_selection.close_button.lore")
        ));
    }

    // NOVO: Inicia a tarefa de contagem regressiva
    private void startCountdownTask() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || inv.getViewers().isEmpty()) {
                    cancel();
                    return;
                }
                // Atualiza apenas os itens que são personagens pendentes de exclusão
                boolean updated = false;
                for (int slot : CHARACTER_SLOTS) {
                    if (slotToCharacterMap.containsKey(slot)) {
                        CharacterData charData = slotToCharacterMap.get(slot);
                        if (charData.isPendingDeletion()) {
                            // Recria o item para atualizar a lore do tempo restante
                            String classSymbol = charData.getSelectedClass().getSymbol();
                            String classDisplayName = charData.getSelectedClass().getDisplayName(player);

                            long remainingSeconds = charData.getRemainingDeletionTimeSeconds();
                            long minutes = TimeUnit.SECONDS.toMinutes(remainingSeconds);
                            long seconds = remainingSeconds % 60;

                            List<String> lore = new ArrayList<>();
                            lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_class", classDisplayName));
                            lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_level", String.valueOf(charData.getLevel())));
                            lore.add(""); // Espaçador
                            lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_pending_deletion"));
                            lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_time_left", minutes, seconds));
                            lore.add(""); // Espaçador
                            lore.add(plugin.getMessage(player, "character.gui.slot_selection.existing_char.lore_manage_deletion"));

                            ItemStack updatedItem = GUIUtils.createGuiItem(Material.RED_STAINED_GLASS_PANE,
                                    plugin.getMessage(player, "character.gui.slot_selection.existing_char.name",
                                            charData.getCharacterName(), classSymbol, classDisplayName),
                                    lore
                            );
                            inv.setItem(slot, updatedItem);
                            updated = true;
                        }
                    }
                }
                if (updated) {
                    player.updateInventory(); // Força a atualização visual para o cliente
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Atualiza a cada segundo (20 ticks)
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

        if (slotToCharacterMap.containsKey(slot)) {
            CharacterData selectedChar = slotToCharacterMap.get(slot);
            this.isTransitioning = true;

            if (selectedChar.isPendingDeletion()) {
                // Se o personagem está pendente, abre a GUI de confirmação para gerenciar a exclusão
                clickedPlayer.playSound(clickedPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new CharacterDeletionConfirmGUI(plugin, characterModule, clickedPlayer, selectedChar).open();
            } else {
                // Lógica normal para personagens não pendentes
                if (event.isLeftClick()) {
                    clickedPlayer.playSound(clickedPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    clickedPlayer.closeInventory();
                    characterModule.selectAndLoadCharacter(clickedPlayer, selectedChar.getCharacterId());
                } else if (event.isRightClick()) {
                    clickedPlayer.playSound(clickedPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    // Abre a GUI de confirmação para iniciar a exclusão
                    new CharacterDeletionConfirmGUI(plugin, characterModule, clickedPlayer, selectedChar).open();
                }
            }
            return;
        }

        if (createCharacterSlots.contains(slot)) {
            if (characters.size() >= MAX_CHARACTERS) {
                clickedPlayer.sendMessage(plugin.getMessage(player, "character.error.max_slots_full"));
                clickedPlayer.playSound(clickedPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            this.isTransitioning = true;
            clickedPlayer.playSound(clickedPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new ClassSelectionGUI(plugin, characterModule, this, clickedPlayer).open();
            return;
        }

        if (slot == CLOSE_BUTTON_SLOT) {
            clickedPlayer.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
            if (countdownTask != null) { // NOVO: Cancela a tarefa ao fechar a GUI
                countdownTask.cancel();
                countdownTask = null;
            }
            if (!this.isTransitioning) {
                Optional<CharacterData> activeCharOpt = plugin.getCharacterAPI().getCharacterData((Player) event.getPlayer());
                if (activeCharOpt.isEmpty()) {
                    Player playerToKick = (Player) event.getPlayer();
                    String kickMessage = plugin.getMessage(playerToKick, "character.gui.slot_selection.kick_on_close");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (playerToKick.isOnline()) {
                            playerToKick.kickPlayer(kickMessage);
                        }
                    }, 1L);
                }
            }
            this.isTransitioning = false;
        }
    }
}