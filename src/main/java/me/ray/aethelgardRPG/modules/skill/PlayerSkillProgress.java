package me.ray.aethelgardRPG.modules.skill;

public class PlayerSkillProgress {
    private SkillType skillType;
    private int level;
    private double experience;

    // Construtor padrão para Gson
    public PlayerSkillProgress() {
        // Inicializa com valores padrão, se necessário
        this.level = 1;
        this.experience = 0;
    }

    public PlayerSkillProgress(SkillType skillType) {
        this.skillType = skillType;
        this.level = 1; // Nível inicial
        this.experience = 0;
    }

    public SkillType getSkillType() {
        return skillType;
    }

    public void setSkillType(SkillType skillType) {
        this.skillType = skillType;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }

    public void addExperience(double amount) {
        this.experience += amount;
        // Lógica de upar de nível será gerenciada pelo SkillManager
    }
}