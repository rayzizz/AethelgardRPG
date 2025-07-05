package me.ray.aethelgardRPG.modules.custommob.api;

import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager; // Import adicionado
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public interface CustomMobAPI {

    Optional<CustomMobData> getCustomMobData(String mobId);

    // Sobrecarga para spawnar a partir de um objeto de dados, essencial para lacaios dinâmicos.
    Optional<LivingEntity> spawnCustomMob(CustomMobData mobData, Location location);

    Optional<LivingEntity> spawnCustomMob(String mobId, Location location);

    Optional<String> getMobDisplayName(String mobId, Player playerContext);

    void setCustomMobCurrentHealth(UUID mobUUID, double health);

    double getCustomMobCurrentHealth(UUID mobUUID);

    // Expõe o manager para corrigir chamadas de outros módulos.
    CustomMobManager getCustomMobManager();
}