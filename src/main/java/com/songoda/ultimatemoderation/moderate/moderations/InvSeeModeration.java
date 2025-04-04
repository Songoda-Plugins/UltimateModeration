package com.songoda.ultimatemoderation.moderate.moderations;

import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.ultimatemoderation.UltimateModeration;
import com.songoda.ultimatemoderation.moderate.AbstractModeration;
import com.songoda.ultimatemoderation.moderate.ModerationType;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InvSeeModeration extends AbstractModeration {
    public InvSeeModeration(UltimateModeration plugin) {
        super(plugin, true, false);
        registerCommand(plugin);
    }

    @Override
    public ModerationType getType() {
        return ModerationType.INV_SEE;
    }

    @Override
    public XMaterial getIcon() {
        return XMaterial.CHEST;
    }

    @Override
    public String getProper() {
        return "InvSee";
    }

    @Override
    public String getDescription() {
        return "Allows you to see inside of a players inventory.";
    }

    @Override
    protected boolean runModeration(CommandSender runner, OfflinePlayer toModerate) {
        Player toModeratePlayer = (Player) toModerate;

        ((Player) runner).openInventory(toModeratePlayer.getInventory());
        return false;
    }
}
