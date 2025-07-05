package me.ray.aethelgardRPG.modules.custommob.ai.impl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.MobAbility;
import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

public class StationaryAIImpl implements MobAI {

    protected final AethelgardRPG plugin;
    protected final CustomMobModule customMobModule;
    protected final LivingEntity self;
    protected final CustomMobData mobData;

    public StationaryAIImpl(AethelgardRPG plugin, CustomMobModule customMobModule, LivingEntity self, CustomMobData mobData) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
        this.self = self;
        this.mobData = mobData;
    }

    @Override
    public void tickLogic(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        if (activeMobInfo.isTaunted()) {
            Entity taunter = Bukkit.getEntity(activeMobInfo.getTauntedBy());
            if (taunter instanceof LivingEntity && taunter.isValid() && !taunter.isDead()) {
                LivingEntity taunterLiving = (LivingEntity) taunter;

                if (self instanceof org.bukkit.entity.Creature) {
                    ((org.bukkit.entity.Creature) self).setTarget(taunterLiving);
                }
                Location targetEyeLocation = taunterLiving.getEyeLocation();
                self.lookAt(targetEyeLocation.getX(), targetEyeLocation.getY(), targetEyeLocation.getZ(), LookAnchor.EYES);
                if (shouldAttemptAbilityUsage(self, taunterLiving, mobData, activeMobInfo)) {
                    tryUseAbilities(activeMobInfo, self, taunterLiving);
                }
                return;
            } else {
                activeMobInfo.setTauntedBy(null);
                activeMobInfo.setTauntEndTime(0);
            }
        }

        if (activeMobInfo.isInCombat() && self instanceof org.bukkit.entity.Creature) {
            org.bukkit.entity.Creature creature = (org.bukkit.entity.Creature) self;
            LivingEntity target = creature.getTarget();
            if (target != null) {
                if (target.isValid() && !target.isDead()) {
                    Location targetEyeLocation = target.getEyeLocation();
                    self.lookAt(targetEyeLocation.getX(), targetEyeLocation.getY(), targetEyeLocation.getZ(), LookAnchor.EYES);
                    if (shouldAttemptAbilityUsage(self, target, mobData, activeMobInfo)) {
                        tryUseAbilities(activeMobInfo, self, target);
                    }
                } else {
                    creature.setTarget(null);
                    onTargetLost(self, mobData, activeMobInfo);
                }
            } else if (creature.getTarget() == null) {
                Player newTarget = findClosestPlayerInRange(self, mobData.getAggroRange());
                if (newTarget != null) {
                    onTargetAcquired(self, newTarget, mobData, activeMobInfo);
                }
            }
        } else if (!activeMobInfo.isInCombat()) {
            Player newTarget = findClosestPlayerInRange(self, mobData.getAggroRange());
            if (newTarget != null) {
                onTargetAcquired(self, newTarget, mobData, activeMobInfo);
            }
        }
    }

    @Override
    public void onTargetAcquired(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        activeMobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
        if (self instanceof org.bukkit.entity.Creature) {
            ((org.bukkit.entity.Creature) self).setTarget(target);
        }
        plugin.getLogger().fine(mobData.getDisplayName() + " (" + getType().name() + ") adquiriu alvo: " + target.getName());
    }

    @Override
    public void onDamaged(LivingEntity self, LivingEntity attacker, double damage, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        activeMobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
        if (attacker != null && self instanceof org.bukkit.entity.Creature) {
            if (((org.bukkit.entity.Creature) self).getTarget() == null) {
                onTargetAcquired(self, attacker, mobData, activeMobInfo);
            }
        }
    }

    @Override
    public void onTargetLost(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        if (self instanceof org.bukkit.entity.Creature) {
            ((org.bukkit.entity.Creature) self).setTarget(null);
        }
        plugin.getLogger().fine(mobData.getDisplayName() + " (" + getType().name() + ") perdeu o alvo.");
    }

    @Override
    public boolean shouldAttemptAbilityUsage(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        return activeMobInfo.isInCombat() && target != null && target.isValid() && !target.isDead();
    }

    protected void tryUseAbilities(ActiveMobInfo activeMobInfo, LivingEntity caster, LivingEntity target) {
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
                            plugin.getLogger().fine(caster.getName() + " tried to use " + ability.getDisplayName(null) + " but missed.");
                            continue;
                        }

                        ability.execute(caster, target instanceof Player ? List.of((Player)target) : List.of());
                        // --- CORREÇÃO APLICADA AQUI ---
                        activeMobInfo.setAbilityCooldownFromTicks(abilityId, ability.getCooldown());
                        plugin.getLogger().fine(caster.getName() + " used " + ability.getDisplayName(null) + " on " + target.getName());
                        break;
                    }
                }
            }
        }
    }

    protected Player findClosestPlayerInRange(LivingEntity mob, double range) {
        return mob.getWorld().getPlayers().stream()
                .filter(player -> player.getGameMode() != org.bukkit.GameMode.SPECTATOR && player.getGameMode() != org.bukkit.GameMode.CREATIVE)
                .filter(player -> player.getLocation().distanceSquared(mob.getLocation()) <= range * range)
                .filter(mob::hasLineOfSight)
                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(mob.getLocation())))
                .orElse(null);
    }

    @Override
    public AIType getType() {
        return AIType.STATIONARY;
    }
}