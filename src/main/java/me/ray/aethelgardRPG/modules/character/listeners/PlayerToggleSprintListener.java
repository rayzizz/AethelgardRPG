package me.ray.aethelgardRPG.modules.character.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.admin.AdminModule;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerToggleSprintListener implements Listener {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final AdminModule adminModule;
    private final Map<UUID, BukkitTask> sprintDrainTasks = new ConcurrentHashMap<>();

    public PlayerToggleSprintListener(AethelgardRPG plugin, CharacterModule characterModule) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.adminModule = plugin.getModuleManager().getModule(AdminModule.class);
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        boolean isInfiniteStats = adminModule != null && adminModule.isInfiniteStatsEnabled(playerUUID);

        if (event.isSprinting()) {
            // Player TENTA começar a sprintar.
            Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
            if (playerDataOpt.isEmpty()) {
                event.setCancelled(true);
                return;
            }
            CharacterData characterData = playerDataOpt.get();
            PlayerAttributes attributes = characterData.getAttributes();

            if (isInfiniteStats) {
                // Se stats infinitos, garante que a stamina esteja no máximo e não inicia o dreno.
                attributes.setCurrentStamina(attributes.getMaxStamina());
                stopStaminaDrain(playerUUID); // Garante que qualquer tarefa de dreno existente seja parada
                characterData.setLastSprintTimeMillis(System.currentTimeMillis()); // Ainda atualiza o tempo do último sprint
            } else {
                // Lógica normal de verificação e dreno de stamina
                FileConfiguration config = plugin.getConfigManager().getCharacterSettingsConfig();
                double minStaminaToStart = config.getDouble("sprint.min-stamina-to-start", 1.0);
                double minStaminaPercentageToStart = config.getDouble("sprint.min-stamina-percentage-to-start", 0.3);

                double currentStamina = attributes.getCurrentStamina();
                double maxStamina = attributes.getMaxStamina();
                double staminaPercentage = maxStamina > 0 ? currentStamina / maxStamina : 0.0;

                // A verificação de 30% acontece AQUI, ao iniciar o sprint.
                if (currentStamina < minStaminaToStart || staminaPercentage < minStaminaPercentageToStart) {
                    event.setCancelled(true);
                    player.setSprinting(false);

                    // CORREÇÃO: Aplica um efeito de lentidão para forçar a parada do sprint no cliente.
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2, 10, false, false, false));

                    player.sendMessage(plugin.getMessage(player, "character.sprint.no-stamina"));
                    return;
                }
                characterData.setLastSprintTimeMillis(System.currentTimeMillis());
                startStaminaDrain(player, characterData);
            }
        } else {
            // Player para de sprintar (por vontade própria ou por outra razão).
            if (isInfiniteStats) {
                // Se stats infinitos, garante que a stamina esteja no máximo e não há tarefa de dreno.
                Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
                playerDataOpt.ifPresent(cd -> cd.getAttributes().setCurrentStamina(cd.getAttributes().getMaxStamina()));
                stopStaminaDrain(playerUUID); // Garante que qualquer tarefa de dreno existente seja parada
            } else {
                // Comportamento normal de parar o dreno.
                stopStaminaDrain(playerUUID);
                Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
                playerDataOpt.ifPresent(playerData -> playerData.setLastSprintTimeMillis(System.currentTimeMillis()));
            }
        }
    }

    private void startStaminaDrain(Player player, CharacterData characterData) {
        UUID playerUUID = player.getUniqueId();
        stopStaminaDrain(playerUUID); // Garante que não haja tarefas duplicadas

        // Verifica novamente por stats infinitos, caso tenha sido ativado durante o sprint
        if (adminModule != null && adminModule.isInfiniteStatsEnabled(playerUUID)) {
            return; // Não inicia o dreno se stats infinitos estiverem ativados
        }

        FileConfiguration config = plugin.getConfigManager().getCharacterSettingsConfig();
        long drainInterval = config.getLong("sprint.drain-interval-ticks", 10L);
        double drainAmount = config.getDouble("sprint.drain-amount", 2.0);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Re-verifica stats infinitos dentro do runnable, caso tenha sido ativado no meio do sprint
                if (adminModule != null && adminModule.isInfiniteStatsEnabled(playerUUID)) {
                    stopStaminaDrain(playerUUID);
                    // Garante que a stamina esteja no máximo
                    Optional<CharacterData> currentDataOpt = characterModule.getCharacterData(player);
                    currentDataOpt.ifPresent(cd -> cd.getAttributes().setCurrentStamina(cd.getAttributes().getMaxStamina()));
                    return;
                }
                if (!player.isOnline() || !player.isSprinting()) {
                    stopStaminaDrain(playerUUID);
                    return;
                }

                PlayerAttributes attributes = characterData.getAttributes();
                double currentStamina = attributes.getCurrentStamina();

                // O jogador só para se a stamina acabar completamente.
                if (currentStamina <= 0) {
                    attributes.setCurrentStamina(0); // Garante que não fique negativo
                    player.setSprinting(false);

                    // Aplica um efeito de lentidão para forçar a parada do sprint no cliente.
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2, 10, false, false, false));

                    player.sendMessage(plugin.getMessage(player, "character.sprint.no-stamina"));
                    stopStaminaDrain(playerUUID);
                    return;
                }

                // Se a stamina for suficiente, continua drenando.
                attributes.setCurrentStamina(currentStamina - drainAmount);
                characterData.setLastSprintTimeMillis(System.currentTimeMillis());
            }
        }.runTaskTimer(plugin, 0L, drainInterval);

        sprintDrainTasks.put(playerUUID, task);
    }

    private void stopStaminaDrain(UUID playerUUID) {
        BukkitTask existingTask = sprintDrainTasks.remove(playerUUID);
        if (existingTask != null) {
            existingTask.cancel();
        }
    }

    public void cancelAllSprintTasks() {
        sprintDrainTasks.values().forEach(BukkitTask::cancel);
        sprintDrainTasks.clear();
    }
}