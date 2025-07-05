package me.ray.aethelgardRPG.modules.skill;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.entity.Player;

public enum SkillType {
    MINERACAO("skill.type.mining.name", "skill.type.mining.description"),
    PESCA("skill.type.fishing.name", "skill.type.fishing.description"),
    ALQUIMIA("skill.type.alchemy.name", "skill.type.alchemy.description"),
    CULINARIA("skill.type.cooking.name", "skill.type.cooking.description"),
    HERBALISMO("skill.type.herbalism.name", "skill.type.herbalism.description"),
    FORJA("skill.type.forging.name", "skill.type.forging.description");

    private final String nameKey;
    private final String descriptionKey;

    SkillType(String nameKey, String descriptionKey) {
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    /**
     * Obtém o nome de exibição traduzido para esta habilidade.
     * @param player O jogador para obter o contexto de idioma. Pode ser nulo para usar o padrão do servidor.
     * @return O nome traduzido da habilidade.
     */
    public String getDisplayName(Player player) {
        return AethelgardRPG.getInstance().getLanguageManager().getMessage(player, this.nameKey);
    }

    /**
     * Obtém a descrição traduzida para esta habilidade.
     * @param player O jogador para obter o contexto de idioma. Pode ser nulo para usar o padrão do servidor.
     * @return A descrição traduzida da habilidade.
     */
    public String getDescription(Player player) {
        return AethelgardRPG.getInstance().getLanguageManager().getMessage(player, this.descriptionKey);
    }
}