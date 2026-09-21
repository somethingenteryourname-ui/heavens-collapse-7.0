package com.heavenscollapse.listeners;

import com.heavenscollapse.util.AbilityManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Removes a player's tracked hit-counter state on disconnect, so the
 * plugin never accumulates stale entries for players who have left
 * (prevents an unbounded memory leak on long-running servers).
 */
public class PlayerQuitListener implements Listener {

    private final AbilityManager abilityManager;

    public PlayerQuitListener(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        abilityManager.resetPlayer(event.getPlayer().getUniqueId());
    }
}
