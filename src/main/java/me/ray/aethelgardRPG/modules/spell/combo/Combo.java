package me.ray.aethelgardRPG.modules.spell.combo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;

public class Combo {
    private final List<ClickType> clicks;
    private static final String CLICK_COLOR = ChatColor.YELLOW.toString(); // Cor para os botões (ex: Amarelo)
    private static final String SEPARATOR_COLOR = ChatColor.DARK_GRAY.toString(); // Cor para o separador (ex: Cinza Escuro)

    private static final String LOCKED_CLICK_COLOR = ChatColor.YELLOW.toString(); // Cor para os botões no formato travado
    private static final String LOCKED_DASH_SEPARATOR_COLOR = ChatColor.GRAY.toString(); // Cor para o separador " - "

    // Construtor padrão para Gson (pode ser privado se Gson for configurado para acessar campos)
    // Ou, se os campos forem públicos, não precisa.
    // Para List<ClickType>, Gson pode desserializar se ClickType for um enum ou tiver um construtor padrão.
    private Combo() {
        this.clicks = null; // Será preenchido pelo Gson
    }

    public Combo(ClickType... clicks) {
        if (clicks == null || clicks.length != 3) {
            throw new IllegalArgumentException("Combo must consist of exactly 3 clicks.");
        }
        this.clicks = Arrays.asList(clicks);
    }

    public List<ClickType> getClicks() {
        return clicks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Combo combo = (Combo) o;
        return Objects.equals(clicks, combo.clicks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clicks);
    }

    @Override
    public String toString() {
        // Pode ser mantido como está para logs ou alterado para usar getDisplayString()
        return clicks.toString();
    }

    /**
     * Retorna uma string formatada representando o combo para exibição.
     * Exemplo: "LMB - RMB - LMB"
     */
    public String getDisplayString() {
        if (clicks == null) return ""; // Proteção para desserialização
        return this.clicks.stream()
                .map(clickType -> CLICK_COLOR + clickType.getDisplayName())
                .collect(Collectors.joining(SEPARATOR_COLOR + " - " + CLICK_COLOR));
    }

    /**
     * Retorna uma string formatada representando o combo "travado" para exibição.
     * Exemplo: "L - R - L" com cores.
     */
    public String getLockedInDisplayString() {
        if (clicks == null) return ""; // Proteção para desserialização
        return this.clicks.stream()
                .map(clickType -> LOCKED_CLICK_COLOR + clickType.getDisplayName())
                .collect(Collectors.joining(LOCKED_DASH_SEPARATOR_COLOR + " - " + LOCKED_CLICK_COLOR));
    }
}