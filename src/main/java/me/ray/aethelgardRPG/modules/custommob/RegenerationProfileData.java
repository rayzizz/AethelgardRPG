package me.ray.aethelgardRPG.modules.custommob;

public class RegenerationProfileData {
    private final boolean enabled;
    private final boolean isPercentage;
    private final double amount;
    private final int intervalTicks;
    private final boolean particleEnabled;
    private final String particleType;
    private final int particleCount;
    private final double particleOffsetX;
    private final double particleOffsetY;
    private final double particleOffsetZ;
    private final double particleSpeed;
    private final boolean floatingTextEnabled;
    private final String floatingTextFormat;
    private final long floatingTextDurationTicks;

    public RegenerationProfileData(boolean enabled, boolean isPercentage, double amount, int intervalTicks,
                                   boolean particleEnabled, String particleType, int particleCount,
                                   double particleOffsetX, double particleOffsetY, double particleOffsetZ, double particleSpeed,
                                   boolean floatingTextEnabled, String floatingTextFormat, long floatingTextDurationTicks) {
        this.enabled = enabled;
        this.isPercentage = isPercentage;
        this.amount = amount;
        this.intervalTicks = intervalTicks;
        this.particleEnabled = particleEnabled;
        this.particleType = particleType;
        this.particleCount = particleCount;
        this.particleOffsetX = particleOffsetX;
        this.particleOffsetY = particleOffsetY;
        this.particleOffsetZ = particleOffsetZ;
        this.particleSpeed = particleSpeed;
        this.floatingTextEnabled = floatingTextEnabled;
        this.floatingTextFormat = floatingTextFormat;
        this.floatingTextDurationTicks = floatingTextDurationTicks;
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isPercentage() { return isPercentage; }
    public double getAmount() { return amount; }
    public int getIntervalTicks() { return intervalTicks; }
    public boolean isParticleEnabled() { return particleEnabled; }
    public String getParticleType() { return particleType; }
    public int getParticleCount() { return particleCount; }
    public double getParticleOffsetX() { return particleOffsetX; }
    public double getParticleOffsetY() { return particleOffsetY; }
    public double getParticleOffsetZ() { return particleOffsetZ; }
    public double getParticleSpeed() { return particleSpeed; }
    public boolean isFloatingTextEnabled() { return floatingTextEnabled; }
    public String getFloatingTextFormat() { return floatingTextFormat; }
    public long getFloatingTextDurationTicks() { return floatingTextDurationTicks; }
}