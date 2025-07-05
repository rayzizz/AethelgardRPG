package me.ray.aethelgardRPG.modules.item;

import me.ray.aethelgardRPG.modules.classcombat.RPGClass;

public class ItemRequirements implements Cloneable {
    private int level;
    private int strength;
    private RPGClass rpgClass;

    public ItemRequirements() {
        this.level = 0;
        this.strength = 0;
        this.rpgClass = RPGClass.NONE;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public RPGClass getRpgClass() {
        return rpgClass;
    }

    public void setRpgClass(RPGClass rpgClass) {
        this.rpgClass = rpgClass;
    }

    @Override
    public ItemRequirements clone() {
        try {
            return (ItemRequirements) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}