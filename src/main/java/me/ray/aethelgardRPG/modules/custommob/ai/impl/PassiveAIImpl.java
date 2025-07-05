package me.ray.aethelgardRPG.modules.custommob.ai.impl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import org.bukkit.entity.LivingEntity;
// Player import is no longer needed here directly for method signatures
// import org.bukkit.entity.Player;

public class PassiveAIImpl implements MobAI {

    protected final AethelgardRPG plugin;
    protected final CustomMobModule customMobModule;
    protected final LivingEntity self;
    protected final CustomMobData mobData;

    public PassiveAIImpl(AethelgardRPG plugin, CustomMobModule customMobModule, LivingEntity self, CustomMobData mobData) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
        this.self = self;
        this.mobData = mobData;
    }

    @Override
    public void tickLogic(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        // Lógica passiva: pode vaguear, mas não ataca.
        // A movimentação pode ser controlada por PathfinderGoals NMS.
        // Ex: Se não estiver em combate, pode tentar vaguear.
        if (!activeMobInfo.isInCombat()) {
            // Lógica de vaguear (se não for feita por NMS Goal)
        }
    }

    @Override
    public void onTargetAcquired(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        // Mobs passivos geralmente não têm alvos ou não reagem a eles.
    }

    @Override
    public void onDamaged(LivingEntity self, LivingEntity attacker, double damage, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        // Mobs passivos podem fugir ao serem atacados.
        // Esta lógica pode ser implementada aqui ou em um PathfinderGoal NMS.
        if (attacker != null) {
            // Não define isInCombat aqui. Se o atacante for um jogador, EntityDamageListenerMob o fará.
            // A IA ainda pode reagir ao dano (ex: fugir).
            activeMobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
            // Ex: Tentar fugir do 'attacker'
        }
    }

    @Override
    public void onTargetLost(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        // Lógica para quando o mob passivo perde o "foco" se estava fugindo.
        // A lógica para definir isInCombat como false é tratada pela combatStateCheckTask.
    }

    @Override
    public boolean shouldAttemptAbilityUsage(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        return false; // Mobs passivos geralmente não usam habilidades ofensivas.
    }

    @Override
    public AIType getType() {
        return AIType.PASSIVE;
    }
}