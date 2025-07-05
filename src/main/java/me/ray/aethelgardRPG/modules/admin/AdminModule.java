package me.ray.aethelgardRPG.modules.admin;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.core.commands.utils.tabcompleters.PlayerNameTabCompleter; // Import generic
import me.ray.aethelgardRPG.modules.admin.commands.tabcompleters.GamemodeTabCompleter; // Import specific
import me.ray.aethelgardRPG.modules.admin.commands.tabcompleters.GetTestWeaponTabCompleter; // Import specific
import me.ray.aethelgardRPG.modules.admin.commands.tabcompleters.RPGAdminTabCompleter; // Import specific
import me.ray.aethelgardRPG.modules.admin.commands.tabcompleters.SetClassTabCompleter;
import me.ray.aethelgardRPG.modules.admin.commands.tabcompleters.SpawnTestMobTabCompleter; // Import new
import me.ray.aethelgardRPG.modules.admin.commands.tabcompleters.GetAccessoryTabCompleter; // Import new tab completer
import me.ray.aethelgardRPG.modules.admin.commands.tabcompleters.GetArmorTabCompleter; // Import new armor tab completer
// Import new command
// Import new armor command
import me.ray.aethelgardRPG.modules.admin.commands.*;
import me.ray.aethelgardRPG.modules.admin.listeners.InfiniteStatsListener;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class AdminModule implements RPGModule {

    private final AethelgardRPG plugin;
    private final Set<UUID> vanishedPlayers; // Simples gerenciamento de vanish em memória
    private final Set<UUID> infiniteStatsPlayers; // NOVO: Gerenciamento de jogadores com stats infinitos

    public AdminModule(AethelgardRPG plugin) {
        this.plugin = plugin;
        this.vanishedPlayers = new HashSet<>();
        this.infiniteStatsPlayers = new HashSet<>(); // Inicializa o novo Set
    }

    @Override
    public String getName() {
        return "Admin"; // Nome do módulo, usado na config, por exemplo
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Admin...");
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo Admin habilitado.");
        // Registra o listener para a funcionalidade de stats infinitos
        plugin.getServer().getPluginManager().registerEvents(new InfiniteStatsListener(plugin, this), plugin);
    }

    @Override
    public void onDisable() {
        // Lógica para quando o módulo é desabilitado
        // Ex: Salvar dados de jogadores em vanish, limpar listas
        vanishedPlayers.clear();
        infiniteStatsPlayers.clear(); // Limpa também a lista de stats infinitos
        plugin.getLogger().info("Módulo Admin desabilitado.");
    }

    @Override
    public void registerCommands(AethelgardRPG mainPlugin) {
        mainPlugin.registerCommand("ban", new BanCommand(mainPlugin), new PlayerNameTabCompleter());
        mainPlugin.registerCommand("kick", new KickCommand(mainPlugin), new PlayerNameTabCompleter());
        mainPlugin.registerCommand("mute", new MuteCommand(mainPlugin, this), new PlayerNameTabCompleter()); // Mute também pode usar PlayerNameTabCompleter para o primeiro arg
        mainPlugin.registerCommand("vanish", new VanishCommand(mainPlugin, this)); // Sem args, sem tab completer específico
        mainPlugin.registerCommand("gamemode", new GamemodeCommand(mainPlugin), new GamemodeTabCompleter());
        mainPlugin.registerCommand("rpgadmin", new RPGAdminCommand(mainPlugin), new RPGAdminTabCompleter());
        mainPlugin.registerCommand("spawntestmob", new SpawnTestMobCommand(mainPlugin), new SpawnTestMobTabCompleter(mainPlugin)); // Added TabCompleter
        mainPlugin.registerCommand("gettestweapon", new GetTestWeaponCommand(mainPlugin), new GetTestWeaponTabCompleter());
        mainPlugin.registerCommand("getaccessory", new GetAccessoryCommand(mainPlugin), new GetAccessoryTabCompleter(mainPlugin)); // Register new accessory command
        mainPlugin.registerCommand("getarmor", new GetArmorCommand(mainPlugin), new GetArmorTabCompleter(mainPlugin)); // Register new armor command
        mainPlugin.registerCommand("setclass", new SetClassCommand(mainPlugin), new SetClassTabCompleter()); // Register SetClass command
        mainPlugin.registerCommand("infinitestats", new InfiniteStatsCommand(mainPlugin, this), new PlayerNameTabCompleter()); // NOVO COMANDO
    }

    public Set<UUID> getVanishedPlayers() {
        return vanishedPlayers;
    }

    public boolean isVanished(UUID playerId) {
        return vanishedPlayers.contains(playerId);
    }

    public void setVanished(UUID playerId, boolean vanish) {
        if (vanish) {
            vanishedPlayers.add(playerId);
        } else {
            vanishedPlayers.remove(playerId);
        }
    }

    // NOVOS MÉTODOS PARA STATS INFINITOS
    public boolean isInfiniteStatsEnabled(UUID playerId) {
        return infiniteStatsPlayers.contains(playerId);
    }

    public boolean toggleInfiniteStats(UUID playerId) {
        if (infiniteStatsPlayers.contains(playerId)) {
            infiniteStatsPlayers.remove(playerId);
            return false; // Desabilitado
        } else {
            infiniteStatsPlayers.add(playerId);
            return true; // Habilitado
        }
    }
}