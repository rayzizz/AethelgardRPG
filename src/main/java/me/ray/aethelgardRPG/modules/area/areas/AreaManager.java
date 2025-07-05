package me.ray.aethelgardRPG.modules.area.areas;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.area.AreaModule;
import me.ray.aethelgardRPG.modules.area.RPGRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AreaManager {

    private final AethelgardRPG plugin;
    private final AreaModule areaModule;
    private final List<RPGRegion> definedRegions; // Lista ordenada por prioridade pode ser útil
    private final Map<UUID, RPGRegion> playerCurrentRegion; // Cache da região atual do jogador

    public AreaManager(AethelgardRPG plugin, AreaModule areaModule) {
        this.plugin = plugin;
        this.areaModule = areaModule;
        this.definedRegions = new ArrayList<>();
        this.playerCurrentRegion = new ConcurrentHashMap<>();
    }

    public void loadAreaConfigurations() {
        // TODO: Carregar definições de áreas de arquivos YAML (ex: areas/safezone.yml, areas/pvp_arena.yml)
        // Cada arquivo definiria um RPGRegion.
        // As regiões devem ser carregadas e talvez ordenadas por prioridade.
        // Exemplo:
        // RPGRegion safeZone = new RPGRegion("safe_spawn", "Spawn Seguro", Bukkit.getWorld("world"),
        //                                   0,0,0, 100,255,100, 10, false, null, true, null,
        //                                   "&aVocê entrou no Spawn Seguro.", "&cVocê saiu do Spawn Seguro.");
        // addRegion(safeZone);

        definedRegions.sort(Comparator.comparingInt(RPGRegion::getPriority).reversed()); // Maior prioridade primeiro
        plugin.getLogger().info(definedRegions.size() + " regiões carregadas e ordenadas.");
    }

    public void addRegion(RPGRegion region) {
        definedRegions.removeIf(r -> r.getId().equals(region.getId())); // Remove se já existir com mesmo ID
        definedRegions.add(region);
        definedRegions.sort(Comparator.comparingInt(RPGRegion::getPriority).reversed());
        plugin.getLogger().info("Região adicionada/atualizada: " + region.getDisplayName());
    }

    public Optional<RPGRegion> getRegionAt(Location location) {
        if (location == null) return Optional.empty();
        // Itera sobre as regiões (já ordenadas por prioridade) e retorna a primeira que contém a localização.
        for (RPGRegion region : definedRegions) {
            if (region.contains(location)) {
                return Optional.of(region);
            }
        }
        return Optional.empty(); // Nenhuma região encontrada (ou é uma área "global" sem regras específicas)
    }

    public Optional<RPGRegion> getPlayerCurrentRegion(Player player) {
        return Optional.ofNullable(playerCurrentRegion.get(player.getUniqueId()));
    }

    public void updatePlayerRegion(Player player, RPGRegion newRegion) { // newRegion pode ser null
        RPGRegion oldRegion = playerCurrentRegion.get(player.getUniqueId());

        if (!Objects.equals(oldRegion, newRegion)) {
            if (oldRegion != null && oldRegion.getExitMessage() != null && !oldRegion.getExitMessage().isEmpty()) {
                // CORREÇÃO: Adicionado 'player' para obter a mensagem no idioma correto.
                player.sendMessage(plugin.getMessage(player, "area.custom-message", oldRegion.getExitMessage()));
            }
            if (newRegion != null) {
                playerCurrentRegion.put(player.getUniqueId(), newRegion);
                if (newRegion.getEntryMessage() != null && !newRegion.getEntryMessage().isEmpty()) {
                    // CORREÇÃO: Adicionado 'player' para obter a mensagem no idioma correto.
                    player.sendMessage(plugin.getMessage(player, "area.custom-message", newRegion.getEntryMessage()));
                }
            } else {
                playerCurrentRegion.remove(player.getUniqueId());
            }
            // TODO: Aplicar/remover regras da nova/antiga região (PvP, etc.)
        }
    }

    public void removePlayerFromCache(Player player) {
        playerCurrentRegion.remove(player.getUniqueId());
    }

    public List<RPGRegion> getAllRegions() {
        return new ArrayList<>(definedRegions);
    }
}