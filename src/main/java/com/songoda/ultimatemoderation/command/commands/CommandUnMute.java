package com.songoda.ultimatemoderation.command.commands;

import com.songoda.ultimatemoderation.UltimateModeration;
import com.songoda.ultimatemoderation.command.AbstractCommand;
import com.songoda.ultimatemoderation.punish.PunishmentType;
import com.songoda.ultimatemoderation.punish.player.PlayerPunishData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandUnMute extends AbstractCommand {

    public CommandUnMute() {
        super(false, true, "UnMute");
    }

    @Override
    protected ReturnType runCommand(UltimateModeration instance, CommandSender sender, String... args) {
        if (args.length != 1)
            return ReturnType.SYNTAX_ERROR;

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);

        if (player == null) {
            instance.getLocale().newMessage("That player does not exist.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        if (!instance.getPunishmentManager().getPlayer(player).getActivePunishments()
                .stream().anyMatch(appliedPunishment -> appliedPunishment.getPunishmentType() == PunishmentType.MUTE)) {
            instance.getLocale().newMessage("That player isn't muted.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        PlayerPunishData playerPunishData = instance.getPunishmentManager().getPlayer(player);

        playerPunishData.expirePunishments(PunishmentType.MUTE);

        instance.getLocale().newMessage(instance.getLocale().getMessage("event.unmute.success")
                .processPlaceholder("player", player.getName()).getMessage()).sendPrefixedMessage(sender);
        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(UltimateModeration instance, CommandSender sender, String... args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "um.mute";
    }

    @Override
    public String getSyntax() {
        return "/UnMute <player>";
    }

    @Override
    public String getDescription() {
        return "Allows you to unmute players.";
    }
}
