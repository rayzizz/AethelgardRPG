package me.ray.aethelgardRPG.modules.quest;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.api.CustomMobAPI;
import me.ray.aethelgardRPG.modules.item.api.ItemAPI;
import org.bukkit.entity.Player;

public class QuestObjective {
    private final QuestObjectiveType type;
    private final String target; // ID do mob, ID do item, etc.
    private final int requiredAmount;
    private final String descriptionKey; // Changed from description

    private int currentAmount;

    public QuestObjective(QuestObjectiveType type, String target, int requiredAmount, String descriptionKey) {
        this.type = type;
        this.target = target;
        this.requiredAmount = requiredAmount;
        this.descriptionKey = descriptionKey;
        this.currentAmount = 0;
    }

    // Getters
    public QuestObjectiveType getType() { return type; }
    public String getTarget() { return target; }
    public int getRequiredAmount() { return requiredAmount; }
    public String getDescriptionKey() { return descriptionKey; }
    public int getCurrentAmount() { return currentAmount; }

    /**
     * Gets the fully formatted and localized description of the objective's progress.
     * Example: "- Kill Goblins [5/10]"
     * @param player The player for language context.
     * @return The formatted string.
     */
    public String getFormattedProgress(Player player) {
        String objectiveText = getObjectiveText(player);
        String progress = String.format("%d/%d", this.currentAmount, this.requiredAmount);
        String formatKey = isCompleted() ? "quest.objective.format.complete" : "quest.objective.format.incomplete";

        return AethelgardRPG.getInstance().getLanguageManager().getMessage(player, formatKey, objectiveText, progress);
    }

    /**
     * Gets just the localized text part of the objective.
     * Example: "Kill 5 Goblins"
     * @param player The player for language context.
     * @return The localized objective text.
     */
    public String getObjectiveText(Player player) {
        // This part requires integration with other modules to get translated names
        String translatedTarget = getTranslatedTarget(player);
        return AethelgardRPG.getInstance().getLanguageManager().getMessage(player, this.descriptionKey, translatedTarget, this.requiredAmount);
    }

    /**
     * Translates the target ID (e.g., "goblin_warrior") into a display name (e.g., "Goblin Warrior").
     * This is a placeholder for integration with Item/Mob modules.
     */
    private String getTranslatedTarget(Player player) {
        AethelgardRPG plugin = AethelgardRPG.getInstance();
        if (this.type == QuestObjectiveType.KILL_MOBS) {
            CustomMobAPI mobAPI = plugin.getCustomMobAPI();
            if (mobAPI != null) {
                return mobAPI.getMobDisplayName(this.target, player).orElse(this.target);
            }
        } else if (this.type == QuestObjectiveType.COLLECT_ITEMS) {
            ItemAPI itemAPI = plugin.getItemAPI();
            if (itemAPI != null) {
                return itemAPI.getRPGItemById(this.target, player)
                        .map(item -> item.getItemMeta().getDisplayName())
                        .orElse(this.target);
            }
        }
        // For TALK_TO_NPC and REACH_LOCATION, the target is often a direct name.
        return this.target;
    }

    // Setters
    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = Math.min(currentAmount, requiredAmount);
    }

    public void incrementAmount(int amount) {
        this.currentAmount = Math.min(this.currentAmount + amount, requiredAmount);
    }

    public boolean isCompleted() {
        return currentAmount >= requiredAmount;
    }

    public QuestObjective copy() {
        QuestObjective copy = new QuestObjective(this.type, this.target, this.requiredAmount, this.descriptionKey);
        copy.setCurrentAmount(this.currentAmount);
        return copy;
    }
}