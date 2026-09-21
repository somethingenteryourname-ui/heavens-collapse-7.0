package com.heavenscollapse.listeners;

import com.heavenscollapse.HeavensCollapseItem;
import com.heavenscollapse.HeavensCollapsePlugin;
import com.heavenscollapse.util.AbilityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Optionally resets a player's Heaven's Collapse hit counter when they
 * switch their held hotbar slot away from the mace.
 *
 * <p>Disabled by default (see {@code reset-on-item-switch} in
 * config.yml) - many players juggle other hotbar items (elytra, rockets,
 * pearls, etc.) between mace swings without meaning to abandon their
 * streak, and briefly touching another slot to equip something shouldn't
 * discard progress that's otherwise still "hot." The idle-timeout in
 * {@link AbilityManager} already resets the streak if the player
 * genuinely stops attacking for a while, which is normally enough on its
 * own. This listener exists for servers that want the stricter
 * "abandon on switch" behaviour and explicitly turn it back on.</p>
 */
public class ItemSwitchListener implements Listener {

    private final HeavensCollapsePlugin plugin;
    private final HeavensCollapseItem itemManager;
    private final AbilityManager abilityManager;

    public ItemSwitchListener(HeavensCollapsePlugin plugin, HeavensCollapseItem itemManager,
                               AbilityManager abilityManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.abilityManager = abilityManager;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!plugin.isResetOnItemSwitchEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (!itemManager.isHeavensCollapse(newItem)) {
            abilityManager.resetPlayer(player.getUniqueId());
        }
    }
}
