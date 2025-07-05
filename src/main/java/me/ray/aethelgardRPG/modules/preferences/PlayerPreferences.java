package me.ray.aethelgardRPG.modules.preferences;

/**
 * Armazena as preferências individuais de um jogador.
 */
public class PlayerPreferences {

    private boolean showSpellParticles;
    private boolean showMapEffectVisuals; // Renomeado de allowMapBreaking

    // Construtor com valores padrão
    public PlayerPreferences() {
        this.showSpellParticles = true;
        this.showMapEffectVisuals = true; // Valor padrão para o novo nome
    }

    // --- Getters ---
    public boolean canShowSpellParticles() {
        return showSpellParticles;
    }

    public boolean canShowMapEffectVisuals() { // Getter renomeado
        return showMapEffectVisuals;
    }

    // --- Setters (para alterar as preferências em tempo real) ---
    public void setShowSpellParticles(boolean showSpellParticles) {
        this.showSpellParticles = showSpellParticles;
    }

    public void setShowMapEffectVisuals(boolean showMapEffectVisuals) { // Setter renomeado
        this.showMapEffectVisuals = showMapEffectVisuals;
    }
}