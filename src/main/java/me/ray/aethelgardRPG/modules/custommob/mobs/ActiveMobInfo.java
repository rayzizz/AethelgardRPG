package me.ray.aethelgardRPG.modules.custommob.mobs;

import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armazena informações de estado para uma instância de mob customizado ativa
 * no mundo.
 */
public class ActiveMobInfo {
    private final UUID entityId;
    private final CustomMobData baseData;

    // Estado dinâmico
    private long despawnTimeMillis = -1;
    private UUID tauntedBy = null;
    private long tauntEndTime = -1;
    private boolean inCombat = false;
    private long lastCombatTimeMillis = -1;
    private double currentHealth;
    private long lastRegenTimeMillis = -1;

    // Para ArmorStands de display
    private UUID nameArmorStandUUID;
    private UUID defenseArmorStandUUID;
    private UUID healthArmorStandUUID;

    // Para IA customizada
    private MobAI assignedAI;

    // Para cooldowns de habilidades
    private final Map<String, Long> abilityCooldowns = new ConcurrentHashMap<>();

    public ActiveMobInfo(UUID entityId, CustomMobData baseData) {
        this.entityId = entityId;
        this.baseData = baseData;
        this.currentHealth = baseData.getHealth();
    }

    // Getters
    public UUID getEntityId() { return entityId; }
    public CustomMobData getBaseData() { return baseData; }
    public long getDespawnTimeMillis() { return despawnTimeMillis; }
    public UUID getTauntedBy() { return tauntedBy; }
    public long getTauntEndTime() { return tauntEndTime; }
    public boolean isInCombat() { return inCombat; }
    public long getLastCombatTimeMillis() { return lastCombatTimeMillis; }
    public double getCurrentHealth() { return currentHealth; }
    public long getLastRegenTimeMillis() { return lastRegenTimeMillis; }
    public UUID getNameArmorStandUUID() { return nameArmorStandUUID; }
    public UUID getDefenseArmorStandUUID() { return defenseArmorStandUUID; }
    public UUID getHealthArmorStandUUID() { return healthArmorStandUUID; }
    public MobAI getAssignedAI() { return assignedAI; }

    // Setters
    public void setDespawnTimeMillis(long despawnTimeMillis) { this.despawnTimeMillis = despawnTimeMillis; }
    public void setTauntedBy(UUID tauntedBy) { this.tauntedBy = tauntedBy; }
    public void setTauntEndTime(long tauntEndTime) { this.tauntEndTime = tauntEndTime; }
    public void setInCombat(boolean inCombat) { this.inCombat = inCombat; }
    public void setLastCombatTimeMillis(long lastCombatTimeMillis) { this.lastCombatTimeMillis = lastCombatTimeMillis; }
    public void setCurrentHealth(double currentHealth) { this.currentHealth = currentHealth; }
    public void setLastRegenTimeMillis(long lastRegenTimeMillis) { this.lastRegenTimeMillis = lastRegenTimeMillis; }
    public void setNameArmorStandUUID(UUID nameArmorStandUUID) { this.nameArmorStandUUID = nameArmorStandUUID; }
    public void setDefenseArmorStandUUID(UUID defenseArmorStandUUID) { this.defenseArmorStandUUID = defenseArmorStandUUID; }
    public void setHealthArmorStandUUID(UUID healthArmorStandUUID) { this.healthArmorStandUUID = healthArmorStandUUID; }
    public void setAssignedAI(MobAI assignedAI) { this.assignedAI = assignedAI; }

    // --- CORREÇÃO APLICADA AQUI ---
    /**
     * Verifica se uma habilidade específica está em cooldown.
     * @param abilityId O ID da habilidade.
     * @return true se estiver em cooldown, false caso contrário.
     */
    public boolean isAbilityOnCooldown(String abilityId) {
        return abilityCooldowns.getOrDefault(abilityId, 0L) > System.currentTimeMillis();
    }

    /**
     * Define o cooldown para uma habilidade a partir de um valor em ticks.
     * @param abilityId O ID da habilidade.
     * @param cooldownTicks O tempo de cooldown em ticks (20 ticks = 1 segundo).
     */
    public void setAbilityCooldownFromTicks(String abilityId, long cooldownTicks) {
        // Converte ticks para milissegundos (1 tick = 50ms)
        long cooldownMillis = cooldownTicks * 50L;
        abilityCooldowns.put(abilityId, System.currentTimeMillis() + cooldownMillis);
    }
    // --- FIM DA CORREÇÃO ---

    public boolean isTaunted() {
        return tauntedBy != null && System.currentTimeMillis() < tauntEndTime;
    }
}