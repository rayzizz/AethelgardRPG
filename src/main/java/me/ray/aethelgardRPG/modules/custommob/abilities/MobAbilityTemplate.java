package me.ray.aethelgardRPG.modules.custommob.abilities;

import me.ray.aethelgardRPG.AethelgardRPG; // Import adicionado
import org.bukkit.entity.Player; // Import adicionado

import java.util.List;
import java.util.Map;

public class MobAbilityTemplate {
    private final String id;
    private final String displayName;
    private final String displayNameKey; // NOVO: Chave de tradução para o nome de exibição
    private final String type; // Ex: PROJECTILE_DAMAGE, SELF_BUFF
    private final int cooldownTicks;
    private final String targetType; // Ex: ENEMY_PLAYER, SELF
    private final double range;
    private final Map<String, Object> parameters; // Para dados específicos da habilidade (dano, tipo de partícula, etc.)
    private final List<String> description; // Descrição para GUIs ou logs
    private final double hitChance; // Chance da habilidade acertar (0.0 a 1.0)
    private final boolean playerDodgeable; // Se o jogador pode tentar esquivar desta habilidade

    // Construtor atualizado
    public MobAbilityTemplate(String id, String displayName, String displayNameKey, String type, int cooldownTicks,
                              String targetType, double range, Map<String, Object> parameters, List<String> description,
                              double hitChance, boolean playerDodgeable) {
        this.id = id;
        this.displayName = displayName;
        this.displayNameKey = displayNameKey; // Atribuição
        this.type = type;
        this.cooldownTicks = cooldownTicks;
        this.targetType = targetType;
        this.range = range;
        this.parameters = parameters;
        this.description = description;
        this.hitChance = hitChance;
        this.playerDodgeable = playerDodgeable;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDisplayNameKey() { return displayNameKey; } // NOVO GETTER
    public String getType() { return type; }
    public int getCooldownTicks() { return cooldownTicks; }
    public String getTargetType() { return targetType; }
    public double getRange() { return range; }
    public Map<String, Object> getParameters() { return parameters; }
    public List<String> getDescription() { return description; }
    public double getHitChance() { return hitChance; }
    public boolean isPlayerDodgeable() { return playerDodgeable; }

    /**
     * Obtém o nome de exibição traduzido da habilidade.
     * @param playerContext O jogador para contexto de idioma (pode ser null para console/padrão).
     * @return O nome traduzido da habilidade.
     */
    public String getDisplayName(Player playerContext) {
        if (displayNameKey != null && !displayNameKey.isEmpty()) {
            return AethelgardRPG.getInstance().getMessage(playerContext, displayNameKey);
        }
        return displayName; // Fallback para o nome direto se não houver chave
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        return (T) parameters.getOrDefault(key, defaultValue);
    }
}