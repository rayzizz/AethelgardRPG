package me.ray.aethelgardRPG.modules.quest;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;

public class Quest {
    private final String id;
    private final String titleKey; // Changed from title
    private final String descriptionKey; // Changed from description
    private final String startingNpcId;
    private final int requiredLevel;
    private final List<String> requiredQuests;

    private final List<QuestObjective> objectives;
    private final List<String> rewardCommands;
    private final double rewardExperience;
    private final double rewardMoney;

    public Quest(String id, String titleKey, String descriptionKey, String startingNpcId,
                 int requiredLevel, List<String> requiredQuests,
                 List<QuestObjective> objectives, List<String> rewardCommands,
                 double rewardExperience, double rewardMoney) {
        this.id = id;
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
        this.startingNpcId = startingNpcId;
        this.requiredLevel = requiredLevel;
        this.requiredQuests = requiredQuests != null ? new ArrayList<>(requiredQuests) : new ArrayList<>();
        this.objectives = new ArrayList<>();
        if (objectives != null) {
            for (QuestObjective obj : objectives) {
                this.objectives.add(obj.copy());
            }
        }
        this.rewardCommands = rewardCommands != null ? new ArrayList<>(rewardCommands) : new ArrayList<>();
        this.rewardExperience = rewardExperience;
        this.rewardMoney = rewardMoney;
    }

    // Getters
    public String getId() { return id; }
    public String getTitleKey() { return titleKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public String getStartingNpcId() { return startingNpcId; }
    public int getRequiredLevel() { return requiredLevel; }
    public List<String> getRequiredQuests() { return requiredQuests; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public List<String> getRewardCommands() { return rewardCommands; }
    public double getRewardExperience() { return rewardExperience; }
    public double getRewardMoney() { return rewardMoney; }

    /**
     * Gets the localized title of the quest for a specific player.
     */
    public String getTitle(Player player) {
        return AethelgardRPG.getInstance().getLanguageManager().getMessage(player, this.titleKey);
    }

    /**
     * Gets the localized description of the quest for a specific player.
     */
    public String getDescription(Player player) {
        return AethelgardRPG.getInstance().getLanguageManager().getMessage(player, this.descriptionKey);
    }

    public boolean areAllObjectivesComplete() {
        for (QuestObjective objective : objectives) {
            if (!objective.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public Quest createPlayerInstance() {
        List<QuestObjective> playerObjectives = new ArrayList<>();
        for (QuestObjective templateObjective : this.objectives) {
            playerObjectives.add(templateObjective.copy());
        }
        return new Quest(id, titleKey, descriptionKey, startingNpcId, requiredLevel,
                requiredQuests, playerObjectives, rewardCommands, rewardExperience, rewardMoney);
    }
}