package me.ray.aethelgardRPG.modules.custommob;

import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomMobData {

    private final String id;
    private final String displayName;
    private final EntityType entityType;
    private final int level;
    private final double health;
    private final double damage;
    private final double physicalDefense;
    private final double magicalDefense;
    private final int experienceDrop;
    private final AIType aiType;
    private final boolean isPlayerSummonedMinion;
    private final double followRange;
    private final double attackRange;
    private final double aggroRange;
    private final String lootTableId;
    private final String regenProfileId; // NOVO
    private final List<String> abilities;
    private final List<String> drops;
    private final double scale;
    private final int respawnTimeSeconds;
    private final Map<String, Object> extraData;

    public CustomMobData(String id, String displayName, EntityType entityType, int level,
                         double health, double damage, double physicalDefense, double magicalDefense,
                         int experienceDrop, AIType aiType, boolean isPlayerSummonedMinion,
                         double followRange, double attackRange, double aggroRange,
                         String lootTableId, String regenProfileId, // NOVO
                         List<String> abilities, List<String> drops, double scale, int respawnTimeSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.entityType = entityType;
        this.level = level;
        this.health = health;
        this.damage = damage;
        this.physicalDefense = physicalDefense;
        this.magicalDefense = magicalDefense;
        this.experienceDrop = experienceDrop;
        this.aiType = aiType;
        this.isPlayerSummonedMinion = isPlayerSummonedMinion;
        this.followRange = followRange;
        this.attackRange = attackRange;
        this.aggroRange = aggroRange;
        this.lootTableId = lootTableId;
        this.regenProfileId = regenProfileId; // NOVO
        this.abilities = abilities != null ? List.copyOf(abilities) : Collections.emptyList();
        this.drops = drops != null ? List.copyOf(drops) : Collections.emptyList();
        this.scale = scale;
        this.respawnTimeSeconds = respawnTimeSeconds;
        this.extraData = new HashMap<>(); // Modificado para ser mutável se necessário
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public EntityType getEntityType() { return entityType; }
    public int getLevel() { return level; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return health; }
    public double getDamage() { return damage; }
    public double getPhysicalDefense() { return physicalDefense; }
    public double getMagicalDefense() { return magicalDefense; }
    public int getExperienceDrop() { return experienceDrop; }
    public AIType getAiType() { return aiType; }
    public boolean isPlayerSummonedMinion() { return isPlayerSummonedMinion; }
    public double getFollowRange() { return followRange; }
    public double getAttackRange() { return attackRange; }
    public double getAggroRange() { return aggroRange; }
    public String getLootTableId() { return lootTableId; }
    public String getRegenProfileId() { return regenProfileId; } // NOVO
    public List<String> getAbilities() { return abilities; }
    public List<String> getDrops() { return drops; }
    public double getScale() { return scale; }
    public int getRespawnTimeSeconds() { return respawnTimeSeconds; }
    public Map<String, Object> getExtraData() { return extraData; }
}