package me.ray.aethelgardRPG.modules.dungeon;

import org.bukkit.Location; // Usaremos Location para pontos de spawn, etc.
import java.util.List;
import java.util.Map;

public class DungeonTemplate {
    private final String id; // ID único do template da dungeon
    private final String name; // Nome de exibição
    private final String description;
    private final int minPlayers;
    private final int maxPlayers;
    private final int requiredLevel; // Nível mínimo para entrar
    private final long timeLimitSeconds; // Limite de tempo para completar (0 = sem limite)

    // Localização de entrada (onde os jogadores são teleportados ao iniciar)
    // private final Location entryLocation; // Precisa ser configurável (mundo, x, y, z)

    // Lista de "fases" ou "encontros" da dungeon
    // Cada fase pode ter mobs específicos, um boss, ou um objetivo
    private final List<DungeonPhase> phases;

    // Recompensas ao completar a dungeon
    private final List<String> completionRewardCommands;
    private final double completionRewardExperience;
    private final double completionRewardMoney;
    // private final List<String> completionRewardItemIds; // IDs de itens a serem dados

    public DungeonTemplate(String id, String name, String description, int minPlayers, int maxPlayers,
                           int requiredLevel, long timeLimitSeconds, List<DungeonPhase> phases,
                           List<String> completionRewardCommands, double completionRewardExperience, double completionRewardMoney) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.requiredLevel = requiredLevel;
        this.timeLimitSeconds = timeLimitSeconds;
        this.phases = phases;
        this.completionRewardCommands = completionRewardCommands;
        this.completionRewardExperience = completionRewardExperience;
        this.completionRewardMoney = completionRewardMoney;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getRequiredLevel() { return requiredLevel; }
    public long getTimeLimitSeconds() { return timeLimitSeconds; }
    public List<DungeonPhase> getPhases() { return phases; }
    public List<String> getCompletionRewardCommands() { return completionRewardCommands; }
    public double getCompletionRewardExperience() { return completionRewardExperience; }
    public double getCompletionRewardMoney() { return completionRewardMoney; }

    /**
     * Representa uma fase ou encontro dentro da dungeon.
     */
    public static class DungeonPhase {
        private final String phaseName;
        // Mobs a serem spawnados nesta fase (ID do mob customizado -> quantidade)
        private final Map<String, Integer> mobsToSpawn;
        private final String bossMobId; // ID do boss desta fase (opcional)
        // private final QuestObjective objective; // Objetivo específico para esta fase (opcional)

        public DungeonPhase(String phaseName, Map<String, Integer> mobsToSpawn, String bossMobId) {
            this.phaseName = phaseName;
            this.mobsToSpawn = mobsToSpawn;
            this.bossMobId = bossMobId;
        }

        public String getPhaseName() { return phaseName; }
        public Map<String, Integer> getMobsToSpawn() { return mobsToSpawn; }
        public String getBossMobId() { return bossMobId; }
    }
}