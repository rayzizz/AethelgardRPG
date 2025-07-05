package me.ray.aethelgardRPG.modules.spell.animation.effects;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory; // Adicionado para melhor controle

// Representa a configuração para um efeito sonoro
public class SoundEffect {
    private final Sound sound;
    private final SoundCategory category;
    private final float volume;
    private final float pitch;

    public SoundEffect(Sound sound, SoundCategory category, float volume, float pitch) {
        this.sound = sound;
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void play(Location location) {
        location.getWorld().playSound(location, sound, category, volume, pitch);
    }
}