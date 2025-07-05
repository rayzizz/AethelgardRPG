package me.ray.aethelgardRPG.modules.custommob.mobs.regen;

/**
 * Armazena os dados de um perfil de regeneração de vida para mobs.
 */
public class RegenProfile {
    private final String id;
    private final long intervalTicks;
    private final double amount;
    private final boolean isPercentage;

    public RegenProfile(String id, long intervalTicks, double amount, boolean isPercentage) {
        this.id = id;
        this.intervalTicks = intervalTicks;
        this.amount = amount;
        this.isPercentage = isPercentage;
    }

    public String getId() {
        return id;
    }

    public long getIntervalTicks() {
        return intervalTicks;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isPercentage() {
        return isPercentage;
    }
}
        