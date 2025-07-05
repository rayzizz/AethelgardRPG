package me.ray.aethelgardRPG.modules.market;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.market.commands.MarketCommand;
import me.ray.aethelgardRPG.modules.market.market.MarketManager;

import java.util.Objects;

public class MarketModule implements RPGModule {

    private final AethelgardRPG plugin;
    private MarketManager marketManager;

    public MarketModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Market";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Market...");
        this.marketManager = new MarketManager(plugin, this);
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo Market habilitado.");
        marketManager.loadMarketListings(); // Carregar listagens do banco de dados, se persistido
        // Command registration is now handled by the main plugin class
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo Market desabilitado.");
        marketManager.saveMarketListings(); // Salvar listagens no banco de dados, se persistido
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public void registerCommands(AethelgardRPG mainPlugin) {
        mainPlugin.registerCommand("market", new MarketCommand(mainPlugin, this));
    }
}