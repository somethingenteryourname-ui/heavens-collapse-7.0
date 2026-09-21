package com.heavenscollapse.listeners;

import com.heavenscollapse.HeavensCollapseItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Keeps the Heaven's Collapse identity intact across anvil operations.
 *
 * <p>Combining an enchanted book, repairing, or renaming Heaven's Collapse
 * in an anvil should never strip the PersistentDataContainer tag, lore,
 * glint or unbreakable flag that the special ability depends on. A normal
 * vanilla/Paper anvil merge preserves these automatically, but some
 * servers layer their own enchanting/attribute plugins on top of the
 * anvil GUI that rebuild the result item and can drop custom item data in
 * the process. This listener defensively re-stamps the identity onto the
 * anvil's result the moment it detects it would otherwise be lost, while
 * leaving whatever the anvil actually changed (enchantments, repair,
 * rename) untouched.</p>
 */
public class AnvilProtectionListener implements Listener {

    private final HeavensCollapseItem itemManager;

    public AnvilProtectionListener(HeavensCollapseItem itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        ItemStack base = anvil.getItem(0);
        ItemStack result = event.getResult();

        if (base == null || result == null) {
            return;
        }
        if (!itemManager.isHeavensCollapse(base)) {
            // The item going into the anvil wasn't Heaven's Collapse -
            // nothing for us to protect.
            return;
        }
        if (itemManager.isHeavensCollapse(result)) {
            // Identity survived the merge on its own - nothing to fix.
            return;
        }

        // The anvil produced a result that lost the Heaven's Collapse
        // identity. Reapply it onto the result's own current item meta so
        // whatever the anvil actually did (added an enchant, repaired
        // durability, renamed it) is kept, and only the identity is
        // restored.
        ItemStack fixed = result.clone();
        itemManager.reapplyIdentity(fixed);
        event.setResult(fixed);
    }
}
