package com.heavenscollapse.listeners;

import com.heavenscollapse.HeavensCollapseItem;
import com.heavenscollapse.HeavensCollapsePlugin;
import com.heavenscollapse.util.AbilityManager;
import com.heavenscollapse.util.EffectsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles every hit landed with the Heaven's Collapse mace: counts it per
 * player (against a single target - see {@link AbilityManager}), and on
 * the configured Nth <b>consecutive, same-target</b> hit checks whether
 * the WIELDER (the attacking player) is airborne. If so, the target is
 * guaranteed to die and the cinematic effects play. If the wielder is
 * standing on the ground, the special fizzles and the hit is treated as a
 * completely normal mace hit.
 *
 * <p>The guaranteed kill goes through Minecraft's normal damage pipeline
 * rather than forcing health to 0 directly, specifically so that Totem of
 * Undying (and anything else hooking damage/death) still works correctly:
 * the target's health is first capped at one heart, and then the
 * triggering hit's damage is set to 20 hearts. Even after armor,
 * enchantment protection and resistance reduce that damage, it still vastly
 * exceeds the one heart of health left - guaranteeing death - while the
 * kill remains a genuine damage event a totem can intercept and consume.</p>
 *
 * <p>When {@code debug.actionbar} is enabled in config.yml, the attacker
 * gets a live action-bar readout of their hit count and, on the checked
 * Nth hit, whether it activated or fizzled because they weren't airborne -
 * this makes the (otherwise invisible) counter and ground check easy to
 * verify while testing.</p>
 */
public class CombatListener implements Listener {

    /** One heart, in Bukkit's health units (max health 20.0 = 10 hearts). */
    private static final double ONE_HEART_HEALTH = 2.0;

    /** Twenty hearts of damage, in Bukkit's health units. */
    private static final double TWENTY_HEARTS_DAMAGE = 40.0;

    private final HeavensCollapsePlugin plugin;
    private final HeavensCollapseItem itemManager;
    private final AbilityManager abilityManager;
    private final EffectsUtil effectsUtil;

    public CombatListener(HeavensCollapsePlugin plugin, HeavensCollapseItem itemManager,
                           AbilityManager abilityManager, EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.abilityManager = abilityManager;
        this.effectsUtil = effectsUtil;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            // Never trigger against the attacker themselves.
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!itemManager.isHeavensCollapse(weapon)) {
            return;
        }

        AbilityManager.HitResult result =
                abilityManager.registerHitVerbose(player.getUniqueId(), target.getUniqueId());

        if (!result.charged()) {
            sendDebugActionbar(player, hitCounterComponent(result.count(), abilityManager.getHitsRequired()));
            return;
        }

        if (player.isOnGround()) {
            // Ground restriction: the special never activates unless the
            // WIELDER is airborne at the moment of the Nth hit. The
            // counter has already been reset by registerHitVerbose(), and
            // this hit proceeds as an entirely normal mace attack.
            sendDebugActionbar(player, Component.text("Heaven's Collapse fizzled - you must be airborne to trigger it.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Three consecutive hits on the same target, wielder airborne:
        // trigger Heaven's Collapse. Cap the target at one heart, then let
        // this hit deal 20 hearts of damage through the normal damage
        // pipeline - lethal even after armor/resistance reduce it, but
        // still a real damage event so Totem of Undying can intercept it.
        target.setHealth(Math.min(ONE_HEART_HEALTH, target.getHealth()));
        event.setDamage(TWENTY_HEARTS_DAMAGE);

        effectsUtil.playSpecialAttack(player, target);
        sendDebugActionbar(player, Component.text("HEAVEN'S COLLAPSE!")
                .color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));
    }

    private Component hitCounterComponent(int count, int required) {
        return Component.text("Heaven's Collapse: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(count + "/" + required).color(NamedTextColor.WHITE));
    }

    private void sendDebugActionbar(Player player, Component message) {
        if (plugin.isDebugActionbarEnabled()) {
            player.sendActionBar(message);
        }
    }
}
