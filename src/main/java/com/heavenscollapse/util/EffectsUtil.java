package com.heavenscollapse.util;

import com.heavenscollapse.HeavensCollapsePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Plays the "divine lightning strike" cinematic: sounds, particles and an
 * optional lightning bolt. Everything here is purely presentational - it
 * never touches health or damage.
 *
 * <p>Every world interaction takes explicit {@link Location}/{@link World}
 * parameters captured by the caller <em>before</em> the killing blow is
 * applied, so this class never depends on the target entity still being
 * alive or even still existing.</p>
 */
public class EffectsUtil {

    /** Rays of the outward warden-particle burst (evenly spaced around a circle). */
    private static final int WARDEN_BURST_RAYS = 24;

    /** How many expanding steps the burst takes before it finishes. */
    private static final int WARDEN_BURST_STEPS = 8;

    /** Final radius, in blocks, the burst reaches. */
    private static final double WARDEN_BURST_MAX_RADIUS = 4.5;

    private final HeavensCollapsePlugin plugin;

    public EffectsUtil(HeavensCollapsePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Plays the full special-attack cinematic centered on the target's
     * current location, with a beam of particles reaching back toward the
     * attacker.
     */
    public void playSpecialAttack(Player attacker, LivingEntity target) {
        World world = target.getWorld();
        Location targetLoc = target.getLocation().add(0, 1.0, 0);
        Location attackerLoc = attacker.getEyeLocation();

        playSounds(world, targetLoc);
        strikeLightning(world, targetLoc);
        playParticles(world, targetLoc, attackerLoc);
    }

    private void playSounds(World world, Location targetLoc) {
        if (!plugin.isSoundsEnabled()) {
            return;
        }
        world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 2.5f, 0.9f);
        world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 2.0f, 1.0f);
        world.playSound(targetLoc, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1.6f, 0.8f);
        world.playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.7f);
        // The Trident's Channeling-strike thunder - distinct from the plain
        // lightning-bolt thunder above - layers in a second, slightly
        // different rumble for a fuller storm sound.
        world.playSound(targetLoc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.8f, 1.0f);
        // The Warden's sonic boom is a rare, instantly recognizable vanilla
        // sound - layering it in gives Heaven's Collapse a distinct signature
        // "custom" identity without requiring a resource pack.
        world.playSound(targetLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.2f, 1.0f);
    }

    private void strikeLightning(World world, Location targetLoc) {
        if (!plugin.isLightningEnabled()) {
            return;
        }
        if (plugin.isLightningDamage()) {
            world.strikeLightning(targetLoc);
            if (!plugin.isLightningFire()) {
                extinguishNearbyFire(targetLoc);
            }
        } else {
            // Visual-only strike: no block damage, no fire, no extra
            // entity damage - just the bolt, flash and thunder.
            world.strikeLightningEffect(targetLoc);
        }
    }

    private void playParticles(World world, Location targetLoc, Location attackerLoc) {
        if (!plugin.isParticlesEnabled()) {
            return;
        }

        world.spawnParticle(Particle.FLASH, targetLoc, 2, 0, 0, 0, 0);
        world.spawnParticle(Particle.END_ROD, targetLoc, 45, 0.4, 0.9, 0.4, 0.05);
        world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 35, 0.5, 1.0, 0.5, 0.15);
        world.spawnParticle(Particle.CLOUD, targetLoc, 18, 0.3, 0.3, 0.3, 0.02);
        // A single Sonic Boom particle carries its own built-in expanding
        // ring animation - an instant, unmistakably "Warden" visual.
        world.spawnParticle(Particle.SONIC_BOOM, targetLoc, 1, 0, 0, 0, 0);
        // A burst of Sculk Charge Pop - the same particle a Sculk Catalyst
        // spits out when it "blooms" - right at the impact point for an
        // immediate, dense flash of teal-black motes before the rings
        // below start traveling outward.
        world.spawnParticle(Particle.SCULK_CHARGE_POP, targetLoc, 25, 0.5, 0.5, 0.5, 0.05);

        spawnBeam(world, attackerLoc, targetLoc);
        spawnWardenBurst(world, targetLoc);
        scheduleSecondBoom(world, targetLoc);
    }

    /**
     * Spawns a short trail of spark particles from the attacker toward the
     * target so the strike reads as connected to the player rather than
     * appearing out of nowhere.
     */
    private void spawnBeam(World world, Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length < 0.5) {
            return;
        }
        direction.normalize();

        int points = (int) Math.min(20, Math.max(4, length * 2));
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            Location point = from.clone().add(direction.clone().multiply(length * t));
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /**
     * Animates rings of Warden-themed particles - Sculk Soul wisps
     * alternating with Sculk Charge Pop motes - shooting outward from the
     * impact point over several ticks, like a shockwave. Runs as a
     * short-lived repeating task rather than a single instant spawn so the
     * particles visibly travel outward instead of just appearing already
     * spread out.
     */
    private void spawnWardenBurst(World world, Location center) {
        double angleStep = (2 * Math.PI) / WARDEN_BURST_RAYS;

        new BukkitRunnable() {
            int step = 1;

            @Override
            public void run() {
                if (step > WARDEN_BURST_STEPS) {
                    cancel();
                    return;
                }

                double radius = (WARDEN_BURST_MAX_RADIUS / WARDEN_BURST_STEPS) * step;
                // Alternate rings between the two particle types so the
                // burst reads as more than a single repeating ring.
                Particle ringParticle = (step % 2 == 0) ? Particle.SCULK_SOUL : Particle.SCULK_CHARGE_POP;

                for (int i = 0; i < WARDEN_BURST_RAYS; i++) {
                    double angle = angleStep * i;
                    double dx = Math.cos(angle) * radius;
                    double dz = Math.sin(angle) * radius;
                    // Gentle vertical waviness so the rings feel organic
                    // rather than perfectly flat.
                    double dy = Math.sin(angle * 2.0 + step) * 0.4;

                    Location point = center.clone().add(dx, dy, dz);
                    world.spawnParticle(ringParticle, point, 1, 0, 0, 0, 0);
                }

                step++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * A second, smaller Sonic Boom pulse a few ticks after the first -
     * echoing the Warden's real attack, which reads as a quick one-two
     * punch rather than a single flat pulse.
     */
    private void scheduleSecondBoom(World world, Location targetLoc) {
        new BukkitRunnable() {
            @Override
            public void run() {
                world.spawnParticle(Particle.SONIC_BOOM, targetLoc, 1, 0, 0, 0, 0);
            }
        }.runTaskLater(plugin, 5L);
    }

    private void extinguishNearbyFire(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    Material type = block.getType();
                    if (type == Material.FIRE || type == Material.SOUL_FIRE) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }
}
