package me.ray.aethelgardRPG;

import me.ray.aethelgardRPG.api.AethelgardAPI;
import me.ray.aethelgardRPG.core.commands.MainCommandExecutor;
import me.ray.aethelgardRPG.core.commands.player.LangCommandExecutor;
import me.ray.aethelgardRPG.core.commands.player.tabcompleters.LangCommandTabCompleter;
import me.ray.aethelgardRPG.core.commands.utils.ChatInputHandler;
import me.ray.aethelgardRPG.core.commands.tabcompleters.MainCommandTabCompleter;
import me.ray.aethelgardRPG.core.configs.ConfigManager;
import me.ray.aethelgardRPG.core.configs.LanguageManager;
import me.ray.aethelgardRPG.core.database.DatabaseManager;
import me.ray.aethelgardRPG.core.modules.ModuleManager;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.core.performance.PerformanceMonitor;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.core.utils.WorldEffectManager;
import me.ray.aethelgardRPG.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.ray.aethelgardRPG.modules.admin.AdminModule;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.character.api.CharacterAPI;
import me.ray.aethelgardRPG.modules.classcombat.ClassCombatModule;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.api.ItemAPI;
import me.ray.aethelgardRPG.modules.spell.SpellModule;
import me.ray.aethelgardRPG.modules.spell.api.SpellAPI;
import me.ray.aethelgardRPG.core.utils.TargetFinder;
import me.ray.aethelgardRPG.modules.skill.SkillModule;
import me.ray.aethelgardRPG.modules.skill.api.SkillAPI;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.api.CustomMobAPI;
import me.ray.aethelgardRPG.modules.quest.QuestModule;
import me.ray.aethelgardRPG.modules.market.MarketModule;
import me.ray.aethelgardRPG.modules.bank.BankModule;
import me.ray.aethelgardRPG.modules.dungeon.DungeonModule;
import me.ray.aethelgardRPG.modules.area.AreaModule;
import me.ray.aethelgardRPG.modules.preferences.PreferencesModule;
import me.ray.aethelgardRPG.modules.preferences.PlayerPreferencesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import me.ray.aethelgardRPG.core.guis.ProtectedGUIManager;

public final class AethelgardRPG extends JavaPlugin implements AethelgardAPI {

    private static AethelgardRPG instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private ModuleManager moduleManager;
    private PerformanceMonitor performanceMonitor;
    private CommandMap commandMap;
    private TargetFinder targetFinder;
    private PlayerPreferencesManager playerPreferencesManager;
    private SessionManager sessionManager;

    @Override
    public void onLoad() {
        instance = this;
        PDCKeys.initialize(this);
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        languageManager = new LanguageManager(this);
        languageManager.loadLanguages();
    }

    @Override
    public void onEnable() {
        getLogger().info("Enabling AethelgardRPG v" + getDescription().getVersion());

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.isConnected() && configManager.getDatabaseConfig().getBoolean("enabled", false)) {
            getLogger().severe("Failed to connect to the database. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        performanceMonitor = new PerformanceMonitor(this);
        sessionManager = new SessionManager(this);
        moduleManager = new ModuleManager(this);
        targetFinder = new TargetFinder(this);

        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            getLogger().log(Level.SEVERE, "Failed to get CommandMap", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Registrar todos os módulos
        moduleManager.registerModule(new AdminModule(this));
        moduleManager.registerModule(new CharacterModule(this));
        moduleManager.registerModule(new ClassCombatModule(this));
        moduleManager.registerModule(new ItemModule(this));
        moduleManager.registerModule(new SpellModule(this));
        moduleManager.registerModule(new SkillModule(this));
        moduleManager.registerModule(new CustomMobModule(this));
        moduleManager.registerModule(new QuestModule(this));
        moduleManager.registerModule(new MarketModule(this));
        moduleManager.registerModule(new BankModule(this));
        moduleManager.registerModule(new DungeonModule(this));
        moduleManager.registerModule(new AreaModule(this));
        moduleManager.registerModule(new PreferencesModule(this));

        // Carregar os módulos (onLoad)
        moduleManager.loadModules();
        WorldEffectManager.initialize(this);
        // Registrar comandos principais
        registerCommand("rpg", new MainCommandExecutor(this), new MainCommandTabCompleter());
        registerCommand("lang", new LangCommandExecutor(this), new LangCommandTabCompleter(this));

        // Registrar comandos dos módulos através do ModuleManager
        moduleManager.registerModuleCommands();

        // Registrar handlers e listeners globais
        new ChatInputHandler(this);
        getServer().getPluginManager().registerEvents(new ProtectedGUIManager(), this);

        // Habilitar os módulos (onEnable)
        moduleManager.enableModules();

        // Recarregar sessões para jogadores que possam estar online durante um /reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            sessionManager.createSession(player);
            CharacterModule charModule = moduleManager.getModule(CharacterModule.class);
            if (charModule != null) {
                charModule.openCharacterSelectionFor(player);
            }
        }

        getLogger().info("AethelgardRPG has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling AethelgardRPG...");
        if (moduleManager != null) {
            moduleManager.disableModules();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("AethelgardRPG has been disabled.");
    }

    public static AethelgardRPG getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public PlayerPreferencesManager getPlayerPreferencesManager() {
        PreferencesModule module = moduleManager.getModule(PreferencesModule.class);
        if (module != null) {
            return module.getPreferencesManager();
        }
        getLogger().severe("PlayerPreferencesManager requested but PreferencesModule is not loaded!");
        return null;
    }

    public String getMessage(String key, Object... args) {
        if (languageManager == null) {
            getLogger().warning("LanguageManager is null when trying to get message for key: " + key);
            return ChatColor.RED + "Missing message: " + key;
        }
        return languageManager.getServerMessage(key, args);
    }

    public String getMessage(Player player, String key, Object... args) {
        if (player == null) {
            return getMessage(key, args);
        }
        if (languageManager == null) {
            getLogger().warning("LanguageManager is null when trying to get player message for key: " + key + " for player " + player.getName());
            return ChatColor.RED + "Missing message: " + key;
        }
        return languageManager.getMessage(player, key, args);
    }

    public List<String> getMessages(String key, Object... args) {
        if (languageManager == null) {
            return List.of(ChatColor.RED + "Missing list: " + key);
        }
        return languageManager.getServerMessages(key, args);
    }

    public List<String> getMessages(Player player, String key, Object... args) {
        if (player == null) {
            return getMessages(key, args);
        }
        if (languageManager == null) {
            return List.of(ChatColor.RED + "Missing list: " + key);
        }
        return languageManager.getMessages(player, key, args);
    }

    /**
     * Envia uma mensagem traduzida para um CommandSender, tratando automaticamente
     * se é um Player (usando seu idioma) ou o Console (usando o idioma padrão).
     * @param sender O destinatário da mensagem.
     * @param key A chave da mensagem.
     * @param args Os argumentos para formatar a mensagem.
     */
    public void sendMessage(CommandSender sender, String key, Object... args) {
        if (sender instanceof Player) {
            sender.sendMessage(getMessage((Player) sender, key, args));
        } else {
            sender.sendMessage(getMessage(key, args));
        }
    }
    public boolean registerCommand(String commandName, CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            PluginCommand command = constructor.newInstance(commandName, this);
            command.setExecutor(executor);
            if (completer != null) {
                command.setTabCompleter(completer);
            }
            commandMap.register(this.getDescription().getName(), command);
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register command '" + commandName + "'", e);
            return false;
        }
    }

    public void extractResourcesFromJar(String resourcePath) {
        resourcePath = resourcePath.endsWith("/") ? resourcePath : resourcePath + "/";
        CodeSource src = getClass().getProtectionDomain().getCodeSource();
        if (src != null) {
            try (ZipInputStream zip = new ZipInputStream(src.getLocation().openStream())) {
                ZipEntry e;
                while ((e = zip.getNextEntry()) != null) {
                    String name = e.getName();
                    if (name.startsWith(resourcePath) && !e.isDirectory()) {
                        File targetFile = new File(getDataFolder(), name);
                        if (!targetFile.exists()) {
                            saveResource(name, false);
                        }
                    }
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Falha ao extrair recursos do JAR do caminho: " + resourcePath, e);
            }
        } else {
            getLogger().severe("Não foi possível obter o CodeSource para extrair recursos.");
        }
    }

    public boolean registerCommand(String commandName, CommandExecutor executor) {
        return registerCommand(commandName, executor, null);
    }

    @Override
    public SkillAPI getSkillAPI() {
        return moduleManager.getModule(SkillModule.class);
    }

    @Override
    public ItemAPI getItemAPI() {
        return moduleManager.getModule(ItemModule.class);
    }

    @Override
    public CustomMobAPI getCustomMobAPI() {
        return moduleManager.getModule(CustomMobModule.class);
    }

    @Override
    public SpellAPI getSpellAPI() {
        return moduleManager.getModule(SpellModule.class);
    }

    @Override
    public CharacterAPI getCharacterAPI() {
        return moduleManager.getModule(CharacterModule.class);
    }

    @Override
    public TargetFinder getTargetFinder() {
        return targetFinder;
    }

    @Override
    public Optional<CharacterData> getCharacterData(Player player) {
        if (sessionManager == null) {
            return Optional.empty();
        }
        return sessionManager.getActiveCharacter(player);
    }
    public <T extends RPGModule> T getModuleByName(String name, Class<T> moduleClass) {
        RPGModule module = moduleManager.getModule(name);
        if (moduleClass.isInstance(module)) {
            return moduleClass.cast(module);
        }
        return null;
    }
}