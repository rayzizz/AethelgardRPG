package me.ray.aethelgardRPG.modules.custommob.abilities;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.MobAbility;
import me.ray.aethelgardRPG.modules.custommob.abilities.impl.AreaOfEffectDamageAbility;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.ray.aethelgardRPG.modules.custommob.abilities.impl.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MobAbilityManager {

    private final AethelgardRPG plugin;
    private final Map<String, MobAbility> registeredAbilities = new HashMap<>();

    public MobAbilityManager(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    public void loadAbilityConfigurations() {
        registeredAbilities.clear();
        File abilitiesDataFolder = new File(plugin.getDataFolder(), "mobs/abilities");
        if (!abilitiesDataFolder.exists()) {
            if (!abilitiesDataFolder.mkdirs()) {
                plugin.getLogger().severe("Não foi possível criar a pasta 'mobs/abilities'. O carregamento de habilidades será ignorado.");
                return;
            }
            plugin.getLogger().info("Pasta 'mobs/abilities' criada.");
        }

        extractDefaultAbilityFiles(abilitiesDataFolder);
        loadAbilitiesFromFolder(abilitiesDataFolder, abilitiesDataFolder);
        plugin.getLogger().info(registeredAbilities.size() + " habilidades de mobs carregadas.");
    }

    private void extractDefaultAbilityFiles(File abilitiesDataFolder) {
        String resourceDirPrefix = "mobs/abilities/";

        try {
            CodeSource src = AethelgardRPG.class.getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jarUrl = src.getLocation();
                // Usar try-with-resources para ZipInputStream
                try (ZipInputStream zip = new ZipInputStream(jarUrl.openStream())) {
                    ZipEntry ze;
                    while ((ze = zip.getNextEntry()) != null) {
                        String entryName = ze.getName();
                        if (entryName.startsWith(resourceDirPrefix) && entryName.endsWith(".yml") && !ze.isDirectory()) {
                            File targetFile = new File(plugin.getDataFolder(), entryName);
                            if (!targetFile.getParentFile().exists()) {
                                targetFile.getParentFile().mkdirs();
                            }
                            if (!targetFile.exists()) {
                                plugin.saveResource(entryName, false);
                                plugin.getLogger().info("Default ability config extracted: " + entryName);
                            }
                        }
                    }
                } // zip é fechado automaticamente aqui
            } else {
                plugin.getLogger().warning("Could not get CodeSource to scan for default ability files.");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error scanning JAR for default ability files", e);
        }
    }

    private void loadAbilitiesFromFolder(File folder, File rootAbilitiesFolder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadAbilitiesFromFolder(file, rootAbilitiesFolder);
            } else if (file.getName().endsWith(".yml")) {
                FileConfiguration abilityConfig = YamlConfiguration.loadConfiguration(file);
                try {
                    String relativePath = rootAbilitiesFolder.toURI().relativize(file.toURI()).getPath().replace(".yml", "");
                    String abilityIdFromFilePath = relativePath.replace(java.io.File.separatorChar, '/');
                    String id = abilityConfig.getString("id", abilityIdFromFilePath);

                    String displayName = abilityConfig.getString("display-name", "Habilidade Desconhecida");
                    String displayNameKey = abilityConfig.getString("display-name-key", ""); // NOVO: Carrega a chave de tradução
                    String type = abilityConfig.getString("type", "GENERIC").toUpperCase();
                    int cooldownTicks = abilityConfig.getInt("cooldown-ticks", 100);
                    String targetType = abilityConfig.getString("target-type", "NONE").toUpperCase();
                    double range = abilityConfig.getDouble("range", 0.0);
                    List<String> description = abilityConfig.getStringList("description");
                    double hitChance = abilityConfig.getDouble("hit-chance", 1.0);
                    boolean playerDodgeable = abilityConfig.getBoolean("player-dodgeable", false);

                    Map<String, Object> parameters = new HashMap<>();
                    ConfigurationSection paramsSection = abilityConfig.getConfigurationSection("parameters");

                    if (paramsSection != null) {
                        for (String key : paramsSection.getKeys(false)) {
                            parameters.put(key, paramsSection.get(key));
                        }
                    }

                    // Construtor atualizado para incluir displayNameKey
                    MobAbilityTemplate template = new MobAbilityTemplate(id, displayName, displayNameKey, type, cooldownTicks,
                            targetType, range, parameters, description,
                            hitChance, playerDodgeable);

                    MobAbility abilityInstance = null;
                    switch (type) {
                        case "MELEE_DASH_ATTACK":
                            abilityInstance = new MeleeDashAttackAbility(template, plugin);
                            break;
                        case "PROJECTILE_DAMAGE":
                            abilityInstance = new ProjectileDamageAbility(template, plugin);
                            break;
                        case "SELF_HEAL":
                            abilityInstance = new SelfHealAbility(template, plugin);
                            break;
                        case "AREA_OF_EFFECT_DAMAGE":
                            abilityInstance = new AreaOfEffectDamageAbility(template, plugin);
                            break;
                        default:
                            plugin.getLogger().warning("Tipo de habilidade desconhecido '" + type + "' para a habilidade ID: " + id + ". Habilidade não será funcional.");
                            break;
                    }

                    if (abilityInstance != null) {
                        registeredAbilities.put(id.toLowerCase(), abilityInstance);
                    }

                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao carregar habilidade do arquivo " + file.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public MobAbility getAbility(String id) {
        return registeredAbilities.get(id.toLowerCase());
    }

    public Set<String> getRegisteredAbilityIds() {
        return registeredAbilities.keySet();
    }
}