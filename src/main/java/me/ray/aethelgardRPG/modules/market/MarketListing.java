package me.ray.aethelgardRPG.modules.market;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MarketListing {
    private final UUID listingId; // ID único para esta listagem
    private final UUID sellerId;
    private final String sellerName; // Para exibição
    private final ItemStack itemStack; // Uma cópia do item
    private final double price;
    private final long listingDateTimestamp;
    private final long expiryDateTimestamp; // Opcional, para listagens que expiram

    public MarketListing(UUID sellerId, String sellerName, ItemStack itemStack, double price, long listingDurationMillis) {
        this.listingId = UUID.randomUUID();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemStack = itemStack.clone(); // Armazena uma cópia para evitar modificações externas
        this.price = price;
        this.listingDateTimestamp = System.currentTimeMillis();
        this.expiryDateTimestamp = listingDurationMillis > 0 ? this.listingDateTimestamp + listingDurationMillis : -1; // -1 para nunca expirar
    }

    // Construtor para carregar do banco de dados
    public MarketListing(UUID listingId, UUID sellerId, String sellerName, ItemStack itemStack, double price, long listingDateTimestamp, long expiryDateTimestamp) {
        this.listingId = listingId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemStack = itemStack; // Assume que já é uma cópia segura ao carregar
        this.price = price;
        this.listingDateTimestamp = listingDateTimestamp;
        this.expiryDateTimestamp = expiryDateTimestamp;
    }

    public UUID getListingId() {
        return listingId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItemStack() {
        return itemStack.clone(); // Retorna uma cópia para segurança
    }

    public double getPrice() {
        return price;
    }

    public long getListingDateTimestamp() { return listingDateTimestamp; }

    public long getExpiryDateTimestamp() { return expiryDateTimestamp; }

    public boolean isExpired() {
        return expiryDateTimestamp != -1 && System.currentTimeMillis() > expiryDateTimestamp;
    }
}