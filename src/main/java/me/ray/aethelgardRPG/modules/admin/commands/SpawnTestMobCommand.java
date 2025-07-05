package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData; // Import CustomMobData
import me.ray.aethelgardRPG.modules.custommob.api.CustomMobAPI; // Import CustomMobAPI
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity; // Import LivingEntity
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnTestMobCommand implements CommandExecutor {

    private final AethelgardRPG plugin;

    public SpawnTestMobCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command")); // Console message
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("aethelgardrpg.admin.chat.spawntestmob")) { // Permissão para o comando
            player.sendMessage(plugin.getMessage(player, "general.no-permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.spawntestmob.usage"));
            return true;
        }

        String mobId = args[0];

        CustomMobAPI customMobAPI = plugin.getCustomMobAPI(); // Corrigido: Obtém a API
        if (customMobAPI != null) {
            // CORREÇÃO: Usando o método getCustomMobData da CustomMobAPI
            CustomMobData mobData = customMobAPI.getCustomMobData(mobId).orElse(null);
            if (mobData == null) {
                player.sendMessage(plugin.getMessage(player, "admin.chat.spawntestmob.not-found", mobId));
                return true;
            }

            // CORREÇÃO: Usando a sobrecarga de spawnCustomMob que aceita CustomMobData
            LivingEntity spawnedMob = customMobAPI.spawnCustomMob(mobData, player.getLocation()).orElse(null);
            if (spawnedMob != null) {
                player.sendMessage(plugin.getMessage(player, "admin.chat.spawntestmob.success", mobData.getDisplayName()));
            } else {
                player.sendMessage(plugin.getMessage(player, "admin.chat.spawntestmob.spawn-failed", mobData.getDisplayName()));
            }
        } else {
            player.sendMessage(plugin.getMessage(player, "admin.chat.spawntestmob.module-not-found"));
        }
        return true;
    }
}