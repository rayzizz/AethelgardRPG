package me.ray.aethelgardRPG.modules.spell.combo;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ChatColor;

public class PlayerComboInput {
    private static final int MAX_CLICKS = 3;
    private final List<ClickInput> inputs;
    private final long maxTimeBetweenClicksMillis; // Tempo máximo entre cliques para continuar o combo
    private static final String CLICK_COLOR = ChatColor.YELLOW.toString(); // Cor para os botões (ex: Amarelo)
    private static final String SEPARATOR_COLOR = ChatColor.DARK_GRAY.toString(); // Cor para o separador (ex: Cinza Escuro)
    
    public PlayerComboInput(long maxTimeBetweenClicksMillis) {
        this.inputs = new ArrayList<>(MAX_CLICKS);
        this.maxTimeBetweenClicksMillis = maxTimeBetweenClicksMillis;
    }

    public void addClick(ClickType clickType) {
        long currentTime = System.currentTimeMillis();
        if (!inputs.isEmpty()) {
            ClickInput lastInput = inputs.get(inputs.size() - 1);
            if (currentTime - lastInput.timestamp() > maxTimeBetweenClicksMillis) {
                inputs.clear(); // Combo expirou, começa de novo
            }
        }

        inputs.add(new ClickInput(clickType, currentTime));
        if (inputs.size() > MAX_CLICKS) {
            inputs.remove(0); // Mantém apenas os últimos MAX_CLICKS
        }
    }
     

    public boolean isComplete() {
        return inputs.size() == MAX_CLICKS;
    }

    public Combo getAsCombo() {
        if (!isComplete()) {
            return null;
        }
        return new Combo(inputs.get(0).clickType(), inputs.get(1).clickType(), inputs.get(2).clickType());
    }
    /**
     * Retorna uma string formatada representando o input atual do combo para exibição.
     * Exemplo: "LMB", "LMB > RMB"
     */
    public String getCurrentInputDisplayString() {
        if (inputs.isEmpty()) {
            return ""; // Retorna string vazia se não houver inputs
        }
        return inputs.stream()
                .map(clickInput -> CLICK_COLOR + clickInput.clickType().getDisplayName())
                .collect(java.util.stream.Collectors.joining(SEPARATOR_COLOR + " - " + CLICK_COLOR));
    }
    
    public boolean isEmpty() {
        return inputs.isEmpty();
    }
    public void clear() {
        inputs.clear();
    }

    private record ClickInput(ClickType clickType, long timestamp) {}
}
