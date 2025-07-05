// C:/Users/r/IdeaProjects/AethelgardRPG/src/main/java/me/ray/aethelgardRPG/modules/custommob/ai/impl/AggressiveRangedAIImpl.java
package me.ray.aethelgardRPG.modules.custommob.ai.impl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.MobAbility;
import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AggressiveRangedAIImpl implements MobAI {

    protected final AethelgardRPG plugin;
    protected final CustomMobModule customMobModule;

    public AggressiveRangedAIImpl(AethelgardRPG plugin, CustomMobModule customMobModule, LivingEntity self, CustomMobData mobData) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
    }

    @Override
    public void tickLogic(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        if (!(self instanceof Mob mob)) {
            return;
        }

        if (activeMobInfo.isTaunted()) {
            Entity taunter = Bukkit.getEntity(activeMobInfo.getTauntedBy());
            if (taunter instanceof LivingEntity && taunter.isValid() && !taunter.isDead()) {
                mob.setTarget((LivingEntity) taunter);
            } else {
                activeMobInfo.setTauntedBy(null);
                activeMobInfo.setTauntEndTime(0);
            }
        }

        LivingEntity target = mob.getTarget();
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

        List<MobAbility> availableAbilities = new ArrayList<>();
        for (String abilityId : mobData.getAbilities()) {
            if (!activeMobInfo.isAbilityOnCooldown(abilityId)) {
                MobAbility ability = customMobModule.getMobAbilityManager().getAbility(abilityId);
                if (ability != null) {
                    double abilityRange = ability.getRange();
                    if (caster.getLocation().distanceSquared(target.getLocation()) <= abilityRange * abilityRange) {
                        availableAbilities.add(ability);
                    }
                }
            }
        }

        if (!availableAbilities.isEmpty()) {
            // Escolhe uma habilidade aleatoriamente da lista de disponíveis
            MobAbility abilityToUse = availableAbilities.get(ThreadLocalRandom.current().nextInt(availableAbilities.size()));

            // Verifica a chance de acerto
            if (abilityToUse.getTemplate().getHitChance() < 1.0 && Math.random() > abilityToUse.getTemplate().getHitChance()) {
                // A habilidade errou! Aplica um cooldown reduzido (ex: metade do normal)
                activeMobInfo.setAbilityCooldownFromTicks(abilityToUse.getId(), abilityToUse.getCooldown() / 2);
                plugin.getLogger().fine(caster.getName() + " tentou usar " + abilityToUse.getDisplayName(null) + " mas errou.");
                return; // Sai após a tentativa (não tenta outras habilidades neste tick)
            }

            // A habilidade acertou! Executa e aplica o cooldown completo.
            List<Player> targets = (target instanceof Player) ? List.of((Player) target) : List.of();
            abilityToUse.execute(caster, targets);
            activeMobInfo.setAbilityCooldownFromTicks(abilityToUse.getId(), abilityToUse.getCooldown());
            plugin.getLogger().fine(caster.getName() + " usou " + abilityToUse.getDisplayName(null) + " em " + target.getName());
        }
    }

    @Override
    public AIType getType() {
        return AIType.AGGRESSIVE_RANGED;
    }
}