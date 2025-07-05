package me.ray.aethelgardRPG.modules.bank;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.bank.bank.BankManager;
import me.ray.aethelgardRPG.modules.bank.BankRepository;
import me.ray.aethelgardRPG.modules.bank.commands.BankCommand;
import me.ray.aethelgardRPG.modules.bank.listeners.BankGUIListener;
import me.ray.aethelgardRPG.modules.bank.listeners.BankPlayerListener;

public class BankModule implements RPGModule {

    private final AethelgardRPG plugin;
    private BankManager bankManager;
    private me.ray.aethelgardRPG.modules.bank.BankRepository bankRepository;

    public BankModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Bank";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Bank...");
        this.bankRepository = new BankRepository(plugin);
        this.bankManager = new BankManager(plugin, this, bankRepository);

        // Registra listeners
        plugin.getServer().getPluginManager().registerEvents(new BankGUIListener(bankManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BankPlayerListener(bankManager), plugin);
    }

    @Override
    public void onEnable() {
        // A criação da tabela é feita aqui para garantir que a conexão com o DB esteja ativa.
        if (plugin.getDatabaseManager().isConnected()) {
            bankRepository.createBankTable();
        }
        plugin.getLogger().info("Módulo Bank habilitado.");
        bankManager.loadOnlinePlayerBankAccounts();
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo Bank desabilitado.");
        bankManager.saveAllCachedBankAccounts();
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    @Override
    public void registerCommands(AethelgardRPG mainPlugin) {
        mainPlugin.registerCommand("bank", new BankCommand(mainPlugin, this));
    }
}