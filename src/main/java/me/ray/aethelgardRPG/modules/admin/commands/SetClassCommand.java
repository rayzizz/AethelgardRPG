package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.api.AethelgardAPI;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SetClassCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final AethelgardAPI api;

    public SetClassCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
        this.api = plugin; // AethelgardRPG implements AethelgardAPI
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Determine the player context for messages once.
        Player senderPlayer = (sender instanceof Player) ? (Player) sender : null;

        if (!sender.hasPermission("aethelgardrpg.admin.chat.setclass")) {
            sender.sendMessage(plugin.getMessage(senderPlayer, "general.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage(senderPlayer, "admin.chat.setclass.usage"));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.getMessage(senderPlayer, "general.player-not-found", args[0]));
            return true;
        }

        RPGClass newClass;
        try {
            newClass = RPGClass.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessage(senderPlayer, "admin.chat.setclass.class-not-found", args[1]));
            return true;
        }

        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (characterModule == null) {
            sender.sendMessage(plugin.getMessage(senderPlayer, "general.module-not-found", "Character"));
            return true;
        }

        Optional<CharacterData> characterDataOpt = api.getCharacterData(targetPlayer);
        if (characterDataOpt.isEmpty()) {
            sender.sendMessage(plugin.getMessage(senderPlayer, "admin.chat.setclass.player-no-data", targetPlayer.getName()));
            return true;
        }

        CharacterData characterData = characterDataOpt.get();

        characterData.setSelectedClass(newClass); // This re-initializes PlayerAttributes with the new class's base stats
        characterModule.updatePlayerAttributesFromEquipment(targetPlayer); // Recalculate item bonuses
        if (characterModule.getStatusDisplayManager() != null) {
            characterModule.getStatusDisplayManager().clearPlayerFromCache(targetPlayer.getUniqueId()); // Force action bar update
        }

        characterModule.saveCharacterDataAsync(characterData); // Save the changes

        sender.sendMessage(plugin.getMessage(senderPlayer, "admin.chat.setclass.success", targetPlayer.getName(), newClass.getDisplayName(senderPlayer)));

        if (sender != targetPlayer) {
            targetPlayer.sendMessage(plugin.getMessage(targetPlayer, "admin.chat.setclass.changed-by-other", newClass.getDisplayName(targetPlayer)));
        }
        return true;
    }
}