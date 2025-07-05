package me.ray.aethelgardRPG.modules.spell.combo;

public enum ClickType {
    // MIDDLE (opcional)
    LEFT("L"),
    RIGHT("R");
    // MIDDLE("MMB"); // Opcional: Botão do Meio

    private final String displayName;

    ClickType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
