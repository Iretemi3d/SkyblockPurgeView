package me.Iretemi.skyblockPurge;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Strike implements CommandExecutor {

    UUID owner = UUID.fromString("90ef1e9d-3c94-499c-abf2-004719b93bd7");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player send) || !((Player) sender).getUniqueId().equals(owner)) {
            sender.sendMessage("You must be Xerolite to use this command, hehehe");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("You need to specify a player name");
            return true;
        }
        if (args.length == 1) {
            Player p = Bukkit.getPlayer(args[0]);
            send.getWorld().strikeLightning(p.getLocation());
            return true;
        }

        return true;
    }
}
