package me.ray.aethelgardRPG.modules.custommob.ai.impl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.MobAbility;
import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class AggressiveMeleeAIImpl implements MobAI {

    protected final AethelgardRPG plugin;
    protected final CustomMobModule customMobModule;

    public AggressiveMeleeAIImpl(AethelgardRPG plugin, CustomMobModule customMobModule, LivingEntity self, CustomMobData mobData) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
    }

    @Override
    public void tickLogic(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        if (!(self instanceof Creature creature)) {
            return;
        }

        if (activeMobInfo.isTaunted()) {
            Entity taunter = Bukkit.getEntity(activeMobInfo.getTauntedBy());
            if (taunter instanceof LivingEntity && taunter.isValid() && !taunter.isDead()) {
                creature.setTarget((LivingEntity) taunter);
            } else {
                activeMobInfo.setTauntedBy(null);
                activeMobInfo.setTauntEndTime(0);
            }
        }

        LivingEntity target = creature.getTarget();
        boolean hasValidTarget = target != null && target.isValid() && !target.isDead() && self.hasLineOfSight(target);

        if (hasValidTarget) {
            if (!activeMobInfo.isInCombat()) {
                activeMobInfo.setInCombat(true);
            }
            activeMobInfo.setLastCombatTimeMillis(System.currentTimeMillis());

            if (shouldAttemptAbilityUsage(self, target, mobData, activeMobInfo)) {
                tryUseAbilities(activeMobInfo, self, target, mobData);
            }
        }
    }

    @Override
    public void onTargetAcquired(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        activeMobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
        activeMobInfo.setInCombat(true);
        plugin.getLogger().fine(mobData.getDisplayName() + " (" + getType().name() + ") adquiriu alvo: " + target.getName());
    }

    @Override
    public void onDamaged(LivingEntity self, LivingEntity attacker, double damage, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        activeMobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
        activeMobInfo.setInCombat(true);
    }

    @Override
    public void onTargetLost(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        plugin.getLogger().fine(mobData.getDisplayName() + " (" + getType().name() + ") perdeu o alvo.");
    }

    @Override
    public boolean shouldAttemptAbilityUsage(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        return target != null && target.isValid() && !target.isDead();
    }

    protected void tryUseAbilities(ActiveMobInfo activeMobInfo, LivingEntity caster, LivingEntity target, CustomMobData mobData) {
        if (mobData.getAbilities() == null || mobData.getAbilities().isEmpty()) {
            return;
        }

        for (String abilityId : mobData.getAbilities()) {
            if (!activeMobInfo.isAbilityOnCooldown(abilityId)) {
                MobAbility ability = customMobModule.getMobAbilityManager().getAbility(abilityId);
                if (ability != null) {
                    double abilityRange = ability.getRange();
                    if (abilityRange == 0 || caster.getLocation().distanceSquared(target.getLocation()) <= abilityRange * abilityRange) {
                        if (ability.getTemplate().getHitChance() < 1.0 && Math.random() > ability.getTemplate().getHitChance()) {
                            // --- CORREÇÃO APLICADA AQUI ---
                            activeMobInfo.setAbilityCooldownFromTicks(abilityId, ability.getCooldown() / 2);
                            plugin.getLogger().fine(caster.getName() + " tentou usar " + ability.getDisplayName(null) + " mas errou.");
                            continue;
                        }

                        List<Player> targets = (target instanceof Player) ? List.of((Player) target) : List.of();
                        ability.execute(caster, targets);
                        // --- CORREÇÃO APLICADA AQUI ---
                        activeMobInfo.setAbilityCooldownFromTicks(abilityId, ability.getCooldown());
                        plugin.getLogger().fine(caster.getName() + " usou " + ability.getDisplayName(null) + " em " + target.getName());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public AIType getType() {
        return AIType.AGGRESSIVE_MELEE;
    }
}