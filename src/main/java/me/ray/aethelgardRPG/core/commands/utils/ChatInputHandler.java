package me.ray.aethelgardRPG.core.commands.utils;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {

    private static final Map<UUID, Consumer<String>> waitingForInput = new HashMap<>();
    private static final Map<UUID, BukkitTask> inputTimeouts = new HashMap<>();
    private static AethelgardRPG pluginInstance;

    public ChatInputHandler(AethelgardRPG plugin) {
        pluginInstance = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void requestPlayerInput(Player player, Consumer<String> callback, String timeoutMessage) {
        waitingForInput.put(player.getUniqueId(), callback);

        // Timeout para a entrada
        BukkitTask oldTask = inputTimeouts.remove(player.getUniqueId());
        if (oldTask != null) oldTask.cancel();

        BukkitTask newTask = Bukkit.getScheduler().runTaskLater(pluginInstance, () -> {
            if (waitingForInput.containsKey(player.getUniqueId())) {
                waitingForInput.remove(player.getUniqueId());
                player.sendMessage(timeoutMessage);
            }
        }, 20L * 30); // 30 segundos de timeout
        inputTimeouts.put(player.getUniqueId(), newTask);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (waitingForInput.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            Consumer<String> callback = waitingForInput.remove(player.getUniqueId());
            BukkitTask task = inputTimeouts.remove(player.getUniqueId());
            if (task != null) task.cancel();
            Bukkit.getScheduler().runTask(pluginInstance, () -> callback.accept(event.getMessage()));
        }
    }
}