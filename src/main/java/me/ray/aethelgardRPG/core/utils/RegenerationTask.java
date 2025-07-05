package me.ray.aethelgardRPG.core.utils;

import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;

/**
 * Uma tarefa agendada para regenerar blocos destruídos de forma gradual,
 * evitando picos de lag no servidor.
 */
public class RegenerationTask extends BukkitRunnable {

    private final List<WorldEffectManager.OriginalBlockState> blocksToRestore;
    private final int blocksPerTick;
    private int currentIndex = 0;

    /**
     * Construtor da tarefa de regeneração.
     * @param blocksToRestore A lista de estados de blocos originais a serem restaurados.
     * @param blocksPerTick   O número de blocos a serem restaurados por execução da tarefa.
     */
    public RegenerationTask(List<WorldEffectManager.OriginalBlockState> blocksToRestore, int blocksPerTick) {
        this.blocksToRestore = blocksToRestore;
        this.blocksPerTick = Math.max(1, blocksPerTick); // Garante que pelo menos 1 bloco seja restaurado.
    }

    @Override
    public void run() {
        // Itera e restaura um número definido de blocos por vez.
        for (int i = 0; i < blocksPerTick; i++) {
            if (currentIndex >= blocksToRestore.size()) {
                this.cancel(); // Todos os blocos foram restaurados, cancela a tarefa.
                return;
            }

            WorldEffectManager.OriginalBlockState originalState = blocksToRestore.get(currentIndex);
            Block block = originalState.getLocation().getBlock();

            // Restaura o bloco para seu estado original sem causar atualizações de física em cascata.
            block.setBlockData(originalState.getBlockData(), false);

            currentIndex++;
        }
    }
}