package me.ray.aethelgardRPG.modules.character.leveling;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class LevelingManager {
    private final AethelgardRPG plugin;
    private int maxLevel;
    private int attributePointsPerLevel;
    // Cache para armazenar o XP necessário para upar de um nível para o próximo.
    // Chave: Nível atual, Valor: XP necessário para ir para o Nível Atual + 1
    // Parâmetros da fórmula de XP
    private double levelLinearFactor;
    private double exponentialBaseValue;
    private double exponentialGrowthBase;
    private double exponentialLevelDivisor;
    private double overallDivisor;
    private double difficultyMultiplier;

    private final Map<Integer, Double> xpForLevelUpCache;

    public LevelingManager(AethelgardRPG plugin) {
        this.plugin = plugin;
        this.xpForLevelUpCache = new HashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getCharacterSettingsConfig();

        this.maxLevel = config.getInt("leveling.max-level", 100);
        this.attributePointsPerLevel = config.getInt("leveling.attribute-points-per-level", 3);

        // Carregar parâmetros da fórmula de XP
        this.levelLinearFactor = config.getDouble("leveling.formula.level-linear-factor", 1.0);
        this.exponentialBaseValue = config.getDouble("leveling.formula.exponential-base-value", 300.0);
        this.exponentialGrowthBase = config.getDouble("leveling.formula.exponential-growth-base", 2.0);
        this.exponentialLevelDivisor = config.getDouble("leveling.formula.exponential-level-divisor", 7.0);
        this.overallDivisor = config.getDouble("leveling.formula.overall-divisor", 4.0);
        this.difficultyMultiplier = config.getDouble("leveling.formula.difficulty-multiplier", 1.0);

        // Limpar o cache caso o maxLevel mude em um reload.
        xpForLevelUpCache.clear();

        plugin.getLogger().info("LevelingManager config loaded. Max level: " + maxLevel + ", Attribute points per level: " + attributePointsPerLevel);
        plugin.getLogger().info("XP formula parameters loaded. Multiplier: " + difficultyMultiplier);
    }

    public double getXpForNextLevel(int currentLevel) {
        if (currentLevel < 1) { // Nível mínimo é 1
            currentLevel = 1;
        }
        if (currentLevel >= maxLevel) {
            return Double.MAX_VALUE; // Já está no nível máximo ou acima
        }

        // Calcula usando a fórmula e armazena no cache
        return xpForLevelUpCache.computeIfAbsent(currentLevel, k -> {
            if (k < 1) k = 1; // Garante que o nível não seja menor que 1 para o cálculo

            double term1 = levelLinearFactor * k;
            double term2 = exponentialBaseValue * Math.pow(exponentialGrowthBase, (double) k / exponentialLevelDivisor);

            // Garante que overallDivisor não seja zero para evitar divisão por zero
            double divisor = (overallDivisor == 0) ? 1.0 : overallDivisor;
            return Math.floor((term1 + term2) / divisor) * difficultyMultiplier;
        });
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getAttributePointsPerLevel() {
        return attributePointsPerLevel;
    }
}