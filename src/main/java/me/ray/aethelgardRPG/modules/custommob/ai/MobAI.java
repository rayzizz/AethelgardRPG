package me.ray.aethelgardRPG.modules.custommob.ai;

import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import org.bukkit.entity.LivingEntity;
// Player import is no longer needed here directly for method signatures
// import org.bukkit.entity.Player;

public interface MobAI {

    /**
     * Chamado a cada tick para a lógica principal da IA.
     * @param self A entidade mob que esta IA controla.
     * @param mobData Os dados base do mob.
     * @param activeMobInfo As informações ativas do mob (vida, cooldowns).
     */
    void tickLogic(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo);

    /**
     * Chamado quando um novo alvo é adquirido.
     * @param self A entidade mob.
     * @param target O novo alvo.
     * @param mobData Os dados base do mob.
     * @param activeMobInfo As informações ativas do mob.
     */
    void onTargetAcquired(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo);

    /**
     * Chamado quando o mob recebe dano.
     * @param self A entidade mob.
     * @param attacker A entidade que causou o dano (pode ser nulo).
     * @param damage A quantidade de dano recebida.
     * @param mobData Os dados base do mob.
     * @param activeMobInfo As informações ativas do mob.
     */
    void onDamaged(LivingEntity self, LivingEntity attacker, double damage, CustomMobData mobData, ActiveMobInfo activeMobInfo);

    /**
     * Chamado quando o mob não tem mais um alvo ou o alvo se torna inválido.
     * @param self A entidade mob.
     * @param mobData Os dados base do mob.
     * @param activeMobInfo As informações ativas do mob.
     */
    void onTargetLost(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo);

    /**
     * Determina se esta IA deve tentar usar habilidades.
     * @param self A entidade mob.
     * @param target O alvo atual (pode ser nulo).
     * @param mobData Os dados base do mob.
     * @param activeMobInfo As informações ativas do mob.
     * @return true se deve tentar usar habilidades, false caso contrário.
     */
    boolean shouldAttemptAbilityUsage(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo);

    /**
     * Retorna o tipo de IA.
     * @return O AIType desta implementação.
     */
    AIType getType();
}
