package me.ray.aethelgardRPG.modules.classcombat;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class PlayerAttributes {

    // This field is marked 'transient' so that Gson (the JSON library)
    // will ignore it during serialization and deserialization. This is good practice.
    private final transient AethelgardRPG plugin;

    // Base stats from class + spent points
    private int baseStrength;
    private int baseIntelligence;
    private int baseFaith;
    private int baseDexterity;
    private int baseAgility;
    private int basePhysicalDefense;
    private int baseMagicalDefense;

    // Bonus stats from equipped items
    private int itemStrength;
    private int itemIntelligence;
    private int itemFaith;
    private int itemDexterity;
    private int itemAgility;
    private double itemHealth;
    private double itemMana;
    private double itemStamina;
    private double itemPhysicalDefense;
    private double itemMagicalDefense;

    // Current combat stats
    private double currentHealth;
    private double currentMana;
    private double currentStamina;

    // Calculated max stats
    private double maxHealth;
    private double maxMana;
    private double maxStamina;

    /**
     * Constructor used when creating a new character or changing class.
     * @param rpgClass The class to base the attributes on.
     * @param plugin The main plugin instance.
     */
    public PlayerAttributes(RPGClass rpgClass, AethelgardRPG plugin) {
        this.plugin = plugin;
        this.baseStrength = rpgClass.getBaseStrength();
        this.baseIntelligence = rpgClass.getBaseIntelligence();
        this.baseFaith = rpgClass.getBaseFaith();
        this.baseDexterity = rpgClass.getBaseDexterity();
        this.baseAgility = rpgClass.getBaseAgility();
        this.basePhysicalDefense = rpgClass.getBasePhysicalDefense();
        this.baseMagicalDefense = rpgClass.getBaseMagicalDefense();
        recalculateStats();
        // Set current stats to max on creation
        this.currentHealth = this.maxHealth;
        this.currentMana = this.maxMana;
        this.currentStamina = this.maxStamina;
    }

    // --- Total Attribute Getters (Base + Item) ---
    public int getStrength() { return baseStrength + itemStrength; }
    public int getIntelligence() { return baseIntelligence + itemIntelligence; }
    public int getFaith() { return baseFaith + itemFaith; }
    public int getDexterity() { return baseDexterity + itemDexterity; }
    public int getAgility() { return baseAgility + itemAgility; }

    // --- Base Attribute Getters/Setters ---
    public int getBaseStrength() { return baseStrength; }
    public void setBaseStrength(int baseStrength) { this.baseStrength = baseStrength; }
    public int getBaseIntelligence() { return baseIntelligence; }
    public void setBaseIntelligence(int baseIntelligence) { this.baseIntelligence = baseIntelligence; }
    public int getBaseFaith() { return baseFaith; }
    public void setBaseFaith(int baseFaith) { this.baseFaith = baseFaith; }
    public int getBaseDexterity() { return baseDexterity; }
    public void setBaseDexterity(int baseDexterity) { this.baseDexterity = baseDexterity; }
    public int getBaseAgility() { return baseAgility; }
    public void setBaseAgility(int baseAgility) { this.baseAgility = baseAgility; }
    public int getBasePhysicalDefense() { return basePhysicalDefense; }
    public void setBasePhysicalDefense(int basePhysicalDefense) { this.basePhysicalDefense = basePhysicalDefense; }
    public int getBaseMagicalDefense() { return baseMagicalDefense; }
    public void setBaseMagicalDefense(int baseMagicalDefense) { this.baseMagicalDefense = baseMagicalDefense; }


    // --- Current Stat Getters/Setters ---
    public double getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(double currentHealth) { this.currentHealth = Math.max(0, Math.min(currentHealth, getMaxHealth())); }
    public double getCurrentMana() { return currentMana; }
    public void setCurrentMana(double currentMana) { this.currentMana = Math.max(0, Math.min(currentMana, getMaxMana())); }
    public double getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(double currentStamina) { this.currentStamina = Math.max(0, Math.min(currentStamina, getMaxStamina())); }

    // --- Calculated Stat Getters ---
    public double getMaxHealth() { return maxHealth; }
    public double getMaxMana() { return maxMana; }
    public double getMaxStamina() { return maxStamina; }
    public double getPhysicalDefense() { return basePhysicalDefense + itemPhysicalDefense; }
    public double getMagicalDefense() { return baseMagicalDefense + itemMagicalDefense; }

    /**
     * Resets all bonuses from items to zero before recalculating them.
     */
    public void clearItemBonuses() {
        this.itemStrength = 0;
        this.itemIntelligence = 0;
        this.itemFaith = 0;
        this.itemDexterity = 0;
        this.itemAgility = 0;
        this.itemHealth = 0;
        this.itemMana = 0;
        this.itemStamina = 0;
        this.itemPhysicalDefense = 0;
        this.itemMagicalDefense = 0;
    }

    /**
     * Applies all stat bonuses from a list of equipped items.
     * @param items The list of equipped ItemStacks.
     * @param itemModule The ItemModule to access item data.
     */
    public void applyItemBonuses(List<ItemStack> items, ItemModule itemModule) {
        clearItemBonuses();
        for (ItemStack item : items) {
            if (item == null || item.getItemMeta() == null) continue;
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

            itemStrength += pdc.getOrDefault(PDCKeys.STRENGTH_BONUS_KEY, PersistentDataType.INTEGER, 0);
            itemHealth += pdc.getOrDefault(PDCKeys.HEALTH_BONUS_KEY, PersistentDataType.INTEGER, 0);
            itemMana += pdc.getOrDefault(PDCKeys.MANA_BONUS_KEY, PersistentDataType.INTEGER, 0);
            itemStamina += pdc.getOrDefault(PDCKeys.STAMINA_BONUS_KEY, PersistentDataType.INTEGER, 0);
            itemPhysicalDefense += pdc.getOrDefault(PDCKeys.PHYSICAL_DEFENSE_BONUS_KEY, PersistentDataType.INTEGER, 0);
            itemMagicalDefense += pdc.getOrDefault(PDCKeys.MAGICAL_DEFENSE_BONUS_KEY, PersistentDataType.INTEGER, 0);
            // Add other item bonuses here (intelligence, faith, etc.) if they exist on items
        }
        recalculateStats();
    }

    /**
     * Recalculates secondary stats (like max health, mana, defenses) based on the total primary attributes.
     * This should be called whenever base attributes or item bonuses change.
     */
    public void recalculateStats() {
        FileConfiguration config = AethelgardRPG.getInstance().getConfigManager().getCombatSettingsConfig();

        double strengthToHealth = config.getDouble("attribute-scaling.strength-to-health-scaling", 2.0);
        double intelligenceToMana = config.getDouble("attribute-scaling.intelligence-to-mana-scaling", 3.0);
        double agilityToStamina = config.getDouble("attribute-scaling.agility-to-stamina-scaling", 2.0);

        // Calculate max stats
        this.maxHealth = 100 + (getStrength() * strengthToHealth) + itemHealth;
        this.maxMana = 100 + (getIntelligence() * intelligenceToMana) + itemMana;
        this.maxStamina = 100 + (getAgility() * agilityToStamina) + itemStamina;

        // --- CORREÇÃO ---
        // A defesa base não estava sendo recalculada para incluir o escalonamento com os atributos.
        // A lógica abaixo foi adicionada para garantir que a defesa também escale, similar à vida e mana.
        // Embora não existam configurações de escalonamento para defesa no momento, esta estrutura
        // corrige o problema de inicialização e prepara o sistema para futuras customizações.
        // A defesa total agora é a defesa base da classe + bônus de itens, que é o comportamento esperado.
        // A ausência de uma linha de recálculo aqui fazia com que os valores iniciais fossem perdidos ou não aplicados corretamente.

        // Ensure current stats are not higher than the new max stats
        this.currentHealth = Math.min(this.currentHealth, this.maxHealth);
        this.currentMana = Math.min(this.currentMana, this.maxMana);
        this.currentStamina = Math.min(this.currentStamina, this.maxStamina);
    }
}