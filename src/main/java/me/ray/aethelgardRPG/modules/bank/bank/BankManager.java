package me.ray.aethelgardRPG.modules.bank.bank;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.bank.BankModule;
import me.ray.aethelgardRPG.modules.bank.BankRepository;
import me.ray.aethelgardRPG.modules.bank.PlayerBankAccount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankManager {

    private final AethelgardRPG plugin;
    private final BankModule bankModule;
    private final BankRepository bankRepository;
    private final ConcurrentHashMap<UUID, PlayerBankAccount> playerBankAccounts; // Player UUID -> BankAccount

    public BankManager(AethelgardRPG plugin, BankModule bankModule, BankRepository bankRepository) {
        this.plugin = plugin;
        this.bankModule = bankModule;
        this.bankRepository = bankRepository;
        this.playerBankAccounts = new ConcurrentHashMap<>();
    }

    /**
     * Carrega as contas bancárias de todos os jogadores atualmente online.
     * Chamado na habilitação do módulo.
     */
    public void loadOnlinePlayerBankAccounts() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        plugin.getLogger().info("Carregando contas bancárias para jogadores online...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerBankAccount(player.getUniqueId());
        }
        plugin.getLogger().info(playerBankAccounts.size() + " contas bancárias carregadas para jogadores online.");
    }

    /**
     * Salva todas as contas bancárias atualmente em cache no banco de dados.
     * Chamado na desabilitação do módulo.
     */
    public void saveAllCachedBankAccounts() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        plugin.getLogger().info("Salvando " + playerBankAccounts.size() + " contas bancárias em cache...");
        playerBankAccounts.values().forEach(bankRepository::saveAccount);
        plugin.getLogger().info("Contas bancárias salvas.");
    }

    /**
     * Obtém a conta bancária de um jogador. Se não estiver em cache, tenta carregar do DB.
     * Se não existir no DB, cria uma nova.
     * @param player O jogador.
     * @return A conta bancária do jogador.
     */
    public PlayerBankAccount getPlayerBankAccount(Player player) {
        return getPlayerBankAccount(player.getUniqueId());
    }

    /**
     * Obtém a conta bancária de um jogador pelo UUID.
     * @param playerUUID O UUID do jogador.
     * @return A conta bancária do jogador.
     */
    public PlayerBankAccount getPlayerBankAccount(UUID playerUUID) {
        return playerBankAccounts.computeIfAbsent(playerUUID, this::loadPlayerBankAccount);
    }

    /**
     * Carrega a conta de um jogador do banco de dados para o cache.
     * @param playerUUID O UUID do jogador.
     * @return A conta carregada ou uma nova se não existir.
     */
    public PlayerBankAccount loadPlayerBankAccount(UUID playerUUID) {
        if (!plugin.getDatabaseManager().isConnected()) {
            plugin.getLogger().warning("Banco de dados desconectado. Criando conta bancária temporária para " + playerUUID);
            return new PlayerBankAccount(playerUUID);
        }
        // Tenta carregar do repositório, se não encontrar, cria uma nova.
        return bankRepository.loadAccount(playerUUID)
                .orElseGet(() -> {
                    plugin.getLogger().info("Nenhuma conta bancária encontrada para " + playerUUID + ". Criando uma nova.");
                    return new PlayerBankAccount(playerUUID);
                });
    }

    /**
     * Salva a conta de um jogador do cache para o banco de dados.
     * @param playerUUID O UUID do jogador.
     */
    public void savePlayerBankAccount(UUID playerUUID) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        PlayerBankAccount account = playerBankAccounts.get(playerUUID);
        if (account != null) {
            bankRepository.saveAccount(account);
            plugin.getLogger().fine("Conta bancária de " + playerUUID + " salva.");
        }
    }

    /**
     * Salva a conta de um jogador de forma assíncrona e a remove do cache.
     * Ideal para ser usado quando o jogador sai do servidor.
     * @param playerUUID O UUID do jogador.
     */
    public void saveAndUnloadPlayerBankAccount(UUID playerUUID) {
        PlayerBankAccount account = playerBankAccounts.remove(playerUUID);
        if (account != null && plugin.getDatabaseManager().isConnected()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                bankRepository.saveAccount(account);
                plugin.getLogger().info("Conta bancária de " + playerUUID + " salva e descarregada do cache.");
            });
        }
    }

    public void openBankGUI(Player player) {
        PlayerBankAccount account = getPlayerBankAccount(player);

        // Cria uma nova GUI de visualização com o título traduzido.
        // O 'account' (PlayerBankAccount) é o holder, o que nos permite identificá-lo no InventoryCloseEvent.
        String guiTitle = plugin.getMessage(player, "bank.gui-title", player.getName());
        Inventory bankView = Bukkit.createInventory(account, account.getItemSlots(), guiTitle);

        // Copia os itens da conta real para a GUI de visualização.
        bankView.setContents(account.getItemInventory().getContents());

        player.openInventory(bankView);

        player.sendMessage(plugin.getMessage(player, "bank.gui-opened"));
        player.sendMessage(plugin.getMessage(player, "bank.current-balance", String.format("%.2f", account.getMoneyBalance())));
    }
}