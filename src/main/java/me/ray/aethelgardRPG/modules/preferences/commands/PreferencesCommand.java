package me.ray.aethelgardRPG.modules.preferences.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.preferences.guis.PreferencesGUI; // Importar a GUI
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PreferencesCommand implements CommandExecutor {

    private final AethelgardRPG plugin; // Precisamos da instância do plugin para criar a GUI

    public PreferencesCommand(AethelgardRPG plugin) {
        this.plugin = plugin; // Armazenar a instância do plugin
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Verifica se o comando foi executado por um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        // Ignora quaisquer argumentos e sempre abre a GUI
        // A lógica de alteração das preferências agora é exclusiva da GUI
        new PreferencesGUI(plugin, plugin.getPlayerPreferencesManager()).open(player);

        return true;
    }
    // Os métodos sendUsage e formatStatus foram removidos, pois não são mais necessários.
}