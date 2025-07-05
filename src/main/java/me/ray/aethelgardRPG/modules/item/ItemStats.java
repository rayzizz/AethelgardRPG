package me.ray.aethelgardRPG.modules.item;

public class ItemStats implements Cloneable {
    private double physicalDamage;
    private double magicalDamage;
    private double healthBonus;
    private double manaBonus;
    private double strengthBonus;
    private double intelligenceBonus;
    private double faithBonus;
    private double dexterityBonus;
    private double agilityBonus;
    private double staminaBonus;
    private double physicalDefenseBonus;
    private double magicalDefenseBonus;

    public ItemStats() {
        // Default constructor initializes all to 0 or appropriate defaults
    }

    public double getPhysicalDamage() {
        return physicalDamage;
    }

    public void setPhysicalDamage(double physicalDamage) {
        this.physicalDamage = physicalDamage;
    }

    public double getMagicalDamage() {
        return magicalDamage;
    }

    public void setMagicalDamage(double magicalDamage) {
        this.magicalDamage = magicalDamage;
    }

    public double getHealthBonus() {
        return healthBonus;
    }

    public void setHealthBonus(double healthBonus) {
        this.healthBonus = healthBonus;
    }

    public double getManaBonus() {
        return manaBonus;
    }

    public void setManaBonus(double manaBonus) {
        this.manaBonus = manaBonus;
    }

    public double getStrengthBonus() {
        return strengthBonus;
    }

    public void setStrengthBonus(double strengthBonus) {
        this.strengthBonus = strengthBonus;
    }

    public double getIntelligenceBonus() { return intelligenceBonus; }

    public void setIntelligenceBonus(double intelligenceBonus) { this.intelligenceBonus = intelligenceBonus; }

    public double getFaithBonus() { return faithBonus; }

    public void setFaithBonus(double faithBonus) { this.faithBonus = faithBonus; }

    public double getDexterityBonus() { return dexterityBonus; }

    public void setDexterityBonus(double dexterityBonus) { this.dexterityBonus = dexterityBonus; }

    public double getAgilityBonus() { return agilityBonus; }

    public void setAgilityBonus(double agilityBonus) { this.agilityBonus = agilityBonus; }

    public double getStaminaBonus() { return staminaBonus; }

    public void setStaminaBonus(double staminaBonus) { this.staminaBonus = staminaBonus; }

    public double getPhysicalDefenseBonus() {
        return physicalDefenseBonus;
    }

    public void setPhysicalDefenseBonus(double physicalDefenseBonus) {
        this.physicalDefenseBonus = physicalDefenseBonus;
    }

    public double getMagicalDefenseBonus() {
        return magicalDefenseBonus;
    }

    public void setMagicalDefenseBonus(double magicalDefenseBonus) {
        this.magicalDefenseBonus = magicalDefenseBonus;
    }

    /**
     * Checks if any stat bonus is greater than zero.
     * @return true if any stat has a bonus, false otherwise.
     */
    public boolean hasAnyStat() {
        return physicalDamage > 0 || magicalDamage > 0 ||
                healthBonus > 0 || manaBonus > 0 || staminaBonus > 0 ||
                strengthBonus > 0 || intelligenceBonus > 0 ||
                faithBonus > 0 || dexterityBonus > 0 || agilityBonus > 0 ||
                physicalDefenseBonus > 0 || magicalDefenseBonus > 0;
    }

    @Override
    public ItemStats clone() {
        try {
            return (ItemStats) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}