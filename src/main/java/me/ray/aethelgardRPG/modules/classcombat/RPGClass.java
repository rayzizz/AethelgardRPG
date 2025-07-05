package me.ray.aethelgardRPG.modules.classcombat;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public enum RPGClass {

    // Definição das classes com seus atributos base, ícones e SÍMBOLOS
    // Formato: (chaveNome, chaveDescricao, icone, simbolo, vidaInicial, manaInicial, forca, inteligencia, fe, destreza, agilidade, defFisica, defMagica, stamina)

    NONE(
            "class.none.name", "class.none.description", Material.PAPER, "❓", // Símbolo para classe "Nenhuma"
            20, 0, 0, 0, 0, 0, 0, 0, 0, 100
    ),
    GUERREIRO(
            "class.guerreiro.name", "class.guerreiro.description", Material.IRON_SWORD, "⚔\uFE0F", // Símbolo para Guerreiro
            120, 20, 15, 5, 5, 10, 8, 10, 2, 100
    ),
    MAGO(
            "class.mago.name", "class.mago.description", Material.BLAZE_ROD, "☄\uFE0F", // Símbolo para Mago
            80, 100, 5, 15, 8, 5, 10, 2, 10, 100
    ),
    ARQUEIRO(
            "class.arqueiro.name", "class.arqueiro.description", Material.BOW, "\uD83C\uDFF9", // Símbolo para Arqueiro
            100, 40, 8, 8, 5, 15, 12, 6, 6, 120
    ),
    NECROMANTE(
            "class.necromante.name", "class.necromante.description", Material.BONE, "⚰\uFE0F", // Símbolo para Necromante
            90, 90, 5, 12, 12, 5, 8, 4, 8, 100
    ),
    CLERIGO(
            "class.clerigo.name", "class.clerigo.description", Material.GOLDEN_AXE, "❤\uFE0F", // Símbolo para Clérigo
            110, 80, 10, 8, 15, 5, 5, 8, 8, 100
    ),
    ASSASSINO(
            "class.assassino.name", "class.assassino.description", Material.IRON_SWORD, "\uD83D\uDDE1\uFE0F", // Símbolo para Assassino
            90, 50, 10, 5, 5, 12, 15, 5, 5, 130
    );

    private final String displayNameKey;
    private final String descriptionKey;
    private final Material icon;
    private final String symbol; // NOVO CAMPO: Símbolo da classe
    private final double startingHealth;
    private final double startingMana;
    private final int baseStrength;
    private final int baseIntelligence;
    private final int baseFaith;
    private final int baseDexterity;
    private final int baseAgility;
    private final int basePhysicalDefense;
    private final int baseMagicalDefense;
    private final double baseStamina;

    // CONSTRUTOR ATUALIZADO para incluir o símbolo
    RPGClass(String displayNameKey, String descriptionKey, Material icon, String symbol, double startingHealth, double startingMana,
             int baseStrength, int baseIntelligence, int baseFaith, int baseDexterity, int baseAgility,
             int basePhysicalDefense, int baseMagicalDefense, double baseStamina) {
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
        this.icon = icon;
        this.symbol = symbol; // Atribui o novo campo
        this.startingHealth = startingHealth;
        this.startingMana = startingMana;
        this.baseStrength = baseStrength;
        this.baseIntelligence = baseIntelligence;
        this.baseFaith = baseFaith;
        this.baseDexterity = baseDexterity;
        this.baseAgility = baseAgility;
        this.basePhysicalDefense = basePhysicalDefense;
        this.baseMagicalDefense = baseMagicalDefense;
        this.baseStamina = baseStamina;
    }

    public String getDisplayName(Player player) {
        return AethelgardRPG.getInstance().getLanguageManager().getMessage(player, displayNameKey);
    }

    public String getDisplayDescription(Player player) {
        return AethelgardRPG.getInstance().getLanguageManager().getMessage(player, descriptionKey);
    }

    public Material getIcon() {
        return icon;
    }

    // NOVO GETTER para o símbolo
    public String getSymbol() {
        return symbol;
    }

    // Método para compatibilidade com a GUI de fallback
    public String getDescription(Player player) {
        return getDisplayDescription(player);
    }

    public double getStartingHealth() {
        return startingHealth;
    }

    public double getStartingMana() {
        return startingMana;
    }

    // Getters para os atributos base (usados pelo PlayerAttributes e CharacterModule)
    public int getBaseStrength() { return baseStrength; }
    public int getBaseIntelligence() { return baseIntelligence; }
    public int getBaseFaith() { return baseFaith; }
    public int getBaseDexterity() { return baseDexterity; }
    public int getBaseAgility() { return baseAgility; }
    public int getBasePhysicalDefense() { return basePhysicalDefense; }
    public int getBaseMagicalDefense() { return baseMagicalDefense; }
    public double getBaseStamina() { return baseStamina; }

    public static RPGClass fromString(String className) {
        if (className == null) {
            return NONE;
        }
        try {
            return RPGClass.valueOf(className.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}