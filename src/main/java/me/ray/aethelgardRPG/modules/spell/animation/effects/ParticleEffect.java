package me.ray.aethelgardRPG.modules.spell.animation.effects;

import org.bukkit.Location;
import org.bukkit.Particle;

// Representa a configuração para um efeito de partícula
public class ParticleEffect {
    private final Particle particleType;
    private final int count;
    private final double offsetX, offsetY, offsetZ;
    private final double speed;
    private final Object data; // Pode ser Color para REDSTONE, etc.

    public ParticleEffect(Particle particleType, int count, double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        this.particleType = particleType;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.speed = speed;
        this.data = data;
    }

    public void play(Location location) {
        location.getWorld().spawnParticle(particleType, location, count, offsetX, offsetY, offsetZ, speed, data);
    }
}