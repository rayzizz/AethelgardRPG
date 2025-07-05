package me.ray.aethelgardRPG.modules.market.market;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.market.MarketListing;
import me.ray.aethelgardRPG.modules.market.MarketModule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MarketManager {

    private final AethelgardRPG plugin;
    private final MarketModule marketModule;
    private final ConcurrentHashMap<UUID, MarketListing> activeListings; // ListingID -> MarketListing

    private static final double DEFAULT_LISTING_FEE_PERCENT = 0.05; // 5%
    private static final long DEFAULT_LISTING_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 dias

    public MarketManager(AethelgardRPG plugin, MarketModule marketModule) {
        this.plugin = plugin;
        this.marketModule = marketModule;
        this.activeListings = new ConcurrentHashMap<>();
    }

    public void loadMarketListings() {
        // TODO: Implementar carregamento de listagens do banco de dados
        plugin.getLogger().info("Carregando listagens do mercado do banco de dados...");
        // Exemplo:
        // List<MarketListing> listingsFromDb = database.getAllListings();
        // listingsFromDb.forEach(listing -> activeListings.put(listing.getListingId(), listing));
        plugin.getLogger().info(activeListings.size() + " listagens ativas carregadas.");
    }

    public void saveMarketListings() {
        // TODO: Implementar salvamento de listagens no banco de dados
        plugin.getLogger().info("Salvando listagens do mercado no banco de dados...");
        // activeListings.values().forEach(database::saveOrUpdateListing);
        plugin.getLogger().info(activeListings.size() + " listagens salvas.");
    }

    public boolean listItem(Player seller, ItemStack itemToSell, double price) {
        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (characterModule == null) return false;
        Optional<CharacterData> sellerDataOpt = characterModule.getCharacterData(seller);
        if (sellerDataOpt.isEmpty()) return false;
        CharacterData sellerData = sellerDataOpt.get();

        if (itemToSell == null || itemToSell.getAmount() == 0) {
            // CORREÇÃO: Adicionado 'seller'
            seller.sendMessage(plugin.getMessage(seller, "market.cannot-list-air"));
            return false;
        }
        if (price <= 0) {
            // CORREÇÃO: Adicionado 'seller'
            seller.sendMessage(plugin.getMessage(seller, "market.invalid-price"));
            return false;
        }

        // TODO: Verificar se o jogador tem o item no inventário e removê-lo
        // if (!seller.getInventory().containsAtLeast(itemToSell, itemToSell.getAmount())) {
        //     seller.sendMessage(plugin.getMessage(seller, "market.item-not-found-inventory"));
        //     return false;
        // }
        // seller.getInventory().removeItem(itemToSell.clone()); // Remove a cópia exata

        // TODO: Cobrar taxa de listagem (opcional)
        // double listingFee = price * DEFAULT_LISTING_FEE_PERCENT;
        // if (sellerData.getMoney() < listingFee) {
        //     seller.sendMessage(plugin.getMessage(seller, "market.not-enough-money-for-fee", String.format("%.2f", listingFee)));
        //     // Devolver o item ao inventário se foi removido
        //     return false;
        // }
        // sellerData.setMoney(sellerData.getMoney() - listingFee);

        MarketListing newListing = new MarketListing(seller.getUniqueId(), seller.getName(), itemToSell, price, DEFAULT_LISTING_DURATION_MS);
        activeListings.put(newListing.getListingId(), newListing);

        // TODO: Salvar a nova listagem no banco de dados imediatamente ou em batch

        // CORREÇÃO: Adicionado 'seller'
        seller.sendMessage(plugin.getMessage(seller, "market.item-listed-success", itemToSell.getType().name(), String.format("%.2f", price)));
        return true;
    }

    public boolean purchaseItem(Player buyer, UUID listingId) {
        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (characterModule == null) return false;
        Optional<CharacterData> buyerDataOpt = characterModule.getCharacterData(buyer);
        if (buyerDataOpt.isEmpty()) return false;
        CharacterData buyerData = buyerDataOpt.get();

        MarketListing listing = activeListings.get(listingId);
        if (listing == null || listing.isExpired()) {
            // CORREÇÃO: Adicionado 'buyer'
            buyer.sendMessage(plugin.getMessage(buyer, "market.listing-not-found-or-expired"));
            if (listing != null) activeListings.remove(listingId); // Remove se expirou
            return false;
        }

        if (listing.getSellerId().equals(buyer.getUniqueId())) {
            // CORREÇÃO: Adicionado 'buyer'
            buyer.sendMessage(plugin.getMessage(buyer, "market.cannot-buy-own-item"));
            return false;
        }

        if (buyerData.getMoney() < listing.getPrice()) {
            // CORREÇÃO: Adicionado 'buyer'
            buyer.sendMessage(plugin.getMessage(buyer, "market.not-enough-money-to-buy", String.format("%.2f", listing.getPrice())));
            return false;
        }

        // TODO: Dar o item ao comprador (cuidar de inventário cheio)
        // buyer.getInventory().addItem(listing.getItemStack());

        buyerData.setMoney(buyerData.getMoney() - listing.getPrice());

        // TODO: Dar o dinheiro ao vendedor (PlayerData do vendedor pode não estar carregado se offline)
        // Isso exigirá uma forma de "correio" ou adicionar ao saldo pendente do vendedor no DB.
        // PlayerData sellerData = characterModule.getCharacterData(Bukkit.getPlayer(listing.getSellerId())); (se online)
        // if (sellerData != null) sellerData.setMoney(sellerData.getMoney() + listing.getPrice());
        // else database.addPendingBalance(listing.getSellerId(), listing.getPrice());

        activeListings.remove(listingId);
        // TODO: Remover do banco de dados

        // CORREÇÃO: Adicionado 'buyer'
        buyer.sendMessage(plugin.getMessage(buyer, "market.item-purchased-success", listing.getItemStack().getType().name(), listing.getSellerName()));
        // TODO: Notificar o vendedor (se online ou via sistema de correio)
        return true;
    }

    public List<MarketListing> getAllActiveListings() {
        return activeListings.values().stream()
                .filter(listing -> !listing.isExpired())
                .collect(Collectors.toList());
    }

    // TODO: Métodos para remover listagens (cancelar pelo vendedor, admin), limpar expiradas, pesquisar/filtrar.
}