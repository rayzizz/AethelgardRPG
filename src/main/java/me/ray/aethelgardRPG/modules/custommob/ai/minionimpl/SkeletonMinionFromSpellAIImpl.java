package me.ray.aethelgardRPG.modules.custommob.ai.minionimpl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import org.bukkit.entity.LivingEntity;

public class SkeletonMinionFromSpellAIImpl extends SummonedMinionAIImpl {

    public SkeletonMinionFromSpellAIImpl(AethelgardRPG plugin, CustomMobModule customMobModule, LivingEntity self, CustomMobData mobData) {
        super(plugin, customMobModule, self, mobData);
        // Qualquer lógica de inicialização específica para o esqueleto da magia pode vir aqui,
        // mas por enquanto, o construtor da superclasse é suficiente.
    }

    @Override
    public AIType getType() {
        return AIType.SKELETON_MINION_FROM_SPELL;
    }

    // Você pode sobrescrever outros métodos de SummonedMinionAIImpl aqui
    // se o esqueleto da magia precisar de um comportamento ligeiramente diferente
    // do que um lacaio genérico.
}