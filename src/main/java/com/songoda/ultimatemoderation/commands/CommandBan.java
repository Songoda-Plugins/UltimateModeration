package com.songoda.ultimatemoderation.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.craftaro.core.utils.TimeUtils;
import com.songoda.ultimatemoderation.UltimateModeration;
import com.songoda.ultimatemoderation.punish.Punishment;
import com.songoda.ultimatemoderation.punish.PunishmentType;
import com.songoda.ultimatemoderation.utils.VaultPermissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandBan extends AbstractCommand {
    private final UltimateModeration plugin;

    public CommandBan(UltimateModeration plugin) {
        super(CommandType.CONSOLE_OK, "Ban");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (args.length < 1) {
            return ReturnType.SYNTAX_ERROR;
        }

        // I dream of the day when someone creates a ticket because
        // they can't ban someone for the reason "Stole me 2h sword"
        long duration = 0;
        StringBuilder reasonBuilder = new StringBuilder();
        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                String line = args[i];
                long time = TimeUtils.parseTime(line);
                if (time != 0) {
                    duration += time;
                } else {
                    reasonBuilder.append(line).append(" ");
                }

            }
        }
        String reason = reasonBuilder.toString().trim();

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);

        if (this.plugin.getPunishmentManager().getPlayer(player).getActivePunishments()
                .stream().anyMatch(appliedPunishment -> appliedPunishment.getPunishmentType() == PunishmentType.BAN)) {
            this.plugin.getLocale().newMessage("That player is already banned.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        if (duration == 0 && !sender.hasPermission("um.ban.permanent")) {
            this.plugin.getLocale().getMessage("event.general.nopermission").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        long durationFinal = duration;
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            if (sender instanceof Player && VaultPermissions.hasPermission(player, "um.ban.exempt")) {
                this.plugin.getLocale().newMessage("You cannot ban this player.").sendPrefixedMessage(sender);
                return;
            }

            new Punishment(PunishmentType.BAN, durationFinal == 0 ? -1 : durationFinal, reason.equals("") ? null : reason)
                    .execute(sender, player);
        });

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        } else if (args.length == 2) {
            return Arrays.asList("1D", "2D", "3D", "4D");
        } else if (args.length == 3) {
            return Collections.singletonList("For being bad");
        }
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "um.ban";
    }

    @Override
    public String getSyntax() {
        return "/Ban <player> [duration] [reason]";
    }

    @Override
    public String getDescription() {
        return "Allows you to ban players.";
    }
}
