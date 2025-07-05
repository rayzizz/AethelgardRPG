package me.ray.aethelgardRPG.modules.spell.animation;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class AnimationSequence implements Animation {
    private final List<AnimationFrame> frames;
    private final List<Long> frameDelaysTicks;

    public AnimationSequence() {
        this.frames = new ArrayList<>();
        this.frameDelaysTicks = new ArrayList<>();
    }

    public AnimationSequence addFrame(AnimationFrame frame, long delayTicksBeforeFrame) {
        this.frames.add(frame);
        this.frameDelaysTicks.add(delayTicksBeforeFrame);
        return this;
    }

    @Override
    public void run(SpellCastContext context, AethelgardRPG plugin) {
        if (frames.isEmpty()) {
            return;
        }

        long cumulativeDelay = 0;
        for (int i = 0; i < frames.size(); i++) {
            final AnimationFrame frame = frames.get(i);
            long delayForThisFrame = frameDelaysTicks.get(i);

            cumulativeDelay += delayForThisFrame;

            // Otimização: Agenda cada frame de uma vez com o delay acumulado.
            // Evita o uso de um BukkitRunnable rodando a cada tick.
            new BukkitRunnable() {
                @Override
                public void run() {
                    frame.execute(context, plugin);
                }
            }.runTaskLater(plugin, cumulativeDelay);
        }
    }
}