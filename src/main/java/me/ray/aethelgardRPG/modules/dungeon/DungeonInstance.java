package me.ray.aethelgardRPG.modules.dungeon;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public class DungeonInstance {
    private final UUID instanceId;
    private final DungeonTemplate template;
    private final List<Player> players; // Jogadores atualmente na instância
    private long startTimeMillis;
    private int currentPhaseIndex;
    private boolean completed;
    private BukkitTask timerTask; // Para dungeons com limite de tempo

    // private World instanceWorld; // Se cada instância tiver seu próprio mundo (complexo, via SlimeWorldManager ou similar)
    // Ou, se usar uma área no mundo principal, precisa de bounding box e lógica de reset.

    public DungeonInstance(DungeonTemplate template, List<Player> initialPlayers) {
        this.instanceId = UUID.randomUUID();
        this.template = template;
        this.players = new ArrayList<>(initialPlayers);
        this.startTimeMillis = System.currentTimeMillis();
        this.currentPhaseIndex = 0;
        this.completed = false;
    }

    // Getters
    public UUID getInstanceId() { return instanceId; }
    public DungeonTemplate getTemplate() { return template; }
    public List<Player> getPlayers() { return new ArrayList<>(players); } // Retorna cópia
    public long getStartTimeMillis() { return startTimeMillis; }
    public int getCurrentPhaseIndex() { return currentPhaseIndex; }
    public boolean isCompleted() { return completed; }

    // Setters e métodos de controle
    public void setTimerTask(BukkitTask timerTask) { this.timerTask = timerTask; }
    public void cancelTimer() {
        if (this.timerTask != null && !this.timerTask.isCancelled()) {
            this.timerTask.cancel();
        }
    }

    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);
        // TODO: Lógica se todos os jogadores saírem (fechar instância?)
    }

    public void advancePhase() {
        this.currentPhaseIndex++;
        // TODO: Lógica para iniciar a próxima fase (spawnar mobs, etc.)
    }

    public void completeDungeon() {
        this.completed = true;
        cancelTimer();
        // TODO: Lógica de recompensa, teleportar jogadores para fora
    }

    // TODO: Métodos para verificar objetivos da fase, lidar com morte de jogadores na dungeon, etc.
}