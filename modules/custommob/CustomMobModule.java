package me.ray.aethelgardRPG.modules.custommob;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager;
import me.ray.aethelgardRPG.modules.custommob.abilities.MobAbilityManager;
import me.ray.aethelgardRPG.modules.custommob.api.CustomMobAPI;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.PlayerData;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import me.ray.aethelgardRPG.modules.custommob.listeners.EntityDamageListenerMob;
import me.ray.aethelgardRPG.modules.custommob.listeners.EntitySpawnListener;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import me.ray.aethelgardRPG.modules.custommob.listeners.CustomMobDeathListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class CustomMobModule implements RPGModule, CustomMobAPI {

    private final AethelgardRPG plugin;
    private CustomMobManager customMobManager;
    private MobAbilityManager mobAbilityManager;
    private BukkitTask combatStateCheckTask;
    private BukkitTask displayUpdateTask;

    // Format for the defenses string
    private String defensesFormat;


    public CustomMobModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CustomMob";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo CustomMob...");
        this.customMobManager = new CustomMobManager(plugin, this);
        this.mobAbilityManager = new MobAbilityManager(plugin);

        plugin.getServer().getPluginManager().registerEvents(new EntitySpawnListener(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new EntityDamageListenerMob(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CustomMobDeathListener(plugin, this), plugin);
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo CustomMob habilitado.");
        loadDisplayConfig();
        customMobManager.loadCustomMobConfigurations();
        mobAbilityManager.loadAbilityConfigurations();
        startCombatStateCheckTask();
        startDisplayUpdateTask();
        cleanupOrphanedCustomMobs();
        cleanupOrphanedDisplayArmorStands();
    }

    private void loadDisplayConfig() {
        FileConfiguration mobSettings = plugin.getConfigManager().getCustomMobSettingsConfig();
        // Health bar y-offset and show duration are no longer needed as it's part of the custom name.
        defensesFormat = mobSettings.getString("display.defenses.format", "&7[&bDefP&f: {phys} &7| &dDefM&f: {mag}&7]");
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo CustomMob desabilitado.");
        stopCombatStateCheckTask();
        stopDisplayUpdateTask();

        if (customMobManager != null) {
            List<UUID> activeMobUUIDs = new ArrayList<>(customMobManager.getActiveCustomMobs().keySet());
            for (UUID mobUUID : activeMobUUIDs) {
                ActiveMobInfo mobInfo = customMobManager.getActiveMobInfo(mobUUID);
                // No specific display to remove as it's on the entity name,
                // but ensure custom name is cleared if desired on disable.
                if (mobInfo != null) { 
                    removeMobDisplays(mobInfo);
                }
                Entity mobEntity = Bukkit.getEntity(mobUUID);
                if (mobEntity != null && mobEntity.isValid()) {
                    mobEntity.remove();
                }
                customMobManager.removeActiveMob(mobUUID);
            }
            plugin.getLogger().info(activeMobUUIDs.size() + " mobs customizados foram processados para remoção.");
        }
    }

    private void cleanupOrphanedCustomMobs() {
        if (customMobManager == null) return;
        plugin.getLogger().info("Verificando mobs customizados órfãos...");
        int removedCount = 0;
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (customMobManager.isCustomMob(entity)) {
                    if (customMobManager.getActiveMobInfo(entity.getUniqueId()) == null) {
                        plugin.getLogger().warning("Encontrado mob customizado órfão: " + entity.getUniqueId() + " (" + entity.getType() + "). Removendo...");
                        entity.remove();
                        removedCount++;
                    }
                }
            }
        }
    }

    @Override
    public CustomMobManager getCustomMobManager() {
        return customMobManager;
    }

    public MobAbilityManager getMobAbilityManager() {
        return mobAbilityManager;
    }

    private String generateMobDisplayName(ActiveMobInfo mobInfo) {
        String baseName = mobInfo.getBaseData().getDisplayName();
        int level = mobInfo.getBaseData().getLevel();
        return ChatColor.translateAlternateColorCodes('&', baseName) + " [Lvl " + level + "]";
    }

    private String formatHealthBar(double currentHealth, double maxHealth) {
        if (maxHealth <= 0) return "";
        FileConfiguration mobSettings = plugin.getConfigManager().getCustomMobSettingsConfig();
        int barLength = mobSettings.getInt("display.health-bar.length", 20);
        String charFilled = mobSettings.getString("display.health-bar.char-filled", "❚");
        String charEmpty = mobSettings.getString("display.health-bar.char-empty", "❚");
        String colorFilled = mobSettings.getString("display.health-bar.color-filled", "&a");
        String colorEmpty = mobSettings.getString("display.health-bar.color-empty", "&c");
        String colorBrackets = mobSettings.getString("display.health-bar.color-brackets", "&7");
        String colorHpText = mobSettings.getString("display.health-bar.color-hp-text", "&e");

        int filledCount = (int) Math.round((currentHealth / maxHealth) * barLength);
        if (currentHealth > 0 && filledCount == 0) filledCount = 1;
        if (currentHealth <= 0) filledCount = 0;

        StringBuilder bar = new StringBuilder();
        bar.append(colorBrackets).append("[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filledCount ? colorFilled : colorEmpty).append(i < filledCount ? charFilled : charEmpty);
        }
        bar.append(colorBrackets).append("]");
        bar.append(" ").append(colorHpText).append(String.format("%.0f/%.0f", Math.max(0, currentHealth), maxHealth));
        return ChatColor.translateAlternateColorCodes('&', bar.toString());
    }

    private String formatDefenseString(ActiveMobInfo mobInfo) {
        if (mobInfo == null || mobInfo.getBaseData() == null) {
            return "";
        }
        CustomMobData data = mobInfo.getBaseData();
        String formatted = defensesFormat
                .replace("{phys}", String.format("%.0f", data.getPhysicalDefense()))
                .replace("{mag}", String.format("%.0f", data.getMagicalDefense()));
        return ChatColor.translateAlternateColorCodes('&', formatted);
    }

    public void removeMobDisplays(ActiveMobInfo mobInfo) {
        // No ArmorStands to remove.
        // If we want to clear the custom name on removal, we'd need the entity.
        // This method might become obsolete or just clear mobInfo fields if any were display-related.
    }


    public void updateMobDisplay(LivingEntity mobEntity, ActiveMobInfo mobInfo) {
        final String mobIdShort = (mobEntity != null ? mobEntity.getUniqueId().toString().substring(0, 6) : "mob_null");
        final String debugPrefix = "[UPDATE_DISPLAY] " + mobIdShort + ": ";
        // Log inicial para ver o estado ao entrar no método
        CustomMobData baseDataForLog = mobInfo != null ? mobInfo.getBaseData() : null;
        plugin.getLogger().info(debugPrefix + "Entrando. Vida: " + (mobInfo != null ? String.format("%.1f", mobInfo.getCurrentHealth()) : "N/A") + "/" + (baseDataForLog != null ? String.format("%.1f", baseDataForLog.getMaxHealth()) : "N/A") + ", EmCombate: " + (mobInfo != null ? mobInfo.isInCombat() : "N/A"));

        if (mobEntity == null || !mobEntity.isValid()) {
            plugin.getLogger().warning(debugPrefix + "MobEntity nulo/inválido. Limpando displays associados a mobInfo.");
            // No specific display to remove from mobInfo as it's all on the entity name now.
            return;
        }

        if (mobInfo == null) {
            plugin.getLogger().warning(debugPrefix + "MobInfo nulo. Removendo custom name da entidade.");
            mobEntity.setCustomNameVisible(false);
            mobEntity.setCustomName("");
            // No ArmorStands to clean up for this mob.
            return;
        }
        // Se o mobInfo não for nulo, mas o baseData for, isso é um erro de lógica interna.
        if (mobInfo.getBaseData() == null) {
            plugin.getLogger().severe(debugPrefix + "MobInfo não é nulo, mas BaseData é nulo! Isso não deveria acontecer. Mob ID: " + mobEntity.getUniqueId());
            mobEntity.setCustomNameVisible(false); // Esconde qualquer nome
            mobEntity.setCustomName("");
            return;
        }

        if (mobInfo.getBaseData().isPlayerSummonedMinion()) {
            // Minions only show their name and level.
            String minionName = generateMobDisplayName(mobInfo);
            if (!minionName.equals(mobEntity.getCustomName())) {
                mobEntity.setCustomName(minionName);
            }
            mobEntity.setCustomNameVisible(true);
            return;
        }

        // --- Non-Minion Logic ---
        String newCustomName;
        if (mobInfo.isInCombat()) {
            newCustomName = formatHealthBar(mobInfo.getCurrentHealth(), mobInfo.getBaseData().getMaxHealth());
            plugin.getLogger().fine(debugPrefix + "Em combate. Nome: " + newCustomName);
        } else {
            String namePart = generateMobDisplayName(mobInfo);
            String defensePart = formatDefenseString(mobInfo);
            newCustomName = namePart + (defensePart.isEmpty() ? "" : " " + defensePart);
            plugin.getLogger().fine(debugPrefix + "Fora de combate. Nome: " + newCustomName);
        }

        if (!newCustomName.equals(mobEntity.getCustomName())) {
            mobEntity.setCustomName(newCustomName);
        }
        mobEntity.setCustomNameVisible(true);

        // Lógica de Regeneração Fora de Combate (apenas para não-lacaios)
        if (!mobInfo.getBaseData().isPlayerSummonedMinion() && !mobInfo.isInCombat()) {
            CustomMobData mobData = mobInfo.getBaseData();
            if (mobData.canOutOfCombatRegen() && mobInfo.getCurrentHealth() < mobData.getMaxHealth()) {
                long regenIntervalMillis = (long) mobData.getOutOfCombatRegenIntervalTicks() * 50L; // 50ms per tick
                long timeSinceLastRegen = System.currentTimeMillis() - mobInfo.getLastRegenTimeMillis();

                plugin.getLogger().info(debugPrefix + "Verificando Regen: vidaAtual=" + String.format("%.1f", mobInfo.getCurrentHealth()) + ", vidaMax=" + String.format("%.1f", mobData.getMaxHealth()) +
                        ", intervaloTicks=" + mobData.getOutOfCombatRegenIntervalTicks() + " (" + regenIntervalMillis + "ms)" +
                        ", ultimoRegenTimestamp=" + mobInfo.getLastRegenTimeMillis() + ", tempoDesdeUltimoRegen=" + timeSinceLastRegen + "ms" +
                        ", condicaoAtendida=" + (timeSinceLastRegen >= regenIntervalMillis));

                if (timeSinceLastRegen >= regenIntervalMillis) {
                    double regenAmountValue;
                    if (mobData.isOutOfCombatRegenPercentage()) {
                        regenAmountValue = mobData.getMaxHealth() * mobData.getOutOfCombatRegenAmount();
                    } else {
                        regenAmountValue = mobData.getOutOfCombatRegenAmount();
                    }
                    double regenAmount = regenAmountValue;
                    plugin.getLogger().info(debugPrefix + "Calculo Regen: isPercentage=" + mobData.isOutOfCombatRegenPercentage() + ", configAmount=" + mobData.getOutOfCombatRegenAmount() + ", actualRegenAmount=" + String.format("%.2f", regenAmount));

                    double newHealth = Math.min(mobInfo.getCurrentHealth() + regenAmount, mobData.getMaxHealth());

                    // Atualiza o tempo da última regeneração ANTES de chamar setCustomMobCurrentHealth
                    mobInfo.setLastRegenTimeMillis(System.currentTimeMillis()); 
                    // Usar setCustomMobCurrentHealth para garantir que a vida vanilla também seja atualizada
                    // e que o display seja notificado (embora o display não mude para mobs fora de combate)
                    setCustomMobCurrentHealth(mobEntity.getUniqueId(), newHealth);
                    plugin.getLogger().info(debugPrefix + "Mob regenerou " + String.format("%.2f", regenAmount) + " de vida. Nova vida: " + String.format("%.1f", newHealth) + ". Timestamp do regen: " + mobInfo.getLastRegenTimeMillis());
                }
            }
        }
    }


    private NamespacedKey getDisplayArmorStandOwnerKey() {
        return new NamespacedKey(plugin, "display_as_owner_mob_uuid");
    }
    private NamespacedKey getDisplayArmorStandTypeKey() {
        return new NamespacedKey(plugin, "display_as_type"); // "name" ou "health"
    }


    private void cleanupOrphanedDisplayArmorStands() {
        // No longer needed as we don't use ArmorStands for display.
    }

    private void cleanupOrphanedDisplayArmorStandsForMob(UUID mobUUID) {
        // No longer needed.
    }


    private void startCombatStateCheckTask() {
        stopCombatStateCheckTask();
        FileConfiguration mobSettings = plugin.getConfigManager().getCustomMobSettingsConfig();
        long checkIntervalTicks = mobSettings.getLong("display.combat-timeout-check-interval-ticks", 20L);
        long combatTimeoutSeconds = mobSettings.getLong("display.combat-timeout-seconds", 10L);
        long combatTimeoutMillis = combatTimeoutSeconds * 1000;

        combatStateCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (customMobManager == null) return;
                long currentTime = System.currentTimeMillis();
                customMobManager.getActiveCustomMobs().entrySet().removeIf(entry -> {
                    ActiveMobInfo mobInfo = entry.getValue();
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (entity == null || !entity.isValid()) {
                        // No specific display to remove from mobInfo. Entity removal is handled elsewhere.
                        return true; // Remove from map
                    }
                    // Only manage combat state for non-minions regarding display
                    if (mobInfo.isInCombat() && !mobInfo.getBaseData().isPlayerSummonedMinion()) {
                        if (currentTime - mobInfo.getLastCombatTimeMillis() > combatTimeoutMillis) {
                            plugin.getLogger().info("[CombatStateCheck] Mob " + entity.getUniqueId().toString().substring(0,6) + " saindo de combate. Vida ANTES: " + String.format("%.1f", mobInfo.getCurrentHealth()));
                            mobInfo.setInCombat(false);
                            // Resetar o tempo da última regeneração para que ela não comece imediatamente
                            mobInfo.setLastRegenTimeMillis(System.currentTimeMillis()); 
                            plugin.getLogger().info("[CombatStateCheck] Mob " + entity.getUniqueId().toString().substring(0,6) + " saiu de combate. Vida DEPOIS (antes do updateDisplay): " + String.format("%.1f", mobInfo.getCurrentHealth()) + ", ultimoRegenTimestamp: " + mobInfo.getLastRegenTimeMillis());
                            // updateMobDisplay will be called by the displayUpdateTask or other events.
                            // Explicitly call here to ensure timely update of combat state effects.
                            if (entity instanceof LivingEntity) {
                                updateMobDisplay((LivingEntity) entity, mobInfo);
                            }
                        }
                    }
                    return false; // Keep in map
                });
            }
        }.runTaskTimer(plugin, checkIntervalTicks, checkIntervalTicks);
        plugin.getLogger().info("Combat state check task started (interval: " + checkIntervalTicks + " ticks, timeout: " + combatTimeoutSeconds + " seconds).");
    }

    private void stopCombatStateCheckTask() {
        if (combatStateCheckTask != null && !combatStateCheckTask.isCancelled()) {
            combatStateCheckTask.cancel();
            combatStateCheckTask = null;
        }
    }

    private void startDisplayUpdateTask() {
        stopDisplayUpdateTask();
        FileConfiguration mobSettings = plugin.getConfigManager().getCustomMobSettingsConfig();
        long updateInterval = mobSettings.getLong("display.update-interval-ticks", 1L); // Default to 1 tick (20x per second)

        displayUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (customMobManager == null) return;
                for (Map.Entry<UUID, ActiveMobInfo> entry : customMobManager.getActiveCustomMobs().entrySet()) {
                    ActiveMobInfo mobInfo = entry.getValue();
                    Entity entity = Bukkit.getEntity(entry.getKey());

                    if (entity instanceof LivingEntity && entity.isValid()) {
                        // updateMobDisplay handles minion logic internally (sets name on entity, removes AS)
                        // and non-minion logic (manages health bar AS).
                        updateMobDisplay((LivingEntity) entity, mobInfo);
                    } else if (entity == null || !entity.isValid()) {
                        // No specific display to remove from mobInfo.
                        // The combatStateCheckTask or onDisable will handle removing from activeCustomMobs map
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, updateInterval); // Start immediately, then repeat
        plugin.getLogger().info("Mob display update task started (interval: " + updateInterval + " ticks).");
    }

    private void stopDisplayUpdateTask() {
        if (displayUpdateTask != null && !displayUpdateTask.isCancelled()) {
            displayUpdateTask.cancel();
            displayUpdateTask = null;
        }
    }

    public void setupInitialDisplay(LivingEntity mobEntity, ActiveMobInfo mobInfo) {
        // plugin.getLogger().info("[SETUP_DISPLAY] Para " + (mobEntity != null ? mobEntity.getUniqueId().toString().substring(0,6) : "mob_null") +
        //                          (mobInfo != null && mobInfo.getBaseData() != null ? ", isMinion: " + mobInfo.getBaseData().isPlayerSummonedMinion() : ", baseData_null"));

        if (mobInfo != null && mobEntity != null && mobEntity.isValid()) {
            if (mobInfo.getBaseData() == null) { // Checagem de segurança adicional
                plugin.getLogger().severe("[SETUP_DISPLAY] BaseData em mobInfo é nulo para " + mobEntity.getUniqueId().toString().substring(0,6) + "! Abortando setup de display.");
                return;
            }
            // Initial display is set by the first call to updateMobDisplay from the task.
            // We just need to ensure the mob is registered.
            // For minions, their name is set directly.
            // For non-minions, updateMobDisplay will set the name + defenses.
            // Call updateMobDisplay once to set the initial name.
            updateMobDisplay(mobEntity, mobInfo);
            plugin.getLogger().fine("[SETUP_DISPLAY] Chamado updateMobDisplay para " + mobEntity.getUniqueId().toString().substring(0,6));
        }
    }


    public void removeDisplayArmorStands(ActiveMobInfo mobInfo) { // Kept for API compatibility if used elsewhere
        removeMobDisplays(mobInfo);
    }

    public void updateMultiLineMobDisplay(LivingEntity mobEntity, ActiveMobInfo mobInfo, Player viewerContext) {
        if (mobInfo != null && mobEntity != null && mobEntity.isValid()) {
            updateMobDisplay(mobEntity, mobInfo);
        }
    }

    public void spawnFloatingText(Location location, String text, long durationTicks) {
        if (location.getWorld() == null) return;
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class, as -> {
            as.setMarker(true);
            as.setVisible(false);
            as.setSmall(true);
            as.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
            as.setCustomNameVisible(true);
            as.setInvulnerable(true);
            as.setGravity(false);
        });
        new BukkitRunnable() {
            private long ticksLived = 0;
            private final double upwardSpeedPerTick = 0.03;
            @Override
            public void run() {
                ticksLived++;
                if (!armorStand.isValid() || ticksLived >= durationTicks) {
                    if (armorStand.isValid()) armorStand.remove();
                    this.cancel();
                    return;
                }
                armorStand.teleport(armorStand.getLocation().add(0, upwardSpeedPerTick, 0));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public Optional<LivingEntity> spawnCustomMob(String mobId, Location location) {
        LivingEntity mob = customMobManager.spawnCustomMob(mobId, location); // This internally calls setupInitialDisplay via CustomMobManager
        return Optional.ofNullable(mob);
    }

    @Override
    public Optional<LivingEntity> spawnCustomMob(CustomMobData mobData, Location location) {
        LivingEntity mob = customMobManager.spawnCustomMob(mobData, location); // This internally calls setupInitialDisplay via CustomMobManager
        return Optional.ofNullable(mob);
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return customMobManager.isCustomMob(entity);
    }

    @Override
    public Optional<String> getCustomMobId(Entity entity) {
        if (isCustomMob(entity)) {
            return Optional.ofNullable(entity.getPersistentDataContainer().get(customMobManager.CUSTOM_MOB_ID_KEY, PersistentDataType.STRING));
        }
        return Optional.empty();
    }

    @Override
    public Optional<CustomMobData> getCustomMobData(String mobId) {
        return Optional.ofNullable(customMobManager.getMobData(mobId));
    }

    @Override
    public Optional<Double> getCustomMobCurrentHealth(UUID entityId) {
        return Optional.ofNullable(customMobManager.getActiveMobInfo(entityId))
                .map(ActiveMobInfo::getCurrentHealth);
    }

    @Override
    public void setCustomMobCurrentHealth(UUID entityId, double health) {
        Optional.ofNullable(customMobManager.getActiveMobInfo(entityId))
                .ifPresent(info -> {
                    plugin.getLogger().info("[SetCustomHealth] Para " + entityId.toString().substring(0,6) + ": definindo vida para " + String.format("%.1f", health) + ". Vida anterior: " + String.format("%.1f", info.getCurrentHealth()));
                    info.setCurrentHealth(health);
                    Entity entity = Bukkit.getEntity(entityId);
                    if (entity instanceof LivingEntity) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.getPersistentDataContainer().set(customMobManager.CURRENT_HEALTH_KEY, PersistentDataType.DOUBLE, health);
                        if (info.getBaseData().getMaxHealth() > 0) {
                            double healthPercentage = health / info.getBaseData().getMaxHealth();
                            double vanillaMaxHealth = livingEntity.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                            livingEntity.setHealth(Math.max(0.001, vanillaMaxHealth * healthPercentage));
                        } else if (health > 0) {
                            livingEntity.setHealth(0.001);
                        } else {
                            livingEntity.setHealth(0);
                        }
                        // updateMobDisplay will be called by the displayUpdateTask,
                        // or immediately if the mob is in combat (to update the health bar).
                        // Do NOT call updateMobDisplay here for out-of-combat regeneration to avoid nested calls.
                        if (info.isInCombat() || info.getBaseData().isPlayerSummonedMinion()) {
                             updateMobDisplay(livingEntity, info);
                        }
                    }
                });
    }

    private boolean isPeaceful(EntityType type) { // Keep this utility method
        // ... (implementation unchanged)
        switch (type) {
            case COW:
            case PIG:
            case SHEEP:
            case CHICKEN:
            case SQUID:
            case BAT:
            case VILLAGER:
            case WANDERING_TRADER:
            case HORSE:
            case DONKEY:
            case MULE:
            case LLAMA:
            case TRADER_LLAMA:
            case CAT:
            case OCELOT:
            case FOX:
            case PANDA:
            case BEE:
            case TURTLE:
            case DOLPHIN:
            case COD:
            case SALMON:
            case PUFFERFISH:
            case TROPICAL_FISH:
            case RABBIT:
            case PARROT:
            case GOAT:
            case AXOLOTL:
            case GLOW_SQUID:
            case ALLAY:
            case FROG:
            case TADPOLE:
            case SNIFFER:
            case CAMEL:
                return true;
            default:
                return false;
        }
    }

    private Player findClosestPlayer(LivingEntity mob, double range) { // Keep this utility method
        return mob.getWorld().getPlayers().stream()
                .filter(player -> player.getGameMode() != org.bukkit.GameMode.SPECTATOR && player.getGameMode() != org.bukkit.GameMode.CREATIVE)
                .filter(player -> player.getLocation().distanceSquared(mob.getLocation()) <= range * range)
                .filter(mob::hasLineOfSight)
                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(mob.getLocation())))
                .orElse(null);
    }

    private void tryToUseAbilities(ActiveMobInfo activeMobInfo, LivingEntity caster, Player target) { // Keep this utility method
        MobAI ai = activeMobInfo.getAssignedAI();
        if (ai != null && target != null && ai.shouldAttemptAbilityUsage(caster, target, activeMobInfo.getBaseData(), activeMobInfo)) {
            // AI should handle ability execution logic
        }
    }

    private boolean tryPlayerDodge(Player target, MobAbility ability) { // Keep this utility method
        CharacterModule charModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (charModule == null) return false;
        Optional<PlayerData> playerDataOpt = charModule.getCharacterData(target);
        if (playerDataOpt.isEmpty()) return false;
        PlayerAttributes attributes = playerDataOpt.get().getAttributes();
        if (attributes == null) return false;
        FileConfiguration combatSettings = plugin.getConfigManager().getCombatSettingsConfig();
        double dodgeChanceConfig = combatSettings.getDouble("dodge.agility-scaling-factor", 0.005);
        double baseDodgeChance = combatSettings.getDouble("dodge.base-chance", 0.0);
        double playerDodgeChance = baseDodgeChance + (attributes.getAgility() * dodgeChanceConfig);
        playerDodgeChance = Math.min(playerDodgeChance, combatSettings.getDouble("dodge.max-chance", 0.75));
        return Math.random() < playerDodgeChance;
    }
}
