package me.ray.aethelgardRPG.modules.custommob;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.custommob.abilities.MobAbilityManager;
import me.ray.aethelgardRPG.modules.custommob.ai.AIManager;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;
import me.ray.aethelgardRPG.modules.custommob.api.CustomMobAPI;
import me.ray.aethelgardRPG.modules.custommob.listeners.CustomMobDeathListener;
import me.ray.aethelgardRPG.modules.custommob.listeners.EntityDamageListenerMob;
import me.ray.aethelgardRPG.modules.custommob.listeners.EntitySpawnListener;
import me.ray.aethelgardRPG.modules.custommob.listeners.MinionTargetListener;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager;
import me.ray.aethelgardRPG.modules.custommob.mobs.regen.MobRegenManager;
import me.ray.aethelgardRPG.modules.custommob.ai.minionimpl.SummonedMinionAIImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class CustomMobModule implements RPGModule, CustomMobAPI {

    private final AethelgardRPG plugin;
    private CustomMobManager customMobManager;
    private MobAbilityManager abilityManager;
    private MobRegenManager mobRegenManager;
    private AIManager aiManager;

    private BukkitTask combatStateCheckTask;
    private BukkitTask displayUpdateTask;
    private BukkitTask visibilityUpdateTask;
    private BukkitTask customAITickTask;

    private String defensesFormat;
    private long displayUpdateIntervalTicks;
    private double visibilityDistanceSquared;

    private static final double Y_OFFSET_NAME = 0.5;
    private static final double Y_OFFSET_DEFENSE = 0.25;
    private static final double Y_OFFSET_HEALTH = 0.0;

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
        this.abilityManager = new MobAbilityManager(plugin);
        this.mobRegenManager = new MobRegenManager(plugin, this);
        this.aiManager = new AIManager(plugin, this);

        plugin.getServer().getPluginManager().registerEvents(new EntitySpawnListener(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new EntityDamageListenerMob(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CustomMobDeathListener(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MinionTargetListener(plugin, this), plugin);
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo CustomMob habilitado.");
        loadDisplayConfig();
        customMobManager.loadMobConfigurations();
        abilityManager.loadAbilityConfigurations();
        mobRegenManager.loadRegenProfiles();
        startCombatStateCheckTask();
        startDisplayUpdateTask();
        startVisibilityUpdateTask();
        startCustomAITickTask();
        mobRegenManager.startRegenTask();
        cleanupOrphanedCustomMobs();
        cleanupOrphanedDisplayArmorStands();
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo CustomMob desabilitado.");
        stopCombatStateCheckTask();
        stopDisplayUpdateTask();
        stopVisibilityUpdateTask();
        stopCustomAITickTask();
        mobRegenManager.stopRegenTask();

        if (customMobManager != null) {
            List<UUID> activeMobUUIDs = new ArrayList<>(customMobManager.getActiveCustomMobs().keySet());
            for (UUID mobUUID : activeMobUUIDs) {
                ActiveMobInfo mobInfo = customMobManager.getActiveMobInfo(mobUUID);
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

    private void startCustomAITickTask() {
        stopCustomAITickTask();
        long tickInterval = 2L;
        customAITickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (customMobManager == null) return;

                for (Map.Entry<UUID, ActiveMobInfo> entry : customMobManager.getActiveCustomMobs().entrySet()) {
                    ActiveMobInfo mobInfo = entry.getValue();
                    MobAI ai = mobInfo.getAssignedAI();
                    if (ai == null) continue;

                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (entity instanceof LivingEntity && entity.isValid() && !entity.isDead()) {
                        try {
                            ai.tickLogic((LivingEntity) entity, mobInfo.getBaseData(), mobInfo);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.SEVERE, "Erro ao executar a lógica da IA para o mob " + mobInfo.getBaseData().getId(), e);
                            mobInfo.setAssignedAI(null);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, tickInterval);
    }

    private void stopCustomAITickTask() {
        if (customAITickTask != null && !customAITickTask.isCancelled()) {
            customAITickTask.cancel();
            customAITickTask = null;
        }
    }

    @Override
    public CustomMobManager getCustomMobManager() {
        return customMobManager;
    }

    public MobAbilityManager getMobAbilityManager() {
        return abilityManager;
    }

    public MobRegenManager getMobRegenManager() {
        return mobRegenManager;
    }

    public AIManager getAIManager() {
        return aiManager;
    }

    private void loadDisplayConfig() {
        FileConfiguration mobSettings = plugin.getConfigManager().getCustomMobSettingsConfig();
        defensesFormat = mobSettings.getString("display.defenses.format", "&7[&bDefP&f: {phys} &7| &dDefM&f: {mag}&7]");
        displayUpdateIntervalTicks = mobSettings.getLong("display.update-interval-ticks", 1L);
        visibilityDistanceSquared = mobSettings.getDouble("display.visibility-distance-squared", 1024.0);
    }

    @Override
    public Optional<CustomMobData> getCustomMobData(String mobId) {
        return Optional.ofNullable(customMobManager.getMobData(mobId));
    }

    public void setupInitialDisplay(LivingEntity mobEntity, ActiveMobInfo mobInfo) {
        if (mobInfo == null || mobInfo.getBaseData() == null) return;
        if (mobInfo.getBaseData().isPlayerSummonedMinion()) return;

        Location baseLocation = mobEntity.getLocation().add(0, mobEntity.getHeight(), 0);
        UUID ownerUUID = mobEntity.getUniqueId();
        removeMobDisplays(mobInfo);

        ArmorStand nameAS = spawnDisplayArmorStand(baseLocation.clone().add(0, Y_OFFSET_NAME, 0), generateMobDisplayName(mobInfo.getBaseData()), ownerUUID);
        mobInfo.setNameArmorStandUUID(nameAS.getUniqueId());

        ArmorStand defenseAS = spawnDisplayArmorStand(baseLocation.clone().add(0, Y_OFFSET_DEFENSE, 0), formatDefenseString(mobInfo), ownerUUID);
        mobInfo.setDefenseArmorStandUUID(defenseAS.getUniqueId());

        ArmorStand healthAS = spawnDisplayArmorStand(baseLocation.clone().add(0, Y_OFFSET_HEALTH, 0), "", ownerUUID);
        healthAS.setCustomNameVisible(false);
        mobInfo.setHealthArmorStandUUID(healthAS.getUniqueId());
    }

    public void removeMobDisplays(ActiveMobInfo mobInfo) {
        if (mobInfo == null) return;
        if (mobInfo.getNameArmorStandUUID() != null) {
            Entity as = Bukkit.getEntity(mobInfo.getNameArmorStandUUID());
            if (as != null) as.remove();
        }
        if (mobInfo.getDefenseArmorStandUUID() != null) {
            Entity as = Bukkit.getEntity(mobInfo.getDefenseArmorStandUUID());
            if (as != null) as.remove();
        }
        if (mobInfo.getHealthArmorStandUUID() != null) {
            Entity as = Bukkit.getEntity(mobInfo.getHealthArmorStandUUID());
            if (as != null) as.remove();
        }
    }

    private void updateDynamicMobDisplay(LivingEntity mobEntity, ActiveMobInfo mobInfo) {
        if (mobEntity == null || !mobEntity.isValid() || mobInfo == null || mobInfo.getBaseData() == null) {
            return;
        }

        if (mobInfo.getBaseData().isPlayerSummonedMinion()) {
            String minionName = generateMinionDisplayName(mobEntity, mobInfo);
            if (mobEntity.getCustomName() == null || !mobEntity.getCustomName().equals(minionName)) {
                mobEntity.setCustomName(minionName);
            }
            if (!mobEntity.isCustomNameVisible()) mobEntity.setCustomNameVisible(true);
            return;
        }

        Entity nameAS = Bukkit.getEntity(mobInfo.getNameArmorStandUUID());
        Entity defenseAS = Bukkit.getEntity(mobInfo.getDefenseArmorStandUUID());
        Entity healthAS = Bukkit.getEntity(mobInfo.getHealthArmorStandUUID());

        if (nameAS == null || !nameAS.isValid() || defenseAS == null || !defenseAS.isValid() || healthAS == null || !healthAS.isValid()) {
            setupInitialDisplay(mobEntity, mobInfo);
            return;
        }

        Location baseLocation = mobEntity.getLocation().add(0, mobEntity.getHeight(), 0);
        nameAS.teleport(baseLocation.clone().add(0, Y_OFFSET_NAME, 0));
        defenseAS.teleport(baseLocation.clone().add(0, Y_OFFSET_DEFENSE, 0));
        healthAS.teleport(baseLocation.clone().add(0, Y_OFFSET_HEALTH, 0));

        if (mobInfo.isInCombat()) {
            String healthText = formatHealthBar(mobInfo.getCurrentHealth(), mobInfo.getBaseData().getMaxHealth());
            if (healthAS.getCustomName() == null || !healthAS.getCustomName().equals(healthText)) {
                healthAS.setCustomName(healthText);
            }
            if (!healthAS.isCustomNameVisible()) healthAS.setCustomNameVisible(true);
            if (defenseAS.isCustomNameVisible()) defenseAS.setCustomNameVisible(false);
        } else {
            if (!defenseAS.isCustomNameVisible()) defenseAS.setCustomNameVisible(true);
            if (healthAS.isCustomNameVisible()) healthAS.setCustomNameVisible(false);
        }
    }

    public void updateAllMobDisplaysForPlayer(Player viewerContext) {
        if (customMobManager == null) return;
        for (Map.Entry<UUID, ActiveMobInfo> entry : customMobManager.getActiveCustomMobs().entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity instanceof LivingEntity && entity.isValid()) {
                updateMultiLineMobDisplay((LivingEntity) entity, entry.getValue());
            }
        }
    }

    private ArmorStand spawnDisplayArmorStand(Location location, String name, UUID ownerUUID) {
        return location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setMarker(true);
            armorStand.setVisible(false);
            armorStand.setSmall(true);
            armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', name));
            armorStand.setCustomNameVisible(true);
            armorStand.setInvulnerable(true);
            armorStand.setGravity(false);
            armorStand.getPersistentDataContainer().set(getDisplayArmorStandOwnerKey(), PersistentDataType.STRING, ownerUUID.toString());
        });
    }

    private String generateMobDisplayName(CustomMobData mobData) {
        String mobName = mobData.getDisplayName();
        if (mobName == null || mobName.isEmpty()) {
            mobName = "Unknown Mob";
        }
        int level = mobData.getLevel();
        String formattedName = String.format("&7[Nv. %d] %s", level, mobName);
        return ChatColor.translateAlternateColorCodes('&', formattedName);
    }

    private String generateMinionDisplayName(LivingEntity mobEntity, ActiveMobInfo mobInfo) {
        CustomMobData mobData = mobInfo.getBaseData();
        double currentHealth = mobInfo.getCurrentHealth();
        double maxHealth = mobData.getMaxHealth();
        long despawnTime = mobInfo.getDespawnTimeMillis();
        long secondsLeft = (despawnTime > 0) ? (despawnTime - System.currentTimeMillis()) / 1000 : 0;

        String ownerName = mobEntity.getPersistentDataContainer().get(SummonedMinionAIImpl.SUMMON_OWNER_NAME_KEY, PersistentDataType.STRING);
        if (ownerName == null) ownerName = "???";

        String healthDisplay = String.format("%.0f/%.0f", Math.max(0, currentHealth), maxHealth);
        String timeDisplay = String.valueOf(Math.max(0, secondsLeft));

        String baseMinionName = mobData.getDisplayName();
        if (baseMinionName == null || baseMinionName.isEmpty()) {
            baseMinionName = "Minion";
        }

        String coloredFormat = "&e%s's &a%s &f(&c%s&f HP) - &b%ss";
        String finalName = String.format(coloredFormat, ownerName, baseMinionName, healthDisplay, timeDisplay);

        return ChatColor.translateAlternateColorCodes('&', finalName);
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

        StringBuilder bar = new StringBuilder();
        bar.append(colorBrackets).append("[");
        bar.append(colorFilled);
        for (int i = 0; i < filledCount; i++) bar.append(charFilled);
        bar.append(colorEmpty);
        for (int i = filledCount; i < barLength; i++) bar.append(charEmpty);
        bar.append(colorBrackets).append("]");
        bar.append(" ").append(colorHpText).append(String.format("%.0f/%.0f", Math.max(0, currentHealth), maxHealth));
        return ChatColor.translateAlternateColorCodes('&', bar.toString());
    }

    private String formatDefenseString(ActiveMobInfo mobInfo) {
        CustomMobData data = mobInfo.getBaseData();
        String formatted = defensesFormat
                .replace("{phys}", String.format("%.0f", data.getPhysicalDefense()))
                .replace("{mag}", String.format("%.0f", data.getMagicalDefense()));
        return ChatColor.translateAlternateColorCodes('&', formatted);
    }

    private void startDisplayUpdateTask() {
        stopDisplayUpdateTask();
        long updateInterval = this.displayUpdateIntervalTicks;
        displayUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (customMobManager == null) return;
                for (Map.Entry<UUID, ActiveMobInfo> entry : customMobManager.getActiveCustomMobs().entrySet()) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (entity instanceof LivingEntity && entity.isValid()) {
                        updateDynamicMobDisplay((LivingEntity) entity, entry.getValue());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, updateInterval);
    }

    private void stopDisplayUpdateTask() {
        if (displayUpdateTask != null && !displayUpdateTask.isCancelled()) {
            displayUpdateTask.cancel();
            displayUpdateTask = null;
        }
    }

    private void startVisibilityUpdateTask() {
        stopVisibilityUpdateTask();
        long checkInterval = 10L;
        visibilityUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (customMobManager == null) return;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (Map.Entry<UUID, ActiveMobInfo> entry : customMobManager.getActiveCustomMobs().entrySet()) {
                        UUID mobUUID = entry.getKey();
                        ActiveMobInfo mobInfo = entry.getValue();
                        Entity mobEntity = Bukkit.getEntity(mobUUID);
                        if (mobEntity == null || !mobEntity.isValid() || mobInfo.getBaseData().isPlayerSummonedMinion()) {
                            continue;
                        }
                        if (!player.getWorld().equals(mobEntity.getWorld())) {
                            setMobDisplayVisibility(player, mobInfo, false);
                            continue;
                        }
                        double distanceSquared = player.getLocation().distanceSquared(mobEntity.getLocation());
                        boolean shouldBeVisible = distanceSquared <= visibilityDistanceSquared;
                        setMobDisplayVisibility(player, mobInfo, shouldBeVisible);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, checkInterval);
    }

    private void stopVisibilityUpdateTask() {
        if (visibilityUpdateTask != null && !visibilityUpdateTask.isCancelled()) {
            visibilityUpdateTask.cancel();
            visibilityUpdateTask = null;
        }
    }

    private void setMobDisplayVisibility(Player player, ActiveMobInfo mobInfo, boolean visible) {
        if (mobInfo == null) return;
        Entity nameAS = Bukkit.getEntity(mobInfo.getNameArmorStandUUID());
        Entity defenseAS = Bukkit.getEntity(mobInfo.getDefenseArmorStandUUID());
        Entity healthAS = Bukkit.getEntity(mobInfo.getHealthArmorStandUUID());
        if (visible) {
            if (nameAS != null) player.showEntity(plugin, nameAS);
            if (defenseAS != null) player.showEntity(plugin, defenseAS);
            if (healthAS != null) player.showEntity(plugin, healthAS);
        } else {
            if (nameAS != null) player.hideEntity(plugin, nameAS);
            if (defenseAS != null) player.hideEntity(plugin, defenseAS);
            if (healthAS != null) player.hideEntity(plugin, healthAS);
        }
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
                for (Map.Entry<UUID, ActiveMobInfo> entry : customMobManager.getActiveCustomMobs().entrySet()) {
                    ActiveMobInfo mobInfo = entry.getValue();
                    if (mobInfo.isInCombat() && !mobInfo.getBaseData().isPlayerSummonedMinion()) {
                        if (currentTime - mobInfo.getLastCombatTimeMillis() > combatTimeoutMillis) {
                            mobInfo.setInCombat(false);
                            mobInfo.setLastRegenTimeMillis(System.currentTimeMillis());
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, checkIntervalTicks, checkIntervalTicks);
    }

    private void stopCombatStateCheckTask() {
        if (combatStateCheckTask != null && !combatStateCheckTask.isCancelled()) {
            combatStateCheckTask.cancel();
            combatStateCheckTask = null;
        }
    }

    private void cleanupOrphanedCustomMobs() {
        if (customMobManager == null) return;
        int removedCount = 0;
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (customMobManager.isCustomMob(entity)) {
                    if (customMobManager.getActiveMobInfo(entity.getUniqueId()) == null) {
                        entity.remove();
                        removedCount++;
                    }
                }
            }
        }
        if (removedCount > 0) {
            plugin.getLogger().info(removedCount + " mobs customizados órfãos foram removidos.");
        }
    }

    private NamespacedKey getDisplayArmorStandOwnerKey() {
        return new NamespacedKey(plugin, "display_as_owner_mob_uuid");
    }

    private void cleanupOrphanedDisplayArmorStands() {
        int removedCount = 0;
        NamespacedKey ownerKey = getDisplayArmorStandOwnerKey();
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand as : world.getEntitiesByClass(ArmorStand.class)) {
                if (as.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
                    try {
                        UUID ownerUUID = UUID.fromString(as.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING));
                        if (Bukkit.getEntity(ownerUUID) == null) {
                            as.remove();
                            removedCount++;
                        }
                    } catch (Exception e) {
                        as.remove();
                        removedCount++;
                    }
                }
            }
        }
        if (removedCount > 0) {
            plugin.getLogger().info(removedCount + " ArmorStands de display órfãos foram removidos.");
        }
    }

    public void spawnFloatingText(Location location, String text, long durationTicks) {
        if (location.getWorld() == null) return;
        location.getWorld().spawn(location, ArmorStand.class, as -> {
            as.setMarker(true);
            as.setVisible(false);
            as.setSmall(true);
            as.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
            as.setCustomNameVisible(true);
            as.setInvulnerable(true);
            as.setGravity(false);
            new BukkitRunnable() {
                private long ticksLived = 0;
                private final double upwardSpeedPerTick = 0.03;
                @Override
                public void run() {
                    ticksLived++;
                    if (!as.isValid() || ticksLived >= durationTicks) {
                        if (as.isValid()) as.remove();
                        this.cancel();
                        return;
                    }
                    as.teleport(as.getLocation().add(0, upwardSpeedPerTick, 0));
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }

    @Override
    public Optional<LivingEntity> spawnCustomMob(String mobId, Location location) {
        return customMobManager.spawnCustomMob(mobId, location);
    }

    @Override
    public Optional<LivingEntity> spawnCustomMob(CustomMobData mobData, Location location) {
        return customMobManager.spawnCustomMob(mobData, location);
    }

    @Override
    public Optional<String> getMobDisplayName(String mobId, Player playerContext) {
        return getCustomMobData(mobId).map(this::generateMobDisplayName);
    }

    @Override
    public double getCustomMobCurrentHealth(UUID entityId) {
        ActiveMobInfo info = customMobManager.getActiveMobInfo(entityId);
        return info != null ? info.getCurrentHealth() : 0.0;
    }

    @Override
    public void setCustomMobCurrentHealth(UUID entityId, double health) {
        ActiveMobInfo info = customMobManager.getActiveMobInfo(entityId);
        if (info == null) return;
        info.setCurrentHealth(health);
        Entity entity = Bukkit.getEntity(entityId);
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.getPersistentDataContainer().set(CustomMobManager.CURRENT_HEALTH_KEY, PersistentDataType.DOUBLE, health);
            if (info.getBaseData().getMaxHealth() > 0) {
                double healthPercentage = health / info.getBaseData().getMaxHealth();
                double vanillaMaxHealth = livingEntity.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                livingEntity.setHealth(Math.max(0.001, vanillaMaxHealth * healthPercentage));
            } else if (health <= 0) {
                livingEntity.setHealth(0);
            }
        }
    }

    public void refreshVisibleMobNames(Player player) {
        updateAllMobDisplaysForPlayer(player);
    }

    public void updateMultiLineMobDisplay(LivingEntity mobEntity, ActiveMobInfo mobInfo) {
        if (mobEntity == null || !mobEntity.isValid() || mobInfo == null || mobInfo.getBaseData() == null) {
            return;
        }

        if (mobInfo.getBaseData().isPlayerSummonedMinion()) {
            String minionName = generateMinionDisplayName(mobEntity, mobInfo);
            if (mobEntity.getCustomName() == null || !mobEntity.getCustomName().equals(minionName)) {
                mobEntity.setCustomName(minionName);
            }
            if (!mobEntity.isCustomNameVisible()) mobEntity.setCustomNameVisible(true);
            return;
        }

        Entity nameAS = Bukkit.getEntity(mobInfo.getNameArmorStandUUID());
        Entity defenseAS = Bukkit.getEntity(mobInfo.getDefenseArmorStandUUID());
        Entity healthAS = Bukkit.getEntity(mobInfo.getHealthArmorStandUUID());

        if (nameAS == null || !nameAS.isValid() || defenseAS == null || !defenseAS.isValid() || healthAS == null || !healthAS.isValid()) {
            setupInitialDisplay(mobEntity, mobInfo);
            return;
        }

        String nameText = generateMobDisplayName(mobInfo.getBaseData());
        if (nameAS.getCustomName() == null || !nameAS.getCustomName().equals(nameText)) {
            nameAS.setCustomName(nameText);
        }

        updateDynamicMobDisplay(mobEntity, mobInfo);
    }
}