package me.ray.aethelgardRPG.modules.skill.api;

import me.ray.aethelgardRPG.modules.skill.SkillType;
import org.bukkit.entity.Player;

public interface SkillAPI {

    /**
     * Obtém o nível de uma habilidade específica para um jogador.
     * @param player O jogador.
     * @param skillType O tipo de habilidade.
     * @return O nível da habilidade do jogador, ou 0 se não encontrado.
     */
    int getSkillLevel(Player player, SkillType skillType);

    /**
     * Adiciona experiência a uma habilidade específica de um jogador.
     */
    void addSkillExperience(Player player, SkillType skillType, double amount);

    // Outros métodos: getMaxLevel(SkillType), getExperienceForLevel(SkillType, int), etc.
}