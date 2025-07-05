package me.ray.aethelgardRPG.modules.spell.spells.archer;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.WorldEffectManager;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.preferences.PlayerPreferences;
import me.ray.aethelgardRPG.modules.preferences.PlayerPreferencesManager;
import me.ray.aethelgardRPG.modules.spell.spells.BaseSpell;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Magia "Tiro Potente" para a classe Arqueiro.
 * Conjura um ataque que, após uma breve fase de carregamento, dispara
 * automaticamente um feixe de energia.
 */
public class ArcherPotentShotSpell extends BaseSpell {

    // --- Configuração da Magia ---
    private static final String ID = "archer_potent_shot";
    private static final int COOLDOWN_SECONDS = 12;
    private static final double MANA_COST = 35.0;
    private static final double BASE_DAMAGE = 40.0;
    private static final double CHARGE_SECONDS = 3.0;
    private static final int REQUIRED_LEVEL = 10;
    private static final double KNOCKBACK_STRENGTH = 1.2;
    private static final int CHARGE_SLOWNESS_AMPLIFIER = 1; // 0 = Slow I, 1 = Slow II
    private static final double BEAM_SPEED = 4.0;

    // --- NOVAS CONFIGURAÇÕES PARA O RASTRO DO FEIXE ---
    private static final double BEAM_TRAIL_LENGTH = 15.0; // Comprimento do rastro visível do feixe em blocos
    private static final double PARTICLE_DENSITY = 0.4; // Densidade das partículas no rastro (menor valor = mais denso)

    // --- Gerenciamento de Estado ---
    private static final Set<UUID> chargingPlayers = new HashSet<>();

    public ArcherPotentShotSpell(AethelgardRPG plugin) {
        super(plugin,
                ID,
                "spell.archer.potent_shot.name",
                "spell.archer.potent_shot.description",
                COOLDOWN_SECONDS,
                MANA_COST,
                RPGClass.ARQUEIRO,
                REQUIRED_LEVEL,
                100.0 // <<< AQUI VOCÊ DEFINE O ALCANCE DO TIRO (em blocos)
        );
    }

    @Override
    public SpellCastResult execute(SpellCastContext context) {
        Player caster = context.getCaster();

        if (chargingPlayers.contains(caster.getUniqueId())) {
            return SpellCastResult.FAIL_OTHER;
        }

        new ChargeTask(caster).runTaskTimer(plugin, 0L, 1L);

        return SpellCastResult.SUCCESS;
    }

    private void fire(Player caster) {
        chargingPlayers.remove(caster.getUniqueId());

        Location fireLocation = caster.getEyeLocation();
        // Estes sons são considerados parte do efeito central da magia, não apenas visuais.
        // Se você quiser que eles também sejam baseados em preferências, precisaria de um loop aqui.
        caster.getWorld().playSound(fireLocation, Sound.BLOCK_GLASS_BREAK, 1.0F, 1.2F);
        caster.getWorld().playSound(fireLocation, Sound.BLOCK_BEACON_POWER_SELECT, 4F, 1.5F);

        new BeamTask(caster).runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Método auxiliar para obter jogadores que estão dentro de um raio.
     * Não filtra por preferências, essas devem ser checadas no ponto de uso.
     * @param center A localização central para verificar jogadores próximos.
     * @param radius O raio para verificar.
     * @return Uma lista de jogadores dentro do alcance.
     */
    private List<Player> getPlayersInRadius(Location center, double radius) {
        List<Player> playersInRange = new ArrayList<>();
        double radiusSquared = radius * radius;
        // Itera por todos os jogadores online no mundo
        for (Player p : center.getWorld().getPlayers()) {
            // Verifica se estão dentro do raio especificado
            if (p.getLocation().distanceSquared(center) <= radiusSquared) {
                playersInRange.add(p);
            }
        }
        return playersInRange;
    }

    /**
     * Tarefa interna que controla a fase de carregamento da magia.
     */
    private class ChargeTask extends BukkitRunnable {
        private final Player caster;
        private final long chargeDurationTicks = (long) (CHARGE_SECONDS * 20);
        private long ticksElapsed = 0;
        private final Random random = new Random();

        private static final double RING_1_RADIUS = 3.3;
        private static final double RING_2_RADIUS = 2.7;
        private static final double RING_3_RADIUS = 2.2;
        private static final double RING_4_RADIUS = 1.6;
        private static final double RING_5_RADIUS = 1.0;

        private static final int TICKS_PER_RING_DRAW = 10;

        public ChargeTask(Player caster) {
            this.caster = caster;
            chargingPlayers.add(caster.getUniqueId());
            caster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) chargeDurationTicks + 20, CHARGE_SLOWNESS_AMPLIFIER, true, false));
        }

        @Override
        public void run() {
            if (!caster.isOnline() || caster.isDead()) {
                chargingPlayers.remove(caster.getUniqueId());
                caster.removePotionEffect(PotionEffectType.SLOWNESS);
                this.cancel();
                return;
            }

            if (ticksElapsed >= chargeDurationTicks) {
                caster.removePotionEffect(PotionEffectType.SLOWNESS);
                fire(caster);
                this.cancel();
                return;
            }

            double progress = (double) ticksElapsed / chargeDurationTicks;
            Location origin = caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(1.0));

            // Obtém todos os jogadores dentro do raio para processamento individual
            List<Player> nearbyPlayers = getPlayersInRadius(origin, 30.0); // Raio para efeitos visuais

            // Calcula os vetores de direção uma vez
            Vector direction = origin.getDirection().normalize();
            Vector side = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            Vector vertical = side.clone().crossProduct(direction).normalize();

            // Loop pelos jogadores próximos para enviar pacotes de partículas individuais
            PlayerPreferencesManager prefsManager = plugin.getPlayerPreferencesManager();

            for (Player p : nearbyPlayers) {
                PlayerPreferences viewerPrefs = prefsManager.getPreferences(p);
                if (viewerPrefs.canShowSpellParticles()) { // Verifica a preferência INDIVIDUAL do espectador
                    //p.spawnParticle(Particle.FIREWORK, origin, 1, 0, 0, 0, 0);

                    if (ticksElapsed >= 0) {
                        drawRingForPlayer(p, origin, side, vertical, RING_1_RADIUS, ticksElapsed, 0);
                    }
                    if (ticksElapsed >= TICKS_PER_RING_DRAW) {
                        Location ring2Center = origin.clone().add(direction.clone().multiply(1.0));
                        drawRingForPlayer(p, ring2Center, side, vertical, RING_2_RADIUS, ticksElapsed, TICKS_PER_RING_DRAW);
                    }
                    if (ticksElapsed >= 2 * TICKS_PER_RING_DRAW) {
                        Location ring3Center = origin.clone().add(direction.clone().multiply(2.0));
                        drawRingForPlayer(p, ring3Center, side, vertical, RING_3_RADIUS, ticksElapsed, 2 * TICKS_PER_RING_DRAW);
                    }
                    if (ticksElapsed >= 3 * TICKS_PER_RING_DRAW) {
                        Location ring4Center = origin.clone().add(direction.clone().multiply(3.0));
                        drawRingForPlayer(p, ring4Center, side, vertical, RING_4_RADIUS, ticksElapsed, 3 * TICKS_PER_RING_DRAW);
                    }
                    if (ticksElapsed >= 4 * TICKS_PER_RING_DRAW) {
                        Location ring5Center = origin.clone().add(direction.clone().multiply(4.0));
                        drawRingForPlayer(p, ring5Center, side, vertical, RING_5_RADIUS, ticksElapsed, 4 * TICKS_PER_RING_DRAW);
                    }
                }
            }

            // Os sons de carregamento são agora individuais, baseados na preferência de partículas.
            playChargeSounds(origin, progress, nearbyPlayers);
            ticksElapsed++;
        }

        private void drawRingForPlayer(Player player, Location center, Vector side, Vector vertical, double radius, long currentTicksElapsed, long ringStartTick) {
            double ringDrawProgress = (double)(currentTicksElapsed - ringStartTick) / TICKS_PER_RING_DRAW;
            ringDrawProgress = Math.max(0, Math.min(1.0, ringDrawProgress));

            double totalAngleToDraw = ringDrawProgress * (2 * Math.PI);
            double currentAngle = Math.PI / 2;

            Particle particleType = (ringDrawProgress >= 1.0) ? Particle.FLAME : Particle.WITCH;

            for (double angleOffset = 0; angleOffset <= totalAngleToDraw; angleOffset += Math.PI / 16) {
                double angle = currentAngle - angleOffset;
                Vector offset = side.clone().multiply(radius * Math.cos(angle))
                        .add(vertical.clone().multiply(radius * Math.sin(angle)));
                Location particleLoc = center.clone().add(offset);
                player.spawnParticle(particleType, particleLoc, 1, 0, 0, 0, 0);

                if (random.nextInt(100) < 5) {
                    player.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0.1, 0.1, 0.1, 0.1);
                }
            }

            if (ringDrawProgress >= 1.0) {
                double flowAngle = (currentTicksElapsed * (2.5 / radius)) % (2 * Math.PI);
                Vector flowOffset = side.clone().multiply(radius * Math.cos(flowAngle)).add(vertical.clone().multiply(radius * Math.sin(flowAngle)));
                player.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(flowOffset), 1, 0, 0, 0, 0);
            }
        }

        private void playChargeSounds(Location center, double progress, List<Player> nearbyPlayers) {
            PlayerPreferencesManager prefsManager = plugin.getPlayerPreferencesManager();
            for (Player p : nearbyPlayers) {
                PlayerPreferences viewerPrefs = prefsManager.getPreferences(p);
                if (viewerPrefs.canShowSpellParticles()) { // Verifica a preferência INDIVIDUAL do espectador
                    p.playSound(center, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.4f, 1.2f + (float)progress);

                    if (ticksElapsed == 0)
                        p.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.6f, 1.8f);
                    if (ticksElapsed == TICKS_PER_RING_DRAW)
                        p.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7f, 1.9f);
                    if (ticksElapsed == 2 * TICKS_PER_RING_DRAW)
                        p.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 2.0f);
                    if (ticksElapsed == 3 * TICKS_PER_RING_DRAW)
                        p.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.85f, 2.05f);
                    if (ticksElapsed == 4 * TICKS_PER_RING_DRAW)
                        p.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.9f, 2.1f);
                }
            }
        }
    }

    /**
     * Tarefa interna que controla o movimento e a colisão do feixe de energia.
     */
    private class BeamTask extends BukkitRunnable {
        private final Player caster;
        private final Vector direction;
        private final Location startLocation;
        private Location beamHeadLocation;
        private double distanceTraveled = 0;

        public BeamTask(Player caster) {
            this.caster = caster;
            this.startLocation = caster.getEyeLocation();
            this.beamHeadLocation = startLocation.clone();
            this.direction = startLocation.getDirection().normalize();
        }

        @Override
        public void run() {
            if (distanceTraveled == 0) {
                caster.sendMessage("§e[DEBUG] Iniciando Tiro Potente. Alcance configurado: " + getMaxRange());
            }

            PlayerPreferencesManager prefsManager = plugin.getPlayerPreferencesManager();
            List<Player> nearbyPlayers = getPlayersInRadius(beamHeadLocation, getMaxRange() + 20.0);

            // Desenha o rastro do feixe para jogadores que podem vê-lo
            for (double d = Math.max(0, distanceTraveled - BEAM_TRAIL_LENGTH); d < distanceTraveled; d += PARTICLE_DENSITY) {
                Location particleLoc = startLocation.clone().add(direction.clone().multiply(d));
                for (Player p : nearbyPlayers) {
                    if (prefsManager.getPreferences(p).canShowSpellParticles()) {
                        p.spawnParticle(Particle.GLOW_SQUID_INK, particleLoc, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Verifica colisão com entidades
            for (Entity entity : beamHeadLocation.getWorld().getNearbyEntities(beamHeadLocation, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(caster.getUniqueId()) && !(entity instanceof FallingBlock)) {
                    handleEntityImpact((LivingEntity) entity, nearbyPlayers, prefsManager);
                    this.cancel();
                    return;
                }
            }

            beamHeadLocation.add(direction.clone().multiply(BEAM_SPEED));
            distanceTraveled += BEAM_SPEED;

            // Verifica colisão com blocos ou alcance máximo
            if (distanceTraveled > getMaxRange() || beamHeadLocation.getBlock().getType().isSolid()) {
                if (beamHeadLocation.getBlock().getType().isSolid()) {
                    handleBlockImpact(nearbyPlayers, prefsManager);
                } else {
                    handleMaxRangeImpact(nearbyPlayers, prefsManager);
                }
                this.cancel();
            }
        }

        private void handleEntityImpact(LivingEntity target, List<Player> nearbyPlayers, PlayerPreferencesManager prefsManager) {
            target.damage(BASE_DAMAGE, caster);
            Vector knockbackDirection = caster.getLocation().getDirection().normalize();
            target.setVelocity(knockbackDirection.multiply(KNOCKBACK_STRENGTH).setY(0.4));

            Location impactLocation = target.getEyeLocation();
            for (Player p : nearbyPlayers) {
                if (prefsManager.getPreferences(p).canShowSpellParticles()) {
                    p.spawnParticle(Particle.EXPLOSION_EMITTER, impactLocation, 1);
                    p.spawnParticle(Particle.CRIT, impactLocation, 30, 0.5, 0.5, 0.5, 0.2);
                    p.playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
                    p.playSound(impactLocation, Sound.BLOCK_ANVIL_LAND, 0.8F, 0.5F);
                }
            }
        }

        private void handleBlockImpact(List<Player> nearbyPlayers, PlayerPreferencesManager prefsManager) {
            List<Material> unbreakableBlocks = List.of(Material.BEDROCK, Material.BARRIER, Material.END_PORTAL_FRAME, Material.OBSIDIAN);
            List<Block> affectedBlocks = WorldEffectManager.getBlocksInExplosionRadius(beamHeadLocation, 2, 0.75, unbreakableBlocks);

            boolean showAdvancedVisuals = nearbyPlayers.stream()
                    .anyMatch(p -> prefsManager.getPreferences(p).canShowMapEffectVisuals());

            if (showAdvancedVisuals && !affectedBlocks.isEmpty()) {
                List<Entity> flyingBlocks = new ArrayList<>();
                for (Block block : affectedBlocks) {
                    Entity fb = WorldEffectManager.createFlyingBlockVisual(block);
                    if (fb != null) {
                        flyingBlocks.add(fb);
                    }
                }

                for (Player p : nearbyPlayers) {
                    PlayerPreferences viewerPrefs = prefsManager.getPreferences(p);
                    if (viewerPrefs.canShowMapEffectVisuals()) {
                        p.spawnParticle(Particle.EXPLOSION_EMITTER, beamHeadLocation, 1);
                        p.playSound(beamHeadLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
                    } else {
                        for (Entity fb : flyingBlocks) {
                            p.hideEntity(plugin, fb);
                        }
                        p.spawnParticle(Particle.LARGE_SMOKE, beamHeadLocation, 20, 0.5, 0.5, 0.5, 0.05);
                        p.playSound(beamHeadLocation, Sound.BLOCK_STONE_HIT, 1.0f, 0.8f);
                    }
                }
            } else {
                // Nenhum jogador quer ver os visuais avançados, ou não há blocos para afetar.
                // Mostra o efeito simples para todos os jogadores próximos.
                for (Player p : nearbyPlayers) {
                    p.spawnParticle(Particle.LARGE_SMOKE, beamHeadLocation, 20, 0.5, 0.5, 0.5, 0.05);
                    p.playSound(beamHeadLocation, Sound.BLOCK_STONE_HIT, 1.0f, 0.8f);
                }
            }
        }

        private void handleMaxRangeImpact(List<Player> nearbyPlayers, PlayerPreferencesManager prefsManager) {
            for (Player p : nearbyPlayers) {
                if (prefsManager.getPreferences(p).canShowSpellParticles()) {
                    p.spawnParticle(Particle.LARGE_SMOKE, beamHeadLocation, 10, 0.1, 0.1, 0.1, 0.02);
                    p.playSound(beamHeadLocation, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.5f);
                }
            }
        }
    }
}