package me.ray.aethelgardRPG.modules.dungeon.dungeons;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.dungeon.DungeonInstance;
import me.ray.aethelgardRPG.modules.dungeon.DungeonModule;
import me.ray.aethelgardRPG.modules.dungeon.DungeonTemplate;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonManager {

    private final AethelgardRPG plugin;
    private final DungeonModule dungeonModule;
    private final Map<String, DungeonTemplate> dungeonTemplates; // ID do Template -> Template
    private final ConcurrentHashMap<UUID, DungeonInstance> activeInstances; // ID da Instância -> Instância

    public DungeonManager(AethelgardRPG plugin, DungeonModule dungeonModule) {
        this.plugin = plugin;
        this.dungeonModule = dungeonModule;
        this.dungeonTemplates = new HashMap<>();
        this.activeInstances = new ConcurrentHashMap<>();
    }

    public void loadDungeonTemplates() {
        // TODO: Carregar templates de dungeons de arquivos YAML (ex: dungeons/crypt.yml)
        // Cada arquivo definiria um DungeonTemplate.
        plugin.getLogger().info(dungeonTemplates.size() + " templates de dungeon carregados.");
    }

    public DungeonTemplate getDungeonTemplate(String templateId) {
        return dungeonTemplates.get(templateId);
    }

    public Collection<String> getAllDungeonTemplateIds() {
        return dungeonTemplates.keySet();
    }

    public boolean canGroupEnterDungeon(List<Player> group, DungeonTemplate template) {
        if (group.size() < template.getMinPlayers() || group.size() > template.getMaxPlayers()) {
            // Enviar mensagem ao líder do grupo
            return false;
        }
        for (Player p : group) {
            // TODO: Verificar nível do jogador (usar CharacterAPI)
            // if (characterAPI.getPlayerLevel(p) < template.getRequiredLevel()) return false;

            if (isPlayerInAnyDungeon(p)) {
                // CORREÇÃO: Usar o idioma individual do jogador 'p'
                p.sendMessage(plugin.getMessage(p, "dungeon.already-in-dungeon"));
                return false;
            }
        }
        return true;
    }

    public DungeonInstance startDungeon(DungeonTemplate template, List<Player> group) {
        if (!canGroupEnterDungeon(group, template)) {
            // Mensagem de erro já deve ter sido enviada
            return null;
        }

        DungeonInstance instance = new DungeonInstance(template, group);
        activeInstances.put(instance.getInstanceId(), instance);

        // TODO: Teleportar jogadores para a entrada da dungeon
        // Location entryPoint = template.getEntryLocation();
        // group.forEach(p -> p.teleport(entryPoint));

        // TODO: Iniciar a primeira fase da dungeon
        // instance.advancePhase(); // Ou uma lógica mais específica para iniciar

        if (template.getTimeLimitSeconds() > 0) {
            BukkitRunnable timer = new BukkitRunnable() {
                long timeLeft = template.getTimeLimitSeconds();
                @Override
                public void run() {
                    if (instance.isCompleted() || !activeInstances.containsKey(instance.getInstanceId())) {
                        this.cancel();
                        return;
                    }
                    timeLeft--;
                    // TODO: Enviar mensagem de tempo restante para os jogadores na instância
                    if (timeLeft <= 0) {
                        failDungeon(instance, "Tempo esgotado!");
                        this.cancel();
                    }
                }
            };
            instance.setTimerTask(timer.runTaskTimer(plugin, 20L, 20L));
        }

        // CORREÇÃO: Usar o idioma individual de cada jogador no grupo
        group.forEach(p -> p.sendMessage(plugin.getMessage(p, "dungeon.entered", template.getName())));
        return instance;
    }

    public void failDungeon(DungeonInstance instance, String reason) {
        // CORREÇÃO: Usar o idioma individual de cada jogador na instância
        instance.getPlayers().forEach(p -> p.sendMessage(plugin.getMessage(p, "dungeon.failed", instance.getTemplate().getName(), reason)));
        shutdownDungeonInstance(instance.getInstanceId());
    }

    public void shutdownDungeonInstance(UUID instanceId) {
        DungeonInstance instance = activeInstances.remove(instanceId);
        if (instance != null) {
            instance.cancelTimer();
            // TODO: Limpar mobs da instância, resetar área (se aplicável)
        }
    }

    public void shutdownAllDungeons() {
        activeInstances.keySet().forEach(this::shutdownDungeonInstance);
        plugin.getLogger().info("Todas as instâncias de dungeon ativas foram encerradas.");
    }

    public boolean isPlayerInAnyDungeon(Player player) {
        return activeInstances.values().stream().anyMatch(inst -> inst.getPlayers().contains(player));
    }

    // TODO: Métodos para lidar com morte de mobs/bosses na dungeon, progresso de objetivos, etc.
}