package com.heavenscollapse.listeners;

import com.heavenscollapse.HeavensCollapseItem;
import com.heavenscollapse.HeavensCollapsePlugin;
import org.bukkit.EquipmentSlot;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Implements the lightning-based way to obtain Heaven's Collapse.
 *
 * <h2>Chosen interpretation</h2>
 * <p>The request described this mechanic ambiguously ("if a player is
 * holding the mace / is in the required 'mace' state when struck by
 * lightning"). The interpretation implemented here: if a player is struck
 * by lightning while holding a completely ordinary, vanilla Mace in either
 * hand, that specific Mace item transforms in place into Heaven's
 * Collapse. This is thematically consistent (a plain weapon is "charged"
 * by the heavens into the legendary one), and is naturally duplication-safe:
 * the transformation replaces the existing item rather than adding a new
 * one, and a plain Mace that has already been transformed no longer
 * matches {@link HeavensCollapseItem#isPlainMace(ItemStack)}, so re-firing
 * the same strike (or a second strike immediately after) cannot grant a
 * second copy.</p>
 */
public class LightningObtainListener implements Listener {

    private final HeavensCollapsePlugin plugin;
    private final HeavensCollapseItem itemManager;

    public LightningObtainListener(HeavensCollapsePlugin plugin, HeavensCollapseItem itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightningDamage(EntityDamageEvent event) {
        if (!plugin.isLightningObtainEnabled()) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand = inventory.getItemInOffHand();

        EquipmentSlot slotToTransform = null;
        if (itemManager.isPlainMace(mainHand)) {
            slotToTransform = EquipmentSlot.HAND;
        } else if (itemManager.isPlainMace(offHand)) {
            slotToTransform = EquipmentSlot.OFF_HAND;
        }

        if (slotToTransform == null) {
            // Not holding a plain Mace - nothing to transform, lightning
            // behaves completely normally.
            return;
        }

        ItemStack transformed = itemManager.createItem();
        if (slotToTransform == EquipmentSlot.HAND) {
            inventory.setItemInMainHand(transformed);
        } else {
            inventory.setItemInOffHand(transformed);
        }

        if (plugin.isNegateLightningDamage()) {
            event.setCancelled(true);
        }

        String message = plugin.getMessage("received-from-lightning");
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.05);
    }
}
