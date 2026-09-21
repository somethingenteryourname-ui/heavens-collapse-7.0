package com.heavenscollapse.commands;

import com.heavenscollapse.HeavensCollapseItem;
import com.heavenscollapse.HeavensCollapsePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements {@code /heavenscollapse} and {@code /heavenscollapse <player>}.
 * Requires the {@code heavenscollapse.give} permission (default: op).
 */
public class HeavensCollapseCommand implements CommandExecutor, TabCompleter {

    private final HeavensCollapsePlugin plugin;
    private final HeavensCollapseItem itemManager;

    public HeavensCollapseCommand(HeavensCollapsePlugin plugin, HeavensCollapseItem itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("heavenscollapse.give")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player selfPlayer)) {
                sendMessage(sender, "console-needs-player");
                return true;
            }
            target = selfPlayer;
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                String msg = plugin.getMessage("player-not-found").replace("%player%", args[0]);
                sender.sendMessage(msg.isEmpty() ? ChatColor.RED + "Player not found: " + args[0] : msg);
                return true;
            }
        }

        ItemStack item = itemManager.createItem();
        target.getInventory().addItem(item);

        if (sender.equals(target)) {
            sendMessage(sender, "given");
        } else {
            String givenOther = plugin.getMessage("given-other").replace("%player%", target.getName());
            sender.sendMessage(givenOther.isEmpty() ? ChatColor.GREEN + "Gave Heaven's Collapse to " + target.getName() : givenOther);
            sendMessage(target, "given");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("heavenscollapse.give")) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return Collections.emptyList();
    }

    private void sendMessage(CommandSender sender, String key) {
        String message = plugin.getMessage(key);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }
}
