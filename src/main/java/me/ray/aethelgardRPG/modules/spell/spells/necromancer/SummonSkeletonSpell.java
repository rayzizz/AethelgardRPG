package me.ray.aethelgardRPG.modules.spell.spells.necromancer;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.api.CustomMobAPI;
import me.ray.aethelgardRPG.modules.custommob.ai.AIType;
import me.ray.aethelgardRPG.modules.custommob.ai.minionimpl.SummonedMinionAIImpl;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import me.ray.aethelgardRPG.modules.spell.animation.AnimationFrame;
import me.ray.aethelgardRPG.modules.spell.animation.AnimationSequence;
import me.ray.aethelgardRPG.modules.spell.animation.effects.ParticleEffect;
import me.ray.aethelgardRPG.modules.spell.animation.effects.SoundEffect;
import me.ray.aethelgardRPG.modules.spell.spells.BaseSpell;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class SummonSkeletonSpell extends BaseSpell {
    private int numberOfSkeletonsToSummon;
    private long skeletonDurationSeconds;
    private boolean canDamageOwner;
    private double skeletonHealth;
    private double skeletonDamage;
    private double skeletonPhysicalDefense;
    private double skeletonMagicalDefense;
    private int skeletonMinionLevel;
    private String skeletonMinionFixedDisplayName;

    private List<String> skeletonAbilities;
    private List<String> skeletonDrops;

    private static final int DEFAULT_SKELETONS_TO_SUMMON = 1;
    private static final long DEFAULT_SKELETON_DURATION_SECONDS = 30L;
    private static final boolean DEFAULT_CAN_DAMAGE_OWNER = false;
    private static final double DEFAULT_SKELETON_HEALTH = 30.0;
    private static final double DEFAULT_SKELETON_DAMAGE = 4.0;
    private static final double DEFAULT_SKELETON_PHYSICAL_DEFENSE = 5.0;
    private static final double DEFAULT_SKELETON_MAGICAL_DEFENSE = 2.0;
    private static final int DEFAULT_SKELETON_MINION_LEVEL = 1;
    private static final String DEFAULT_SKELETON_MINION_FIXED_DISPLAY_NAME = "Summoned Skeleton";

    private final Random random = new Random();

    public SummonSkeletonSpell(AethelgardRPG plugin) {
        super(plugin,
                "summon_skeleton_minion",
                "spell.necromancer.summon_skeleton.name",
                "spell.necromancer.summon_skeleton.description",
                15, // Cooldown
                25, // Custo de mana
                RPGClass.NECROMANTE,
                1,  // Nível Requerido
                0   // Alcance
        );
        this.numberOfSkeletonsToSummon = DEFAULT_SKELETONS_TO_SUMMON;
        this.skeletonDurationSeconds = DEFAULT_SKELETON_DURATION_SECONDS;
        this.canDamageOwner = DEFAULT_CAN_DAMAGE_OWNER;
        this.skeletonHealth = DEFAULT_SKELETON_HEALTH;
        this.skeletonDamage = DEFAULT_SKELETON_DAMAGE;
        this.skeletonPhysicalDefense = DEFAULT_SKELETON_PHYSICAL_DEFENSE;
        this.skeletonMagicalDefense = DEFAULT_SKELETON_MAGICAL_DEFENSE;
        this.skeletonMinionLevel = DEFAULT_SKELETON_MINION_LEVEL;
        this.skeletonMinionFixedDisplayName = DEFAULT_SKELETON_MINION_FIXED_DISPLAY_NAME;
        this.skeletonAbilities = new ArrayList<>();
        this.skeletonDrops = new ArrayList<>();
        this.animation = createAnimation();
    }

    private AnimationSequence createAnimation() {
        AnimationSequence sequence = new AnimationSequence();

        AnimationFrame frame1 = new AnimationFrame()
                .addSoundEffect(new SoundEffect(Sound.ENTITY_SKELETON_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f))
                .addSoundEffect(new SoundEffect(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 0.7f, 1.5f));
        sequence.addFrame(frame1, 0L);

        AnimationFrame frame2 = new AnimationFrame()
                .addParticleEffect(new ParticleEffect(Particle.SOUL_FIRE_FLAME, 35, 0.4, 0.6, 0.4, 0.03, null))
                .addParticleEffect(new ParticleEffect(Particle.SMOKE, 20, 0.3, 0.5, 0.3, 0.02, null));
        sequence.addFrame(frame2, 5L);

        return sequence;
    }

    public void setNumberOfSkeletonsToSummon(int count) {
        this.numberOfSkeletonsToSummon = Math.max(1, count);
    }

    public void setSkeletonDurationSeconds(long seconds) {
        this.skeletonDurationSeconds = Math.max(1, seconds);
    }

    public void setCanDamageOwner(boolean canDamage) {
        this.canDamageOwner = canDamage;
    }

    public void setSkeletonMinionLevel(int level) {
        this.skeletonMinionLevel = Math.max(1, level);
    }

    public void setSkeletonMinionFixedDisplayName(String fixedDisplayName) {
        this.skeletonMinionFixedDisplayName = fixedDisplayName != null ? fixedDisplayName : DEFAULT_SKELETON_MINION_FIXED_DISPLAY_NAME;
    }

    @Override
    public SpellCastResult execute(SpellCastContext context) {
        Player caster = context.getCaster();

        CustomMobAPI customMobAPI = plugin.getCustomMobAPI();
        if (customMobAPI == null) {
            caster.sendMessage(plugin.getMessage(caster, "spell.error.custom_mob_module_missing"));
            return SpellCastResult.FAIL_OTHER;
        }

        int skeletonsSpawned = 0;
        Location casterLocation = caster.getLocation();
        Vector direction = casterLocation.getDirection().normalize();
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

        double forwardDistance = 2.0;
        double spreadFactor = 0.75;

        for (int i = 0; i < numberOfSkeletonsToSummon; i++) {
            String minionId = "spell_necromancer_skeleton_minion_" + caster.getUniqueId().toString().substring(0, 4) + "_" + System.currentTimeMillis() + "_" + i;

            // CORREÇÃO: O argumento 'null' para a chave de linguagem foi removido.
            CustomMobData skeletonMinionData = new CustomMobData(
                    minionId,
                    this.skeletonMinionFixedDisplayName,
                    EntityType.SKELETON,
                    this.skeletonMinionLevel,
                    this.skeletonHealth,
                    this.skeletonDamage,
                    this.skeletonPhysicalDefense,
                    this.skeletonMagicalDefense,
                    0, // experienceDrop
                    AIType.SKELETON_MINION_FROM_SPELL,
                    true, // isPlayerSummonedMinion
                    15.0, // followRange
                    0.0,  // attackRange
                    15.0, // aggroRange
                    "default", // lootTableId
                    null, // regenProfileId (lacaios não regeneram)
                    skeletonAbilities,
                    skeletonDrops,
                    1.0, // scale
                    1    // respawnTimeSeconds
            );

            Location spawnLocation;
            if (numberOfSkeletonsToSummon == 1) {
                spawnLocation = casterLocation.clone()
                        .add(direction.clone().multiply(forwardDistance))
                        .add(0, 0.5, 0);
            } else {
                double sideOffset = (i - (numberOfSkeletonsToSummon - 1.0) / 2.0) * spreadFactor;
                spawnLocation = casterLocation.clone()
                        .add(direction.clone().multiply(forwardDistance))
                        .add(perpendicular.clone().multiply(sideOffset))
                        .add(0, 0.5, 0);
            }

            spawnLocation.add(random.nextDouble() * 0.2 - 0.1, 0, random.nextDouble() * 0.2 - 0.1);

            Optional<LivingEntity> spawnedSkeletonOpt = customMobAPI.spawnCustomMob(skeletonMinionData, spawnLocation);

            if (spawnedSkeletonOpt.isPresent()) {
                skeletonsSpawned++;
                LivingEntity skeleton = spawnedSkeletonOpt.get();
                PersistentDataContainer minionPdc = skeleton.getPersistentDataContainer();

                minionPdc.set(PDCKeys.SUMMON_OWNER_KEY, PersistentDataType.STRING, caster.getUniqueId().toString());
                minionPdc.set(PDCKeys.SUMMON_OWNER_NAME_KEY, PersistentDataType.STRING, caster.getName());
                minionPdc.set(PDCKeys.CAN_DAMAGE_OWNER_KEY, PersistentDataType.BYTE, (byte) (canDamageOwner ? 1 : 0));

                customMobAPI.getCustomMobManager().addMinionToOwnerTeam(skeleton);

                ActiveMobInfo mobInfo = customMobAPI.getCustomMobManager().getActiveMobInfo(skeleton.getUniqueId());
                if (mobInfo != null) {
                    mobInfo.setDespawnTimeMillis(System.currentTimeMillis() + skeletonDurationSeconds * 1000L);
                    skeleton.setCustomNameVisible(true);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (skeleton.isValid() && !skeleton.isDead()) {
                            CustomMobAPI mobAPI = plugin.getCustomMobAPI();
                            if (mobAPI != null && mobAPI.getCustomMobManager() != null) {
                                mobAPI.getCustomMobManager().removeActiveMob(skeleton.getUniqueId());
                            }
                            skeleton.getWorld().spawnParticle(Particle.SOUL, skeleton.getLocation().add(0, 1, 0), 15, 0.2, 0.3, 0.2, 0.01);
                            skeleton.remove();
                            plugin.getLogger().fine("Lacaio esqueleto " + skeleton.getUniqueId() + " despawnou por duração.");
                        }
                    }
                }.runTaskLater(plugin, skeletonDurationSeconds * 20L);
            }
        }

        if (skeletonsSpawned > 0) {
            caster.sendMessage(plugin.getMessage(caster, "spell.necromancer.summon_skeleton.success", skeletonsSpawned));
            return SpellCastResult.SUCCESS;
        } else {
            caster.sendMessage(plugin.getMessage(caster, "spell.necromancer.summon_skeleton.fail"));
            return SpellCastResult.FAIL_OTHER;
        }
    }
}