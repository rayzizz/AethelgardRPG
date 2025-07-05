package me.ray.aethelgardRPG.modules.custommob.mobs;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CustomMobManager {

    private final AethelgardRPG plugin;
    private final CustomMobModule customMobModule;
    private final Map<String, CustomMobData> registeredMobTypes = new HashMap<>();
    private final Map<UUID, ActiveMobInfo> activeMobs = new ConcurrentHashMap<>();
    private final Scoreboard mainScoreboard;

    public static final NamespacedKey CURRENT_HEALTH_KEY = new NamespacedKey(AethelgardRPG.getInstance(), "custom_mob_current_health");

    public CustomMobManager(AethelgardRPG plugin, CustomMobModule customMobModule) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void loadMobConfigurations() {
        registeredMobTypes.clear();

        // Extrai todos os arquivos e pastas de 'mobs/' do JAR, se não existirem.
        plugin.extractResourcesFromJar("mobs");

        File mobsFolder = new File(plugin.getDataFolder(), "mobs");
        loadMobsFromFolderRecursive(mobsFolder);
        plugin.getLogger().info(registeredMobTypes.size() + " templates de mobs carregados.");
    }

    private void loadMobsFromFolderRecursive(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadMobsFromFolderRecursive(file);
            } else if (file.getName().endsWith(".yml")) {
                parseMobData(file).ifPresent(this::registerMobData);
            }
        }
    }

    private void registerMobData(CustomMobData mobData) {
        registeredMobTypes.put(mobData.getId().toLowerCase(), mobData);
    }

    private Optional<CustomMobData> parseMobData(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        try {
            String id = config.getString("id", file.getName().replace(".yml", ""));
            String displayName = config.getString("display-name", "Unnamed Mob");
            EntityType entityType = EntityType.valueOf(config.getString("base-entity-type", "ZOMBIE").toUpperCase());

            ConfigurationSection stats = config.getConfigurationSection("stats");
            int level = stats != null ? stats.getInt("level", 1) : 1;
            double health = stats != null ? stats.getDouble("max-health", 20.0) : 20.0;
            double damage = stats != null ? stats.getDouble("base-damage", 3.0) : 3.0;
            double physicalDefense = stats != null ? stats.getDouble("physical-defense", 0.0) : 0.0;
            double magicalDefense = stats != null ? stats.getDouble("magical-defense", 0.0) : 0.0;
            int experienceDrop = stats != null ? stats.getInt("experience-drop", 5) : 5;

            ConfigurationSection behavior = config.getConfigurationSection("behavior");
            AIType aiType = AIType.valueOf(behavior != null ? behavior.getString("ai-type", "AGGRESSIVE_MELEE").toUpperCase() : "AGGRESSIVE_MELEE");
            double followRange = behavior != null ? behavior.getDouble("follow-range", 16.0) : 16.0;
            double attackRange = behavior != null ? behavior.getDouble("attack-range", 2.0) : 2.0;
            double aggroRange = behavior != null ? behavior.getDouble("aggro-range", 16.0) : 16.0;
            String regenProfileId = behavior != null ? behavior.getString("out-of-combat-regen-profile") : null;

            ConfigurationSection visual = config.getConfigurationSection("visual");
            double scale = (visual != null) ? visual.getDouble("scale", 1.0) : 1.0;

            List<String> abilities = config.getStringList("abilities");
            List<String> drops = config.getStringList("drops");

            CustomMobData mobData = new CustomMobData(
                    id, displayName, entityType, level, health, damage,
                    physicalDefense, magicalDefense, experienceDrop,
                    aiType, false, followRange, attackRange, aggroRange,
                    "default", regenProfileId, abilities, drops, scale, 60
            );
            return Optional.of(mobData);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao interpretar os dados do mob de " + file.getName(), e);
            return Optional.empty();
        }
    }

    public CustomMobData getMobData(String mobId) {
        return registeredMobTypes.get(mobId.toLowerCase());
    }

    public Optional<LivingEntity> spawnCustomMob(CustomMobData mobData, Location location) {
        if (mobData == null || location == null || location.getWorld() == null) {
            return Optional.empty();
        }

        try {
            LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, mobData.getEntityType());

            Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(mobData.getMaxHealth());
            entity.setHealth(mobData.getMaxHealth());

            entity.setCustomName(null);
            entity.setCustomNameVisible(false);

            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            pdc.set(PDCKeys.CUSTOM_MOB_ID_KEY, PersistentDataType.STRING, mobData.getId());
            pdc.set(CURRENT_HEALTH_KEY, PersistentDataType.DOUBLE, mobData.getHealth());

            ActiveMobInfo activeMobInfo = new ActiveMobInfo(entity.getUniqueId(), mobData);

            // Cria e atribui a IA ao mob
            MobAI ai = customMobModule.getAIManager().createAI(mobData.getAiType(), mobData, entity);
            if (ai != null) {
                activeMobInfo.setAssignedAI(ai);
                plugin.getLogger().fine("IA do tipo " + ai.getType() + " atribuída ao mob " + mobData.getId());
            }

            activeMobs.put(entity.getUniqueId(), activeMobInfo);
            customMobModule.setupInitialDisplay(entity, activeMobInfo);

            return Optional.of(entity);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao spawnar mob customizado: " + mobData.getId(), e);
            return Optional.empty();
        }
    }

    public Optional<LivingEntity> spawnCustomMob(String mobId, Location location) {
        CustomMobData mobData = getMobData(mobId);
        if (mobData == null) {
            plugin.getLogger().warning("MobData não encontrado para o ID: " + mobId);
            return Optional.empty();
        }
        return spawnCustomMob(mobData, location);
    }

    public String getFormattedMobName(CustomMobData mobData) {
        String name = mobData.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = "Unknown Mob";
        }
        return String.format("&7[Nv. %d] %s", mobData.getLevel(), name);
    }

    public boolean isCustomMob(Entity entity) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer().has(PDCKeys.CUSTOM_MOB_ID_KEY, PersistentDataType.STRING);
    }

    public Optional<CustomMobData> getMobDataFromEntity(Entity entity) {
        if (!isCustomMob(entity)) {
            return Optional.empty();
        }
        String mobId = entity.getPersistentDataContainer().get(PDCKeys.CUSTOM_MOB_ID_KEY, PersistentDataType.STRING);
        return Optional.ofNullable(getMobData(mobId));
    }

    public ActiveMobInfo getActiveMobInfo(UUID entityId) {
        return activeMobs.get(entityId);
    }

    public void removeActiveMob(UUID entityId) {
        ActiveMobInfo removed = activeMobs.remove(entityId);
        if (removed != null) {
            customMobModule.removeMobDisplays(removed);
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                Team team = mainScoreboard.getEntryTeam(entity.getUniqueId().toString());
                if (team != null) {
                    team.removeEntry(entity.getUniqueId().toString());
                }
            }
        }
    }

    public void addMinionToOwnerTeam(LivingEntity minion) {
        PersistentDataContainer pdc = minion.getPersistentDataContainer();
        if (!pdc.has(PDCKeys.SUMMON_OWNER_KEY, PersistentDataType.STRING)) return;

        String ownerUUIDString = pdc.get(PDCKeys.SUMMON_OWNER_KEY, PersistentDataType.STRING);
        Player owner = Bukkit.getPlayer(UUID.fromString(ownerUUIDString));

        if (owner != null) {
            String teamName = "minions_" + owner.getName();
            Team team = mainScoreboard.getTeam(teamName);
            if (team == null) {
                team = mainScoreboard.registerNewTeam(teamName);
                team.setAllowFriendlyFire(false);
                team.setCanSeeFriendlyInvisibles(true);
                team.addEntry(owner.getName());
            }
            team.addEntry(minion.getUniqueId().toString());
        }
    }

    public void updateAllMobDisplaysForPlayer(Player viewerContext) {
        plugin.getLogger().info("Simulando atualização de display de " + activeMobs.size() + " mobs para " + viewerContext.getName());
    }

    public Set<String> getRegisteredMobTypeIds() {
        return Collections.unmodifiableSet(registeredMobTypes.keySet());
    }

    public Map<UUID, ActiveMobInfo> getActiveCustomMobs() {
        return Collections.unmodifiableMap(activeMobs);
    }
}