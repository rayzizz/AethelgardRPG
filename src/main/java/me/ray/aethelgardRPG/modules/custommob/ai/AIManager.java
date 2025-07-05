package me.ray.aethelgardRPG.modules.custommob.ai;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.ai.impl.AggressiveMeleeAIImpl;
import me.ray.aethelgardRPG.modules.custommob.ai.impl.AggressiveRangedAIImpl;
import me.ray.aethelgardRPG.modules.custommob.ai.impl.PassiveAIImpl;
import me.ray.aethelgardRPG.modules.custommob.ai.impl.StationaryAIImpl;
import me.ray.aethelgardRPG.modules.custommob.ai.minionimpl.SkeletonMinionFromSpellAIImpl;
import me.ray.aethelgardRPG.modules.custommob.ai.minionimpl.SummonedMinionAIImpl;
import org.bukkit.entity.LivingEntity;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Level;

public class AIManager {

    private final AethelgardRPG plugin;
    private final CustomMobModule customMobModule;
    private final Map<AIType, BiFunction<CustomMobData, LivingEntity, MobAI>> aiFactories;

    public AIManager(AethelgardRPG plugin, CustomMobModule customMobModule) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
        this.aiFactories = new EnumMap<>(AIType.class);
        registerDefaultAIs();
    }

    private void registerDefaultAIs() {
        // Para cada tipo de IA, associamos uma função que sabe como criar a classe de IA correspondente.
        aiFactories.put(AIType.PASSIVE, (mobData, self) -> new PassiveAIImpl(plugin, customMobModule, self, mobData));
        aiFactories.put(AIType.AGGRESSIVE_MELEE, (mobData, self) -> new AggressiveMeleeAIImpl(plugin, customMobModule, self, mobData));
        aiFactories.put(AIType.AGGRESSIVE_RANGED, (mobData, self) -> new AggressiveRangedAIImpl(plugin, customMobModule, self, mobData));
        aiFactories.put(AIType.STATIONARY, (mobData, self) -> new StationaryAIImpl(plugin, customMobModule, self, mobData));
        aiFactories.put(AIType.SUMMONED_MINION, (mobData, self) -> new SummonedMinionAIImpl(plugin, customMobModule, self, mobData));
        aiFactories.put(AIType.SKELETON_MINION_FROM_SPELL, (mobData, self) -> new SkeletonMinionFromSpellAIImpl(plugin, customMobModule, self, mobData));
        // Adicione outras IAs aqui conforme forem criadas.
    }

    /**
     * Cria uma instância de MobAI com base no AIType fornecido.
     * @param aiType O tipo de IA a ser criado.
     * @param mobData Os dados de configuração do mob.
     * @param self A entidade LivingEntity que a IA controlará.
     * @return Uma instância de MobAI, ou null se o tipo for VANILLA ou não registrado.
     */
    public MobAI createAI(AIType aiType, CustomMobData mobData, LivingEntity self) {
        if (aiType == null || aiType == AIType.VANILLA) {
            return null; // Nenhuma IA customizada ou usa a IA vanilla.
        }

        BiFunction<CustomMobData, LivingEntity, MobAI> factory = aiFactories.get(aiType);

        if (factory != null) {
            return factory.apply(mobData, self);
        }

        plugin.getLogger().log(Level.WARNING, "Nenhuma fábrica de IA encontrada para o tipo: {0}. O mob {1} pode não ter comportamento customizado.", new Object[]{aiType, mobData.getId()});
        return null;
    }
}