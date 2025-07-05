package me.ray.aethelgardRPG.modules.character;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.character.leveling.LevelingManager;
import me.ray.aethelgardRPG.modules.character.display.PlayerStatusDisplayManager;
import me.ray.aethelgardRPG.modules.character.api.CharacterAPI;
import me.ray.aethelgardRPG.modules.character.guis.CharacterSlotSelectionGUI;
import me.ray.aethelgardRPG.modules.character.guis.CharacterDeletionConfirmGUI;
import me.ray.aethelgardRPG.modules.character.guis.PlayerProfileGUI;
import me.ray.aethelgardRPG.modules.character.repository.CharacterRepository;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.character.listeners.*;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import me.ray.aethelgardRPG.core.utils.GUIUtils;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.spell.PlayerSpellManager;
import me.ray.aethelgardRPG.modules.spell.SpellModule;
import me.ray.aethelgardRPG.modules.spell.SpellRegistry;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import me.ray.aethelgardRPG.session.SessionManager;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Registry;
import org.bukkit.GameMode;
import org.bukkit.Location;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CharacterModule implements RPGModule, CharacterAPI {

    private final AethelgardRPG plugin;
    private CharacterRepository characterRepository;
    private SessionManager sessionManager;
    private PlayerStatusDisplayManager statusDisplayManager;
    private LevelingManager levelingManager;
    private BukkitTask baseRegenTask;
    private BukkitTask baseManaRegenTask;
    private BukkitTask baseStaminaRegenerationTask;
    private AccessorySlotListener accessorySlotListener;
    private PlayerToggleSprintListener playerToggleSprintListener;
    public final NamespacedKey PROFILE_COMPASS_KEY;

    private final Map<Integer, BukkitTask> pendingCharacterDeletionTasks = new ConcurrentHashMap<>();

    public CharacterModule(AethelgardRPG plugin) {
        this.plugin = plugin;
        this.PROFILE_COMPASS_KEY = new NamespacedKey(plugin, "profile_compass_item");
    }

    @Override
    public String getName() {
        return "Character";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Character...");
        this.characterRepository = new CharacterRepository(plugin);
        this.sessionManager = plugin.getSessionManager();
        this.statusDisplayManager = new PlayerStatusDisplayManager(plugin, this);
        this.levelingManager = new LevelingManager(plugin);

        // Registrar listeners
        // O novo CharacterPlayerListener lida com os eventos de entrada e saída do jogador.
        plugin.getServer().getPluginManager().registerEvents(new CharacterPlayerListener(plugin, this), plugin);

        // Outros listeners do módulo
        plugin.getServer().getPluginManager().registerEvents(new PlayerRespawnListener(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerProfileItemListener(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerEquipmentListener(plugin, this), plugin);
        this.accessorySlotListener = new AccessorySlotListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerHungerListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(this.accessorySlotListener, plugin);
        this.playerToggleSprintListener = new PlayerToggleSprintListener(plugin, this);
        plugin.getServer().getPluginManager().registerEvents(this.playerToggleSprintListener, plugin);
    }

    @Override
    public void onEnable() {
        if (plugin.getDatabaseManager().isConnected()) {
            characterRepository.createTables();
            schedulePendingDeletionsFromDatabase();
        }
        levelingManager.loadConfig();
        statusDisplayManager.startDisplayTask();
        startBaseRegenerationTask();
        startBaseManaRegenerationTask();
        startBaseStaminaRegenerationTask();
        plugin.getLogger().info("Módulo Character habilitado.");
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info(plugin.getMessage("character.chat.module.saving-data"));
        // O salvamento agora é tratado pelo CharacterPlayerListener ao deslogar,
        // e pelo SessionManager.destroySession() que ele chama.
        statusDisplayManager.stopDisplayTask();
        if (baseRegenTask != null) baseRegenTask.cancel();
        if (baseManaRegenTask != null) baseManaRegenTask.cancel();
        if (baseStaminaRegenerationTask != null) baseStaminaRegenerationTask.cancel();
        if (playerToggleSprintListener != null) playerToggleSprintListener.cancelAllSprintTasks();

        pendingCharacterDeletionTasks.values().forEach(BukkitTask::cancel);
        pendingCharacterDeletionTasks.clear();
        plugin.getLogger().info("Todas as tarefas de exclusão de personagem pendentes foram canceladas.");

        plugin.getLogger().info(plugin.getMessage("character.chat.module.disabled"));
    }

    private void schedulePendingDeletionsFromDatabase() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT character_id, deletion_scheduled_timestamp FROM characters WHERE is_pending_deletion = TRUE")) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int charId = rs.getInt("character_id");
                    Timestamp scheduledTimestamp = rs.getTimestamp("deletion_scheduled_timestamp");
                    if (scheduledTimestamp != null) {
                        LocalDateTime scheduledDateTime = scheduledTimestamp.toLocalDateTime();
                        long remainingMillis = scheduledDateTime.toInstant(ZoneOffset.UTC).toEpochMilli() - System.currentTimeMillis();

                        if (remainingMillis > 0) {
                            long delayTicks = remainingMillis / 50; // 1 tick = 50ms
                            BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                                characterRepository.loadCharacterData(charId).thenAccept(charData -> {
                                    if (charData != null) {
                                        performHardCharacterDeletion(null, charId, charData.getCharacterName());
                                    } else {
                                        plugin.getLogger().warning("Personagem " + charId + " não encontrado para hard delete agendado.");
                                        characterRepository.hardDeleteCharacter(charId);
                                    }
                                });
                                pendingCharacterDeletionTasks.remove(charId);
                            }, delayTicks);
                            pendingCharacterDeletionTasks.put(charId, task);
                            plugin.getLogger().info("Exclusão do personagem " + charId + " reagendada para daqui a " + (remainingMillis / 1000) + " segundos.");
                        } else {
                            plugin.getLogger().info("Exclusão do personagem " + charId + " atrasada, executando agora.");
                            characterRepository.loadCharacterData(charId).thenAccept(charData -> {
                                if (charData != null) {
                                    performHardCharacterDeletion(null, charId, charData.getCharacterName());
                                } else {
                                    plugin.getLogger().warning("Personagem " + charId + " não encontrado para hard delete imediato.");
                                    characterRepository.hardDeleteCharacter(charId);
                                }
                            });
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao carregar exclusões pendentes do banco de dados.", e);
            }
        });
    }

    public void openCharacterSelectionFor(Player player, boolean forceOpenGUI) {
        characterRepository.findCharactersByAccount(player.getUniqueId()).thenAccept(characters -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String playerPreferredLang = plugin.getLanguageManager().getDefaultLang();
                if (!characters.isEmpty()) {
                    playerPreferredLang = characters.stream()
                            .filter(c -> c.getLanguagePreference() != null && !c.getLanguagePreference().isEmpty())
                            .map(CharacterData::getLanguagePreference)
                            .findFirst()
                            .orElse(plugin.getLanguageManager().getDefaultLang());
                }
                plugin.getLanguageManager().setPlayerLanguage(player, playerPreferredLang);

                if (forceOpenGUI || !plugin.getSessionManager().getActiveCharacter(player).isPresent()) {
                    player.sendMessage(plugin.getMessage(player, "character.first_login_prompt"));
                    new CharacterSlotSelectionGUI(plugin, this, player, characters).open();
                    player.setGameMode(GameMode.ADVENTURE);
                } else {
                    allowPlayerToJoin(player);
                }
            });
        });
    }

    public void openCharacterSelectionFor(Player player) {
        openCharacterSelectionFor(player, false);
    }

    public void selectAndLoadCharacter(Player player, int characterId) {
        plugin.getPerformanceMonitor().startTiming("characterLoad." + player.getName());
        characterRepository.loadCharacterData(characterId).thenAccept(characterData -> {
            plugin.getPerformanceMonitor().stopTiming("characterLoad." + player.getName());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (characterData == null) {
                    player.kickPlayer(plugin.getMessage(player, "character.error.load_failed"));
                    return;
                }
                sessionManager.setActiveCharacter(player, characterData);
                characterRepository.updateLastActiveCharacter(player.getUniqueId(), characterId);

                plugin.getLanguageManager().clearPlayerLanguageCache(player);

                if (characterData.getLastLocation() != null) {
                    player.teleport(characterData.getLastLocation());
                } else {
                    player.teleport(player.getWorld().getSpawnLocation());
                }
                applyCharacterDataToPlayer(player, characterData);
                giveOrUpdateProfileCompass(player);
                player.sendMessage(plugin.getMessage(player, "character.chat.welcome_back", characterData.getCharacterName()));
                allowPlayerToJoin(player);

                CustomMobModule customMobModule = plugin.getModuleManager().getModule(CustomMobModule.class);
                if (customMobModule != null && plugin.getModuleManager().isModuleEnabled(CustomMobModule.class)) {
                    customMobModule.updateAllMobDisplaysForPlayer(player);
                    plugin.getLogger().info("Updating mob displays for " + player.getName() + " with saved language.");
                }
            });
        });
    }

    public CompletableFuture<Integer> createNewCharacter(Player player, String characterName, RPGClass selectedClass) {
        Location startLoc = player.getLocation();
        return characterRepository.createCharacter(player.getUniqueId(), characterName, selectedClass, startLoc);
    }

    public void allowPlayerToJoin(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(plugin.getMessage(player, "character.joined_game"));
    }

    private void applyCharacterDataToPlayer(Player player, CharacterData data) {
        // Usa os arrays de ItemStack diretamente
        player.getInventory().setContents(data.getInventoryContents());
        player.getInventory().setArmorContents(data.getArmorContents());
        updatePlayerAttributesFromEquipment(player);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExp(0);
        player.setLevel(data.getLevel());
    }

    public void scheduleCharacterDeletion(Player player, CharacterData characterData) {
        FileConfiguration charSettings = plugin.getConfigManager().getCharacterSettingsConfig();
        boolean delayedDeletionEnabled = charSettings.getBoolean("character-deletion.delayed-deletion-enabled", true);
        int minLevelForDelay = charSettings.getInt("character-deletion.min-level-for-delay", 20);
        long delayInMinutes = charSettings.getLong("character-deletion.delay-in-minutes", 10);

        long finalDelayMillis = delayInMinutes * 60 * 1000;
        LocalDateTime scheduledTimestamp = LocalDateTime.now().plusMinutes(delayInMinutes);

        if (characterData.isPendingDeletion()) {
            player.sendMessage(plugin.getMessage(player, "character.chat.deletion.already-pending"));
            return;
        }

        if (delayedDeletionEnabled && characterData.getLevel() >= minLevelForDelay) {
            characterRepository.softDeleteAndSchedule(characterData.getCharacterId(), scheduledTimestamp).thenRun(() -> {
                characterData.setPendingDeletion(true);
                characterData.setDeletionScheduledTimestamp(scheduledTimestamp);

                BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    performHardCharacterDeletion(player, characterData.getCharacterId(), characterData.getCharacterName());
                    pendingCharacterDeletionTasks.remove(characterData.getCharacterId());
                }, finalDelayMillis / 50);
                pendingCharacterDeletionTasks.put(characterData.getCharacterId(), task);

                player.sendMessage(plugin.getMessage(player, "character.chat.deletion.scheduled",
                        characterData.getCharacterName(), delayInMinutes));
                openCharacterSelectionFor(player, true);
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Erro ao agendar exclusão do personagem " + characterData.getCharacterName(), e);
                player.sendMessage(plugin.getMessage(player, "character.chat.deletion.error-processing"));
                return null;
            });
        } else {
            player.sendMessage(plugin.getMessage(player, "character.chat.deletion.immediate", characterData.getCharacterName()));
            performHardCharacterDeletion(player, characterData.getCharacterId(), characterData.getCharacterName());
        }
    }

    public void performHardCharacterDeletion(Player player, int characterId, String characterName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            characterRepository.hardDeleteCharacter(characterId).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player != null && player.isOnline()) {
                        Optional<CharacterData> activeChar = plugin.getSessionManager().getActiveCharacter(player);
                        if (activeChar.isPresent() && activeChar.get().getCharacterId() == characterId) {
                            plugin.getSessionManager().removeSession(player);
                            player.kickPlayer(plugin.getMessage(player, "character.chat.deletion.kicked", characterName));
                        } else {
                            player.sendMessage(plugin.getMessage(player, "character.chat.deletion.completed", characterName));
                            openCharacterSelectionFor(player, true);
                        }
                    } else {
                        plugin.getLogger().info("Exclusão permanente do personagem " + characterName + " (ID: " + characterId + ") concluída.");
                    }
                });
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Erro ao realizar hard delete do personagem " + characterId + ": " + e.getMessage(), e);
                if (player != null && player.isOnline()) {
                    player.sendMessage(plugin.getMessage(player, "character.chat.deletion.error-processing"));
                }
                return null;
            });
        });
    }

    public boolean cancelCharacterDeletion(Player player, CharacterData characterData) {
        if (!characterData.isPendingDeletion()) {
            player.sendMessage(plugin.getMessage(player, "character.chat.deletion.no-pending"));
            return false;
        }

        BukkitTask task = pendingCharacterDeletionTasks.remove(characterData.getCharacterId());
        if (task != null) {
            task.cancel();
        }

        characterRepository.restoreCharacter(characterData.getCharacterId()).thenRun(() -> {
            characterData.setPendingDeletion(false);
            characterData.setDeletionScheduledTimestamp(null);

            player.sendMessage(plugin.getMessage(player, "character.chat.deletion.cancelled"));
            openCharacterSelectionFor(player, true);
        }).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Erro ao restaurar personagem " + characterData.getCharacterId() + " após cancelamento: " + e.getMessage(), e);
            player.sendMessage(plugin.getMessage(player, "character.chat.deletion.error-processing"));
            return null;
        });
        return true;
    }

    @Override
    public Optional<CharacterData> getCharacterData(Player player) {
        return sessionManager.getActiveCharacter(player);
    }

    @Override
    public Optional<CharacterData> getCharacterData(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        return (player != null) ? getCharacterData(player) : Optional.empty();
    }

    public CompletableFuture<Void> saveActiveCharacterData(Player player) {
        return getCharacterData(player)
                .map(this::saveCharacterDataAsync)
                .orElse(CompletableFuture.completedFuture(null));
    }

    @Override
    public CompletableFuture<Void> saveCharacterDataAsync(CharacterData characterData) {
        if (characterData == null) {
            return CompletableFuture.completedFuture(null);
        }
        Player player = Bukkit.getPlayer(characterData.getAccountUuid());
        if (player != null) {
            characterData.setLastLocation(player.getLocation());
        }
        return characterRepository.saveCharacterData(characterData);
    }

    public void openCharacterCreationGUI(Player player) {
        plugin.getLogger().info("Abrindo GUI de criação de personagem para: " + player.getName());
    }

    public void openCharacterSelectionGUI(Player player, List<CharacterData> characters) {
        plugin.getLogger().info("Abrindo GUI de seleção de personagem para: " + player.getName());
    }

    @Override
    public int getPlayerLevel(Player player) {
        return getCharacterData(player).map(CharacterData::getLevel).orElse(0);
    }

    @Override
    public Optional<RPGClass> getPlayerClass(Player player) {
        return getCharacterData(player).map(CharacterData::getSelectedClass);
    }

    @Override
    public void addExperience(Player player, double amount) {
        if (amount <= 0) return;

        getCharacterData(player).ifPresent(characterData -> {
            if (characterData.getLevel() >= levelingManager.getMaxLevel()) {
                player.sendMessage(plugin.getMessage(player, "character.xp-max-level"));
                return;
            }
            characterData.setExperience(characterData.getExperience() + amount);
            player.sendMessage(plugin.getMessage(player, "character.xp-gain", String.format("%.1f", amount)));
            checkPlayerLevelUp(player, characterData);
        });
    }

    private void checkPlayerLevelUp(Player player, CharacterData characterData) {
        boolean leveledUp = false;
        while (characterData.getLevel() < levelingManager.getMaxLevel() &&
                characterData.getExperience() >= levelingManager.getXpForNextLevel(characterData.getLevel())) {

            double xpNeeded = levelingManager.getXpForNextLevel(characterData.getLevel());
            characterData.setExperience(characterData.getExperience() - xpNeeded);
            characterData.setLevel(characterData.getLevel() + 1);
            int pointsGained = levelingManager.getAttributePointsPerLevel();
            characterData.setAttributePoints(characterData.getAttributePoints() + pointsGained);
            player.sendMessage(plugin.getMessage(player, "character.level-up", characterData.getLevel()));
            player.sendMessage(plugin.getMessage(player, "character.attribute-points-gained", pointsGained));
            playLevelUpEffects(player, characterData.getLevel());
            leveledUp = true;
        }
        if (leveledUp) {
            checkForNewSpellsOnLevelUp(player, characterData);
            if (statusDisplayManager != null) statusDisplayManager.clearPlayerFromCache(player.getUniqueId());
        }
    }

    private void playLevelUpEffects(Player player, int newLevel) {
        FileConfiguration config = plugin.getConfigManager().getCharacterSettingsConfig();
        String basePath = "leveling.effects.";

        if (config.getBoolean(basePath + "title.enabled", true)) {
            String titleText = plugin.getMessage(player, "character.leveling.title", newLevel);
            String subtitleText = plugin.getMessage(player, "character.leveling.subtitle", newLevel);
            int fadeIn = config.getInt(basePath + "title.fade-in-ticks", 10);
            int stay = config.getInt(basePath + "title.stay-ticks", 70);
            int fadeOut = config.getInt(basePath + "title.fade-out-ticks", 20);
            player.sendTitle(titleText, subtitleText, fadeIn, stay, fadeOut);
        }

        if (config.getBoolean(basePath + "sound.enabled", true)) {
            String soundName = config.getString(basePath + "sound.name", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) config.getDouble(basePath + "sound.volume", 1.0);
            float pitch = (float) config.getDouble(basePath + "sound.pitch", 1.0);
            try {
                Sound sound;
                NamespacedKey soundKey = NamespacedKey.minecraft(soundName.toLowerCase().replace('.', '_'));
                sound = Registry.SOUNDS.get(soundKey);

                if (sound == null) {
                    plugin.getLogger().warning(plugin.getMessage(player,"character.leveling.invalid-sound-config", soundName));
                    sound = Sound.ENTITY_PLAYER_LEVELUP;
                }
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getMessage("character.leveling.invalid-sound-config", soundName));
            }
        }

        if (config.getBoolean(basePath + "particles.enabled", true)) {
            String particleName = config.getString(basePath + "particles.type", "TOTEM_OF_UNDYING");
            int count = config.getInt(basePath + "particles.count", 50);
            double offsetX = config.getDouble(basePath + "particles.offset-x", 0.5);
            double offsetY = config.getDouble(basePath + "particles.offset-y", 1.0);
            double offsetZ = config.getDouble(basePath + "particles.offset-z", 0.5);
            double speed = config.getDouble(basePath + "particles.speed", 0.1);
            try {
                Particle particle = Particle.valueOf(particleName.toUpperCase());
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, offsetX, offsetY, offsetZ, speed);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(plugin.getMessage("character.leveling.invalid-particle-config", particleName));
            }
        }
    }

    public void checkForNewSpellsOnLevelUp(Player player, CharacterData characterData) {
        SpellModule spellModule = plugin.getModuleManager().getModule(SpellModule.class);
        if (spellModule == null) {
            return;
        }

        SpellRegistry spellRegistry = spellModule.getSpellRegistry();
        PlayerSpellManager playerSpellManager = spellModule.getPlayerSpellManager();
        RPGClass playerClass = characterData.getSelectedClass();
        int playerLevel = characterData.getLevel();

        for (Spell spell : spellRegistry.getAllSpells().values()) {
            if (spell.getRequiredRPGClass() == playerClass
                    && playerLevel >= spell.getRequiredLevel()
                    && !characterData.getKnownSpellIds().contains(spell.getId())) {
                playerSpellManager.learnSpell(player, spell.getId());
            }
        }
    }

    public void giveOrUpdateProfileCompass(Player player) {
        plugin.getLogger().fine("[COMPASS] Tentando dar bússola para: " + player.getName());
        FileConfiguration charSettings = plugin.getConfigManager().getCharacterSettingsConfig();
        if (!charSettings.getBoolean("profile_compass.enabled", false)) {
            plugin.getLogger().fine("[COMPASS] Bússola desabilitada na config.");
            return;
        }

        int compassSlot = charSettings.getInt("profile_compass.slot", 8);
        if (compassSlot < 0 || compassSlot > 8) {
            plugin.getLogger().warning(plugin.getMessage(player, "character.profile_compass.invalid-slot-config", compassSlot));
            compassSlot = 8;
        }

        String compassName = plugin.getMessage(player, "character.profile_compass.name");
        List<String> compassLore = plugin.getMessages(player, "character.profile_compass.lore");

        ItemStack compass = new GUIUtils.Builder(Material.COMPASS)
                .withName(compassName)
                .withLore(compassLore)
                .build();

        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(PROFILE_COMPASS_KEY, PersistentDataType.BYTE, (byte) 1);
            compass.setItemMeta(meta);
        }

        player.getInventory().setItem(compassSlot, compass);
        if (this.accessorySlotListener != null) {
            this.accessorySlotListener.refreshAccessoryPlaceholders(player);
        }
    }

    public void updatePlayerAttributesFromEquipment(Player player) {
        getCharacterData(player).ifPresent(characterData -> {
            PlayerAttributes attributes = characterData.getAttributes();
            me.ray.aethelgardRPG.modules.item.ItemModule itemModule = plugin.getModuleManager().getModule(me.ray.aethelgardRPG.modules.item.ItemModule.class);
            if (itemModule == null) {
                plugin.getLogger().warning(plugin.getMessage("character.equipment.itemmodule-not-found", player.getName()));
                return;
            }

            attributes.clearItemBonuses();

            List<ItemStack> equippedItems = new ArrayList<>();
            PlayerInventory inventory = player.getInventory();
            if (inventory.getHelmet() != null) equippedItems.add(inventory.getHelmet());
            if (inventory.getChestplate() != null) equippedItems.add(inventory.getChestplate());
            if (inventory.getLeggings() != null) equippedItems.add(inventory.getLeggings());
            if (inventory.getBoots() != null) equippedItems.add(inventory.getBoots());

            FileConfiguration charSettings = plugin.getConfigManager().getCharacterSettingsConfig();
            if (charSettings.getBoolean("accessory_slots.enabled", false)) {
                List<Integer> accessorySlots = charSettings.getIntegerList("accessory_slots.reserved");
                for (int slot : accessorySlots) {
                    if (inventory.getItem(slot) != null) equippedItems.add(inventory.getItem(slot));
                }
            }

            attributes.applyItemBonuses(equippedItems, itemModule);
            plugin.getLogger().fine(plugin.getMessage(player, "character.chat.equipment.attributes-updated", player.getName()));
            if (this.accessorySlotListener != null) {
                this.accessorySlotListener.refreshAccessoryPlaceholders(player);
            }
        });
    }

    public void resetAttributesAndRefundPoints(Player player) {
        getCharacterData(player).ifPresent(characterData -> {
            PlayerAttributes attributes = characterData.getAttributes();
            RPGClass playerClass = characterData.getSelectedClass();

            if (playerClass == null || playerClass == RPGClass.NONE) {
                plugin.getLogger().warning(plugin.getMessage("character.attributes.reset-fail-invalid-data", player.getName()));
                return;
            }

            int refundedPoints = getRefundableAttributePoints(characterData);

            // *** ALTERAÇÃO AQUI ***
            // Reseta todos os atributos base, incluindo as defesas.
            attributes.setBaseStrength(playerClass.getBaseStrength());
            attributes.setBaseIntelligence(playerClass.getBaseIntelligence());
            attributes.setBaseFaith(playerClass.getBaseFaith());
            attributes.setBaseDexterity(playerClass.getBaseDexterity());
            attributes.setBaseAgility(playerClass.getBaseAgility());
            attributes.setBasePhysicalDefense(playerClass.getBasePhysicalDefense());
            attributes.setBaseMagicalDefense(playerClass.getBaseMagicalDefense());

            characterData.setAttributePoints(characterData.getAttributePoints() + refundedPoints);
            saveCharacterDataAsync(characterData);
            updatePlayerAttributesFromEquipment(player);
        });
    }

    public int getRefundableAttributePoints(CharacterData characterData) {
        if (characterData == null) return 0;

        PlayerAttributes attributes = characterData.getAttributes();
        RPGClass playerClass = characterData.getSelectedClass();

        if (attributes == null || playerClass == null || playerClass == RPGClass.NONE) {
            return 0;
        }

        int refundedPoints = 0;
        refundedPoints += (attributes.getBaseStrength() - playerClass.getBaseStrength());
        refundedPoints += (attributes.getBaseIntelligence() - playerClass.getBaseIntelligence());
        refundedPoints += (attributes.getBaseFaith() - playerClass.getBaseFaith());
        refundedPoints += (attributes.getBaseDexterity() - playerClass.getBaseDexterity());
        refundedPoints += (attributes.getBaseAgility() - playerClass.getBaseAgility());

        return Math.max(0, refundedPoints);
    }

    public void refreshAccessoryPlaceholders(Player player) {
        if (this.accessorySlotListener != null) {
            this.accessorySlotListener.refreshAccessoryPlaceholders(player);
        }
    }

    public void reloadCharacterConfigs() {
        if (levelingManager != null) levelingManager.loadConfig();
        if (statusDisplayManager != null) statusDisplayManager.reloadConfig();
        startBaseRegenerationTask();
        startBaseManaRegenerationTask();
        startBaseStaminaRegenerationTask();
    }

    private void startBaseRegenerationTask() {
        if (baseRegenTask != null) baseRegenTask.cancel();

        FileConfiguration charSettings = plugin.getConfigManager().getCharacterSettingsConfig();
        boolean regenEnabled = charSettings.getBoolean("base-regeneration.enabled", true);
        if (!regenEnabled) {
            plugin.getLogger().info(plugin.getMessage("character.chat.regeneration.health-disabled"));
            return;
        } else {
            plugin.getLogger().info(plugin.getMessage("character.chat.regeneration.health-started"));
        }

        long interval = charSettings.getLong("base-regeneration.interval-ticks", 100L);
        double amount = charSettings.getDouble("base-regeneration.amount", 1.0);
        boolean onlyOutOfCombat = charSettings.getBoolean("base-regeneration.only-out-of-combat", true);
        long outOfCombatThresholdMillis = charSettings.getLong("base-regeneration.out-of-combat-threshold-seconds", 10L) * 1000L;

        baseRegenTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    getCharacterData(player).ifPresent(characterData -> {
                        PlayerAttributes attributes = characterData.getAttributes();
                        if (attributes.getCurrentHealth() >= attributes.getMaxHealth()) return;

                        if (onlyOutOfCombat && (System.currentTimeMillis() - characterData.getLastCombatTimeMillis() < outOfCombatThresholdMillis)) {
                            return;
                        }

                        double newHealth = Math.min(attributes.getMaxHealth(), attributes.getCurrentHealth() + amount);
                        attributes.setCurrentHealth(newHealth);

                        if (attributes.getMaxHealth() > 0) {
                            double healthPercentage = newHealth / attributes.getMaxHealth();
                            double vanillaMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                            player.setHealth(Math.max(0.001, Math.min(vanillaMaxHealth, vanillaMaxHealth * healthPercentage)));
                        }
                    });
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void startBaseManaRegenerationTask() {
        if (baseManaRegenTask != null) baseManaRegenTask.cancel();

        FileConfiguration charSettings = plugin.getConfigManager().getCharacterSettingsConfig();
        String basePath = "base-mana-regeneration.";
        boolean regenEnabled = charSettings.getBoolean(basePath + "enabled", true);
        if (!regenEnabled) {
            plugin.getLogger().info(plugin.getMessage("character.chat.regeneration.mana-disabled"));
            return;
        } else {
            plugin.getLogger().info(plugin.getMessage("character.chat.regeneration.mana-started"));
        }

        long interval = charSettings.getLong(basePath + "interval-ticks", 80L);
        double amount = charSettings.getDouble(basePath + "amount", 1.5);
        boolean onlyOutOfCombat = charSettings.getBoolean(basePath + "only-out-of-combat", false);
        long outOfCombatThresholdMillis = charSettings.getLong(basePath + "out-of-combat-threshold-seconds", 5L) * 1000L;

        baseManaRegenTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    getCharacterData(player).ifPresent(characterData -> {
                        PlayerAttributes attributes = characterData.getAttributes();
                        if (attributes.getCurrentMana() >= attributes.getMaxMana()) return;

                        if (onlyOutOfCombat && (System.currentTimeMillis() - characterData.getLastCombatTimeMillis() < outOfCombatThresholdMillis)) {
                            return;
                        }
                        attributes.setCurrentMana(Math.min(attributes.getMaxMana(), attributes.getCurrentMana() + amount));
                    });
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void startBaseStaminaRegenerationTask() {
        if (baseStaminaRegenerationTask != null) baseStaminaRegenerationTask.cancel();

        FileConfiguration charSettings = plugin.getConfigManager().getCharacterSettingsConfig();
        String basePath = "base-stamina-regeneration.";
        boolean regenEnabled = charSettings.getBoolean(basePath + "enabled", true);
        if (!regenEnabled) {
            plugin.getLogger().info(plugin.getMessage("character.chat.regeneration.stamina-disabled"));
            return;
        } else {
            plugin.getLogger().info(plugin.getMessage("character.chat.regeneration.stamina-started"));
        }

        long interval = charSettings.getLong(basePath + "interval-ticks", 40L);
        double amount = charSettings.getDouble(basePath + "amount", 5.0);
        boolean onlyOutOfCombat = charSettings.getBoolean(basePath + "only-out-of-combat", true);
        long outOfCombatThresholdMillis = charSettings.getLong(basePath + "out-of-combat-threshold-seconds", 3L) * 1000L;
        boolean onlyOutOfSprint = charSettings.getBoolean(basePath + "only-out-of-sprint", true);
        long outOfSprintThresholdMillis = charSettings.getLong(basePath + "out-of-sprint-threshold-seconds", 1L) * 1000L;

        baseStaminaRegenerationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    getCharacterData(player).ifPresent(characterData -> {
                        PlayerAttributes attributes = characterData.getAttributes();
                        if (attributes.getCurrentStamina() >= attributes.getMaxStamina()) return;

                        if (onlyOutOfCombat && (System.currentTimeMillis() - characterData.getLastCombatTimeMillis() < outOfCombatThresholdMillis)) {
                            return;
                        }
                        if (onlyOutOfSprint && (System.currentTimeMillis() - characterData.getLastSprintTimeMillis() < outOfSprintThresholdMillis)) {
                            return;
                        }
                        attributes.setCurrentStamina(Math.min(attributes.getMaxStamina(), attributes.getCurrentStamina() + amount));
                    });
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    // --- Getters ---
    public AccessorySlotListener getAccessorySlotListener() {
        return accessorySlotListener;
    }

    public PlayerStatusDisplayManager getStatusDisplayManager() {
        return statusDisplayManager;
    }

    public LevelingManager getLevelingManager() {
        return levelingManager;
    }

    public CharacterRepository getCharacterRepository() {
        return characterRepository;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}