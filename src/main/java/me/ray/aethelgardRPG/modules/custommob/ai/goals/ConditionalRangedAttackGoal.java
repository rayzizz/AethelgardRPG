package me.ray.aethelgardRPG.modules.custommob.ai.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;

import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * Um Goal que envolve outro Goal (neste caso, o de ataque com arco)
 * e só permite que ele seja executado se uma condição externa for atendida.
 */
public class ConditionalRangedAttackGoal extends Goal {

    private final RangedBowAttackGoal<?> wrappedGoal;
    private final Supplier<Boolean> executionCondition;

    public ConditionalRangedAttackGoal(RangedBowAttackGoal<?> wrappedGoal, Supplier<Boolean> executionCondition) {
        this.wrappedGoal = wrappedGoal;
        this.executionCondition = executionCondition;

        // --- CORREÇÃO APLICADA AQUI ---
        // Devido a um problema de compatibilidade de tipo com a coleção interna do Minecraft,
        // definimos as flags manualmente. As flags para um ataque com arco são sempre
        // MOVE (para se posicionar) e LOOK (para mirar no alvo).
        // Esta é uma solução robusta e limpa para o problema de compilação.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * O goal só pode ser usado se a nossa condição for verdadeira E
     * se o goal original também puder ser usado.
     */
    @Override
    public boolean canUse() {
        return executionCondition.get() && wrappedGoal.canUse();
    }

    /**
     * O goal só pode continuar se a nossa condição for verdadeira E
     * se o goal original também puder continuar.
     */
    @Override
    public boolean canContinueToUse() {
        return executionCondition.get() && wrappedGoal.canContinueToUse();
    }

    // Delega todas as outras ações para o goal original.
    @Override
    public void start() {
        wrappedGoal.start();
    }

    @Override
    public void stop() {
        wrappedGoal.stop();
    }

    @Override
    public void tick() {
        wrappedGoal.tick();
    }
}