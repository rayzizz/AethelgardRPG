package me.ray.aethelgardRPG.core.utils;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gerencia interações de magias e habilidades com o mundo, como quebrar e regenerar blocos.
 * Esta classe é projetada para ser configurada via código, permitindo que cada
 * magia defina seus próprios parâmetros de destruição e regeneração de forma global.
 */
public final class WorldEffectManager {

    private static AethelgardRPG plugin;

    // Impede a instanciação da classe utilitária
    private WorldEffectManager() {}

    /**
     * Inicializa o gerenciador. Deve ser chamado no onEnable do plugin.
     * @param pluginInstance A instância principal do plugin.
     */
    public static void initialize(AethelgardRPG pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Classe interna para armazenar o estado original de um bloco antes de ser quebrado.
     */
    public static final class OriginalBlockState {
        private final Location location;
        private final BlockData blockData;

        public OriginalBlockState(Block block) {
            this.location = block.getLocation();
            this.blockData = block.getBlockData();
        }

        public Location getLocation() {
            return location;
        }

        public BlockData getBlockData() {
            return blockData;
        }
    }

    /**
     * Cria um efeito de explosão que pode quebrar e, opcionalmente, regenerar blocos em uma área esférica.
     *
     * @param center A localização central do efeito.
     * @param radius O raio da esfera de efeito (em blocos).
     * @param breakChance A chance (de 0.0 a 1.0) de cada bloco na área ser quebrado.
     * @param createFlyingBlocks Se true, cria entidades de blocos voadores para um efeito visual. Se false, apenas quebra os blocos.
     * @param ignoredMaterials Uma lista de materiais que o efeito não deve quebrar (ex: BEDROCK, BARRIER).
     * @param regenerationDelaySeconds O tempo em segundos para o terreno começar a se regenerar. Use 0 ou um valor negativo para não regenerar.
     */
    public static void createBlockExplosion(Location center, int radius, double breakChance, boolean createFlyingBlocks, List<Material> ignoredMaterials, int regenerationDelaySeconds) {
        if (plugin == null) {
            throw new IllegalStateException("WorldEffectManager não foi inicializado! Chame WorldEffectManager.initialize(this) no onEnable do seu plugin.");
        }

        List<Block> blocksToBreak = new ArrayList<>();
        List<OriginalBlockState> blocksToRegenerate = new ArrayList<>();

        // Itera em um cubo e depois checa a distância para formar uma esfera
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (center.clone().add(x, y, z).distanceSquared(center) <= radius * radius) {
                        Block block = center.getWorld().getBlockAt(center.clone().add(x, y, z));

                        if (block.getType() != Material.AIR && !block.isLiquid() && !ignoredMaterials.contains(block.getType())) {
                            if (ThreadLocalRandom.current().nextDouble() <= breakChance) {
                                if (regenerationDelaySeconds > 0) {
                                    blocksToRegenerate.add(new OriginalBlockState(block));
                                }
                                blocksToBreak.add(block);
                            }
                        }
                    }
                }
            }
        }

        if (blocksToBreak.isEmpty()) {
            return;
        }

        // Quebra os blocos
        for (Block block : blocksToBreak) {
            if (createFlyingBlocks) {
                spawnFlyingBlock(block);
            } else {
                block.breakNaturally();
            }
        }

        // Agenda a regeneração se um tempo de delay positivo foi fornecido.
        if (regenerationDelaySeconds > 0 && !blocksToRegenerate.isEmpty()) {
            long delayInTicks = regenerationDelaySeconds * 20L;
            int blocksPerTick = 5; // Quantidade de blocos a regenerar por vez (ajuste para performance)
            new RegenerationTask(blocksToRegenerate, blocksPerTick).runTaskTimer(plugin, delayInTicks, 2L); // Começa após o delay, e roda a cada 2 ticks.
        }
    }

    /**
     * Quebra um único bloco e lança seus detritos para o air como uma entidade FallingBlock.
     *
     * @param block O bloco a ser quebrado e lançado.
     */
    private static void spawnFlyingBlock(Block block) {
        if (block.getType() == Material.AIR) return;

        BlockData blockData = block.getBlockData();
        block.setType(Material.AIR, false); // This is the line that destroys the block

        Location spawnLoc = block.getLocation().add(0.5, 0.5, 0.5);

        FallingBlock fallingBlock = block.getWorld().spawn(spawnLoc, FallingBlock.class, fb -> {
            fb.setBlockData(blockData);
            fb.setDropItem(false);
            fb.setHurtEntities(false);
            fb.setCancelDrop(true);
        });

        double x = ThreadLocalRandom.current().nextDouble() * 0.8 - 0.4;
        double y = ThreadLocalRandom.current().nextDouble() * 0.6 + 0.5;
        double z = ThreadLocalRandom.current().nextDouble() * 0.8 - 0.4;

        fallingBlock.setVelocity(new Vector(x, y, z));
    }

    // --- NOVO MÉTODO: Obtém blocos na área de explosão sem modificá-los ---
    /**
     * Identifica blocos dentro de um raio esférico que seriam afetados por uma explosão,
     * com base em uma chance de quebra e materiais ignorados. NÃO modifica o mundo.
     *
     * @param center A localização central do efeito.
     * @param radius O raio da área esférica (em blocos).
     * @param breakChance A chance (de 0.0 a 1.0) para cada bloco ser considerado "quebrado".
     * @param ignoredMaterials Uma lista de materiais que não devem ser considerados para quebra.
     * @return Uma lista de blocos que seriam afetados.
     */
    public static List<Block> getBlocksInExplosionRadius(Location center, int radius, double breakChance, List<Material> ignoredMaterials) {
        List<Block> affectedBlocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (center.clone().add(x, y, z).distanceSquared(center) <= radius * radius) {
                        Block block = center.getWorld().getBlockAt(center.clone().add(x, y, z));

                        if (block.getType() != Material.AIR && !block.isLiquid() && !ignoredMaterials.contains(block.getType())) {
                            if (ThreadLocalRandom.current().nextDouble() <= breakChance) {
                                affectedBlocks.add(block);
                            }
                        }
                    }
                }
            }
        }
        return affectedBlocks;
    }

    /**
     * Cria um efeito visual de um bloco voando, sem realmente quebrar o bloco original.
     * Isso é útil para efeitos de "explosão" puramente visuais onde o terreno deve permanecer intacto.
     *
     * @param block O bloco a simular voando. Seu tipo e dados serão usados para a entidade FallingBlock.
     * @return A entidade FallingBlock criada, ou null se o bloco for AIR.
     */
    public static FallingBlock createFlyingBlockVisual(Block block) {
        if (block.getType() == Material.AIR) return null;

        BlockData blockData = block.getBlockData();
        Location spawnLoc = block.getLocation().add(0.5, 0.5, 0.5);

        FallingBlock fallingBlock = block.getWorld().spawn(spawnLoc, FallingBlock.class, fb -> {
            fb.setBlockData(blockData);
            fb.setDropItem(false);
            fb.setHurtEntities(false);
            fb.setCancelDrop(true);
            fb.setGravity(true); // Garante que ele caia
        });

        double x = ThreadLocalRandom.current().nextDouble() * 0.8 - 0.4;
        double y = ThreadLocalRandom.current().nextDouble() * 0.6 + 0.5;
        double z = ThreadLocalRandom.current().nextDouble() * 0.8 - 0.4;

        fallingBlock.setVelocity(new Vector(x, y, z));

        return fallingBlock;
    }
}