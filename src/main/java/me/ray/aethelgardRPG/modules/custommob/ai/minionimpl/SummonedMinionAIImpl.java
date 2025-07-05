package me.ray.aethelgardRPG.modules.custommob.ai.minionimpl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import me.ray.aethelgardRPG.modules.custommob.ai.MobAI;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
// Removido CustomMobManager pois não é usado diretamente aqui
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;
import java.util.UUID;
import java.util.logging.Level; // Para logging de erros

public class SummonedMinionAIImpl implements MobAI {

    protected final AethelgardRPG plugin;
    protected final CustomMobModule customMobModule;
    // Removido 'self' e 'mobData' como campos de instância, pois são passados para tickLogic
    private UUID ownerUUID;
    private LivingEntity ownerEntity;

    public static final NamespacedKey SUMMON_OWNER_KEY = new NamespacedKey(AethelgardRPG.getInstance(), "summon_owner_uuid");
    public static final NamespacedKey SUMMON_OWNER_NAME_KEY = new NamespacedKey(AethelgardRPG.getInstance(), "summon_owner_name");
    private final Random random = new Random(); // Adicionar campo Random
    public SummonedMinionAIImpl(AethelgardRPG plugin, CustomMobModule customMobModule, LivingEntity self, CustomMobData mobData) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
        // self e mobData não são mais armazenados como campos de instância aqui
        loadOwner(self); // Passa 'self' para loadOwner
    }

    private void loadOwner(LivingEntity selfForPdc) { // selfForPdc para clareza
        if (selfForPdc.getPersistentDataContainer().has(SUMMON_OWNER_KEY, PersistentDataType.STRING)) {
            String ownerUuidStr = selfForPdc.getPersistentDataContainer().get(SUMMON_OWNER_KEY, PersistentDataType.STRING);
            if (ownerUuidStr != null) {
                try {
                    this.ownerUUID = UUID.fromString(ownerUuidStr);
                    Entity foundOwner = Bukkit.getEntity(this.ownerUUID);
                    if (foundOwner instanceof LivingEntity) {
                        this.ownerEntity = (LivingEntity) foundOwner;
                    } else {
                        this.ownerEntity = null; // Garante que ownerEntity seja nulo se o foundOwner não for LivingEntity
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UUID de dono inválido para o lacaio " + selfForPdc.getUniqueId() + ": " + ownerUuidStr);
                    this.ownerEntity = null;
                }
            } else {
                this.ownerEntity = null;
            }
        } else {
            this.ownerEntity = null;
        }
    }

    /**
     * Checks if the potentialTarget is another minion belonging to the same owner as this minion.
     * @param potentialTarget The entity to check.
     * @return true if it's a friendly minion, false otherwise.
     */
    private boolean isFriendlyMinion(Entity potentialTarget) {
        if (potentialTarget == null || !(potentialTarget instanceof LivingEntity) || potentialTarget.equals(ownerEntity) || potentialTarget.getUniqueId().equals(ownerUUID)) {
            return false; // Not a living entity, or is the owner itself
        }
        LivingEntity targetLiving = (LivingEntity) potentialTarget;

        // Check if the target is a custom mob managed by our system and has an owner key
        if (!customMobModule.getCustomMobManager().isCustomMob(targetLiving)) {
            return false;
        }
        org.bukkit.persistence.PersistentDataContainer targetPdc = targetLiving.getPersistentDataContainer();
        if (!targetPdc.has(SUMMON_OWNER_KEY, PersistentDataType.STRING)) {
            return false;
        }

        String targetOwnerUuidStr = targetPdc.get(SUMMON_OWNER_KEY, PersistentDataType.STRING);
        if (targetOwnerUuidStr == null) {
            return false;
        }
        try {
            UUID targetOwnerUUID = UUID.fromString(targetOwnerUuidStr);
            return this.ownerUUID != null && this.ownerUUID.equals(targetOwnerUUID);
        } catch (IllegalArgumentException e) {
            return false; // Invalid UUID string
        }
    }

    @Override
    public void tickLogic(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        if (!(self instanceof Mob)) return;
        Mob nmsBukkitMob = (Mob) self;

        // Garante que o dono seja válido
        if (ownerEntity == null || !ownerEntity.isValid() || ownerEntity.isDead()) {
            loadOwner(self); // Tenta recarregar o dono
            if (ownerEntity == null || !ownerEntity.isValid() || ownerEntity.isDead()) {
                plugin.getLogger().info("Lacaio " + self.getUniqueId() + " ("+ mobData.getDisplayName() +") perdeu seu dono ou o dono é inválido, removendo.");
                self.remove();
                return;
            }
        }

        // 1. Valida o alvo NMS ATUAL do lacaio (pode ter sido definido por HurtByTargetGoal ou anteriormente pelo dono)
        LivingEntity currentNmsTarget = nmsBukkitMob.getTarget();
        if (currentNmsTarget != null && (isFriendlyMinion(currentNmsTarget) || currentNmsTarget.equals(ownerEntity))) {
            nmsBukkitMob.setTarget(null); // Don't target friendly minions or the owner
            currentNmsTarget = null; // Update local variable
            activeMobInfo.setInCombat(false); // No longer in combat with this friendly
        }

        // Se o alvo atual morreu ou se tornou inválido, limpa-o.
        if (currentNmsTarget != null && (!currentNmsTarget.isValid() || currentNmsTarget.isDead())) {
            nmsBukkitMob.setTarget(null);
            currentNmsTarget = null;
            activeMobInfo.setInCombat(false);
        }

        // 2. Obtém o alvo designado pelo dono (ODT)
        LivingEntity ownerDesignatedTarget = null;
        if (ownerEntity instanceof Player) {
            Player ownerPlayer = (Player) ownerEntity;
            // Tenta obter o alvo que o jogador está olhando.
            // O 'false' em getTargetEntity é para não incluir entidades não sólidas/fluidos (Paper API).
            // Se não estiver usando Paper, use player.getTargetEntity(maxDistance) que é similar.
            Entity lookedAt = null;
            try { // Envolve em try-catch caso getTargetEntity não exista (Spigot puro sem PaperLib)
                lookedAt = ownerPlayer.getTargetEntity((int) mobData.getAggroRange(), false);
            } catch (NoSuchMethodError e) { // Fallback para Spigot
                try {
                    lookedAt = ownerPlayer.getTargetEntity((int) mobData.getAggroRange());
                } catch (NoSuchMethodError e2) {
                    // Se ainda falhar, logar e não fazer nada com o alvo
                    plugin.getLogger().warning("Método getTargetEntity não encontrado. A IA de alvo do lacaio pode não funcionar como esperado.");
                }
            }

            if (lookedAt instanceof LivingEntity && !lookedAt.equals(self) && !lookedAt.equals(ownerEntity) && !isFriendlyMinion(lookedAt)) {
                ownerDesignatedTarget = (LivingEntity) lookedAt; // Only designate if not self, owner, or friendly minion
            }
        }
        // Adicionar 'else if (ownerEntity instanceof Mob)' se lacaios puderem ser donos de outros lacaios e tiverem alvos.

        // 3. Lógica de decisão de alvo
        if (ownerDesignatedTarget != null) {
            // Dono designou um alvo válido. Se for diferente do alvo atual do lacaio, muda.
            if (nmsBukkitMob.getTarget() == null || !nmsBukkitMob.getTarget().equals(ownerDesignatedTarget)) {
                nmsBukkitMob.setTarget(ownerDesignatedTarget);
            }
        } else {
            // Não tem alvo, não está em combate
            // A lógica de isInCombat = false é tratada pela combatStateCheckTask.
            // Lógica para seguir o dono
            followOwnerLogic(self, nmsBukkitMob);
        }
    }

    private void followOwnerLogic(LivingEntity self, Mob nmsBukkitMob) {
        if (ownerEntity == null || !ownerEntity.isValid() || ownerEntity.isDead()) return;

        if (self.getLocation().distanceSquared(ownerEntity.getLocation()) > 9) { // Segue se estiver a mais de ~3 blocos
            try {
                Object nmsEntityHandle = self.getClass().getMethod("getHandle").invoke(self);
                Object nmsOwnerHandle = ownerEntity.getClass().getMethod("getHandle").invoke(ownerEntity);
                if (nmsEntityHandle instanceof net.minecraft.world.entity.PathfinderMob && nmsOwnerHandle instanceof net.minecraft.world.entity.LivingEntity) {
                    net.minecraft.world.entity.PathfinderMob nmsPathfinder = (net.minecraft.world.entity.PathfinderMob) nmsEntityHandle;
                    net.minecraft.core.BlockPos currentNavTargetPos = nmsPathfinder.getNavigation().getTargetPos();
                    org.bukkit.Location ownerBukkitLocation = ownerEntity.getLocation();
                    net.minecraft.core.BlockPos ownerNmsBlockPos = new net.minecraft.core.BlockPos(ownerBukkitLocation.getBlockX(), ownerBukkitLocation.getBlockY(), ownerBukkitLocation.getBlockZ());

                    if (nmsPathfinder.getNavigation().isDone() || currentNavTargetPos == null || currentNavTargetPos.distSqr(ownerNmsBlockPos) > 4.0) {
                        org.bukkit.Location ownerLoc = ownerEntity.getLocation();
                        double randomAngle = random.nextDouble() * 2 * Math.PI;
                        double randomRadius = 1.0 + random.nextDouble() * 1.0;
                        double offsetX = Math.cos(randomAngle) * randomRadius;
                        double offsetZ = Math.sin(randomAngle) * randomRadius;
                        double targetX = ownerLoc.getX() + offsetX;
                        double targetY = ownerLoc.getY();
                        double targetZ = ownerLoc.getZ() + offsetZ;
                        nmsPathfinder.getNavigation().moveTo(targetX, targetY, targetZ, 1.0D);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erro na lógica de seguir o dono do lacaio (followOwnerLogic)", e);
            }
        } else { // Se estiver perto do dono e sem alvo, para de se mover para evitar jitter
            try {
                Object nmsEntityHandle = self.getClass().getMethod("getHandle").invoke(self);
                if (nmsEntityHandle instanceof net.minecraft.world.entity.PathfinderMob) {
                    net.minecraft.world.entity.PathfinderMob nmsPathfinder = (net.minecraft.world.entity.PathfinderMob) nmsEntityHandle;
                    if (!nmsPathfinder.getNavigation().isDone()) {
                        nmsPathfinder.getNavigation().stop();
                    }
                }
            } catch (Exception e) {
                // Log silencioso ou de debug
            }
        }
    }

    @Override
    public void onTargetAcquired(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        // This method is called when NMS goals (like HurtByTargetGoal) set a target.
        // We need to ensure it doesn't target the owner or other friendly minions.
        if (target == null) return;

        if (target.equals(ownerEntity) || isFriendlyMinion(target)) {
            if (self instanceof Mob) {
                ((Mob) self).setTarget(null); // Clear the target if it's owner or friendly
            }
            plugin.getLogger().fine("Minion " + self.getName() + " attempted to target friendly: " + target.getName() + ". Target cleared.");
            return;
        }

        // If the target is valid (not owner, not friendly minion)
        // Não define isInCombat aqui para o display.
        // A IA considera em combate.
        activeMobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
        if (self instanceof Mob) { // Usar Mob em vez de Creature para consistência
            ((Mob) self).setTarget(target);
        }
        plugin.getLogger().fine("Minion " + self.getName() + " acquired target: " + target.getName());
    }


    @Override
    public void onDamaged(LivingEntity self, LivingEntity attacker, double damage, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        // Não define isInCombat aqui para o display.
        // A IA considera em combate.
        activeMobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
        // Se o atacante for um jogador e não for o dono, o HurtByTargetGoal (NMS) deve chamar onTargetAcquired.
        // Não precisamos definir o alvo aqui explicitamente se o HurtByTargetGoal estiver funcionando.
        // No entanto, se o atacante for outro mob (não-jogador), o HurtByTargetGoal ainda deve funcionar.
        // A lógica em onTargetAcquired já previne mirar em lacaios amigos.
    }

    @Override
    public void onTargetLost(LivingEntity self, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        // A lógica de isInCombat = false é tratada pela combatStateCheckTask.
        if (self instanceof Mob) {
            ((Mob) self).setTarget(null);
        }
    }

    @Override
    public boolean shouldAttemptAbilityUsage(LivingEntity self, LivingEntity target, CustomMobData mobData, ActiveMobInfo activeMobInfo) {
        // Lacaios podem usar habilidades se estiverem em combate, tiverem um alvo válido e este não for o dono ou outro lacaio amigo.
        return activeMobInfo.isInCombat() && target != null && target.isValid() && !target.isDead() &&
                (ownerEntity == null || !target.getUniqueId().equals(ownerEntity.getUniqueId())) && !isFriendlyMinion(target);
    }

    @Override
    public AIType getType() {
        return AIType.SUMMONED_MINION;
    }
}