package me.ray.aethelgardRPG.core.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule; // Import CharacterModule
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MainCommandExecutor implements CommandExecutor {

    private final AethelgardRPG plugin;

    public MainCommandExecutor(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            plugin.sendMessage(sender, "general.plugin-info", plugin.getDescription().getName(), plugin.getDescription().getVersion());
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Handle core commands directly or delegate to a sub-command handler
        switch (subCommand) {
            case "reload":
                if (sender.hasPermission("aethelgardrpg.admin.reload")) {
                    plugin.getConfigManager().reloadAllConfigs(); // Changed to reloadAllConfigs
                    plugin.getLanguageManager().reloadLanguages();

                    // Recarregar módulos
                    plugin.sendMessage(sender, "general.reloading-modules");
                    plugin.getModuleManager().disableModules();
                    // Recarregar configurações específicas dos módulos aqui, se necessário, antes de habilitá-los
                    plugin.getModuleManager().loadModules(); // Recarregar para pegar mudanças de config nos módulos
                    // Recarregar configs específicas do CharacterModule (leveling, action bar, regen)
                    CharacterModule charModule = plugin.getModuleManager().getModule(CharacterModule.class);
                    if (charModule != null) charModule.reloadCharacterConfigs();
                    plugin.getModuleManager().enableModules();

                    plugin.sendMessage(sender, "general.reload-success");
                } else {
                    plugin.sendMessage(sender, "general.no-permission");
                }
                return true;
            // Add more core subcommands here (e.g., version, help)
            default:
                plugin.sendMessage(sender, "general.unknown-command");
                return true;
        }
    }
}