// C:/Users/r/IdeaProjects/AethelgardRPG/src/main/java/me/ray/aethelgardRPG/modules/skill/skills/SkillManager.java
package me.ray.aethelgardRPG.modules.skill.skills;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.skill.PlayerSkillProgress;
import me.ray.aethelgardRPG.modules.skill.SkillModule;
import me.ray.aethelgardRPG.modules.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class SkillManager {

    private final AethelgardRPG plugin;
    private final SkillModule skillModule;
    private final Map<SkillType, Map<Integer, Double>> xpPerLevelCache;
    private final Map<SkillType, Integer> maxSkillLevelCache;
    // NOVO: Cache para fontes de XP (ex: Bloco -> XP)
    private final Map<SkillType, Map<String, Double>> xpSourcesCache;

    public SkillManager(AethelgardRPG plugin, SkillModule skillModule) {
        this.plugin = plugin;
        this.skillModule = skillModule;
        this.xpPerLevelCache = new EnumMap<>(SkillType.class);
        this.maxSkillLevelCache = new EnumMap<>(SkillType.class);
        this.xpSourcesCache = new EnumMap<>(SkillType.class);
    }

    public void loadSkillConfigurations() {
        xpPerLevelCache.clear();
        maxSkillLevelCache.clear();
        xpSourcesCache.clear(); // Limpa o novo cache

        FileConfiguration skillConfig = plugin.getConfigManager().getSkillSettingsConfig();
        ConfigurationSection defaultConfigSection = skillConfig.getConfigurationSection("defaults");

        for (SkillType type : SkillType.values()) {
            String skillPath = "skills." + type.name();
            ConfigurationSection skillSpecificConfig = skillConfig.getConfigurationSection(skillPath);

            int maxLevel = (skillSpecificConfig != null && skillSpecificConfig.contains("max-level"))
                    ? skillSpecificConfig.getInt("max-level")
                    : defaultConfigSection.getInt("max-level", 100);
            maxSkillLevelCache.put(type, maxLevel);

            ConfigurationSection formulaConfig = (skillSpecificConfig != null && skillSpecificConfig.contains("formula"))
                    ? skillSpecificConfig.getConfigurationSection("formula")
                    : defaultConfigSection.getConfigurationSection("formula");

            if (formulaConfig == null) {
                plugin.getLogger().log(Level.SEVERE, "Seção de fórmula de XP não encontrada para a habilidade " + type.name() + " ou nos padrões.");
                continue;
            }

            double linearFactor = formulaConfig.getDouble("level-linear-factor", 100.0);
            double powerBase = formulaConfig.getDouble("power-base", 1.2);
            double powerMultiplier = formulaConfig.getDouble("power-multiplier", 1.05);

            Map<Integer, Double> xpCurve = new HashMap<>();
            for (int level = 1; level <= maxLevel; level++) {
                double xpNeeded = (level * linearFactor) + Math.pow(powerBase, level * powerMultiplier);
                xpCurve.put(level, Math.floor(xpNeeded));
            }
            xpPerLevelCache.put(type, xpCurve);
            plugin.getLogger().info("Configuração de XP por nível carregada para: " + type.name());

            // NOVO: Carregar as fontes de XP
            Map<String, Double> sources = new HashMap<>();
            if (skillSpecificConfig != null && skillSpecificConfig.isConfigurationSection("xp-sources")) {
                ConfigurationSection sourcesSection = skillSpecificConfig.getConfigurationSection("xp-sources");
                for (String sourceKey : sourcesSection.getKeys(false)) {
                    sources.put(sourceKey.toUpperCase(), sourcesSection.getDouble(sourceKey));
                }
            }
            xpSourcesCache.put(type, sources);
            plugin.getLogger().info(sources.size() + " fontes de XP carregadas para: " + type.name());
        }
    }

    public void addExperience(Player player, SkillType skillType, double amount) {
        if (amount <= 0) return;

        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (characterModule == null) return;

        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
        if (playerDataOpt.isEmpty()) return;

        PlayerSkillProgress skillProgress = playerDataOpt.get().getSkillProgress(skillType);
        if (skillProgress.getLevel() >= getMaxLevel(skillType)) {
            return;
        }

        skillProgress.addExperience(amount);
        player.sendMessage(plugin.getMessage(player, "skill.xp-gain", String.format("%.1f", amount), skillType.getDisplayName(player)));

        checkLevelUp(player, skillProgress);
    }

    private void checkLevelUp(Player player, PlayerSkillProgress skillProgress) {
        SkillType skillType = skillProgress.getSkillType();
        int currentLevel = skillProgress.getLevel();
        double currentXp = skillProgress.getExperience();
        int maxLevel = getMaxLevel(skillType);

        if (currentLevel >= maxLevel) return;

        double xpNeededForNextLevel = getXpForLevel(skillType, currentLevel + 1);

        while (currentXp >= xpNeededForNextLevel && currentLevel < maxLevel) {
            currentLevel++;
            skillProgress.setLevel(currentLevel);
            skillProgress.setExperience(currentXp - xpNeededForNextLevel);
            currentXp = skillProgress.getExperience();

            player.sendMessage(plugin.getMessage(player, "skill.level-up", skillType.getDisplayName(player), currentLevel));

            if (currentLevel >= maxLevel) break;
            xpNeededForNextLevel = getXpForLevel(skillType, currentLevel + 1);
        }
    }

    public double getXpForLevel(SkillType skillType, int level) {
        return xpPerLevelCache.getOrDefault(skillType, Collections.emptyMap()).getOrDefault(level, Double.MAX_VALUE);
    }

    public int getMaxLevel(SkillType skillType) {
        return maxSkillLevelCache.getOrDefault(skillType, 1);
    }

    /**
     * NOVO: Obtém a quantidade de XP para uma ação específica (quebrar bloco, pescar item).
     * @param skillType O tipo de habilidade.
     * @param sourceId O ID da fonte (ex: "COAL_ORE", "COD").
     * @return A quantidade de XP, ou 0.0 se não configurado.
     */
    public double getXpForAction(SkillType skillType, String sourceId) {
        if (sourceId == null) return 0.0;
        return xpSourcesCache.getOrDefault(skillType, Collections.emptyMap())
                .getOrDefault(sourceId.toUpperCase(), 0.0);
    }

    public int getPlayerSkillLevel(Player player, SkillType skillType) {
        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (characterModule == null) return 0;

        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
        return playerDataOpt.map(pd -> pd.getSkillProgress(skillType).getLevel()).orElse(0);
    }
}