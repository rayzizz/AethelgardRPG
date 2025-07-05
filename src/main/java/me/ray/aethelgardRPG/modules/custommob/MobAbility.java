package me.ray.aethelgardRPG.modules.custommob;

import me.ray.aethelgardRPG.modules.custommob.abilities.MobAbilityTemplate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player; // Import adicionado

import java.util.List;

public interface MobAbility {
    String getId(); // Identificador único da habilidade
    long getCooldown(); // Cooldown em ticks ou segundos (alterado para long)
    double getRange(); // Alcance da habilidade
    MobAbilityTemplate getTemplate(); // Para acessar parâmetros como hitChance e playerDodgeable

    /**
     * Obtém o nome de exibição traduzido da habilidade.
     * @param playerContext O jogador para contexto de idioma (pode ser null para console/padrão).
     * @return O nome traduzido da habilidade.
     */
    String getDisplayName(Player playerContext);

    /**
     * Executa a habilidade.
     * @param caster A entidade mob que está usando a habilidade.
     * @param targets Lista de alvos potenciais (pode ser nulo ou vazio se a habilidade não tiver alvo).
     */
    void execute(LivingEntity caster, List<Player> targets);
}
