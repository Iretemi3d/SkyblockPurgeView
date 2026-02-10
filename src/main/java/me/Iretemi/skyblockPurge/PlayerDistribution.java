package me.Iretemi.skyblockPurge;

import gg.lode.leadapi.ILeadAPI;
import gg.lode.leadapi.LeadAPI;
import gg.lode.leadapi.api.ITeam;
import gg.lode.leadapi.api.ITeamMember;
import gg.lode.leadapi.api.exception.TeamAlreadyExistsException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDistribution {
    private static final ILeadAPI api = LeadAPI.getApi();
    private static List<Player> teamless = new ArrayList<>();

    public static void checkPlayers(JavaPlugin plugin, World world, Integer maxSize) {
        if (api == null) {
            // Lead is not loaded
            return;
        }
        teamless.clear();
        Title.Times titleTimer = Title.Times.times(Duration.ofSeconds(2), Duration.ofSeconds(5), Duration.ofSeconds(2));


        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(
                    Component.text("Distributing Players...").color(NamedTextColor.WHITE),
                    Component.text("Please do not run any commands!"), titleTimer));

            UUID uuid = player.getUniqueId();
            ITeam teamStatus = api.getTeam(uuid);

            if (teamStatus == null) {
                teamless.add(player);
            }
        }

        new BukkitRunnable() {
                @Override
                public void run() {
                    distributePlayers(world, maxSize);
                }
            }.runTaskLater(plugin, 20L * 10);


    }


    public static void distributePlayers(World world, Integer maxSize) {

        Location loc1 = new Location(world, 411, 42, 437);
        Location loc2 = new Location(world, -434, 42, 473);
        Location loc3 = new Location(world, -429, 42, 1327);
        Location loc4 = new Location(world, 448, 42, 1312);


        List<Location> locations = new ArrayList<>();
        locations.add(loc1);
        locations.add(loc2);
        locations.add(loc3);
        locations.add(loc4);

        int index = 0;
        int teamlessSize = teamless.size();
        int memIndex = 0;
        List<ITeam> teams = api.getTeams();

        for (ITeam team : teams) {
            List<ITeamMember> members = team.getMembers();
            //if team size is smaller than 3 add more members
            int size = members.size();
            if (size < 3) {
                for (int i = 0; i < (3 - size); i++) {
                    if (memIndex >= teamlessSize) break;
                    team.addMember(teamless.get(memIndex));
                    memIndex++;
                }
            }

            // Re-fetch members so newly added teamless players are included
            for (ITeamMember member : team.getMembers()) {
                Player player = Bukkit.getPlayer(member.getUniqueId());
                if (player != null && index < locations.size()) {
                    player.teleport(locations.get(index));
                }
            }
            index++;
        }
//if there are more member with no team
        if (memIndex < teamlessSize) {
            int remPlayer = teamlessSize - memIndex;
            int count = (remPlayer + maxSize - 1) / maxSize;// get how many teams that needs to be created
            int teamNum = teams.size();

            for (int i = 0; i < count; i++) {
                teamNum++;
                try {
                    ITeam team = api.createTeamById(String.valueOf(teamNum));// create team continuing from the current max num
                    for (int a = 0; a < maxSize; a++) {
                        if (memIndex >= teamlessSize) break;
                        team.addMember(teamless.get(memIndex));
                        memIndex++;
                    }

                    // Teleport newly created team members
                    for (ITeamMember member : team.getMembers()) {
                        Player player = Bukkit.getPlayer(member.getUniqueId());
                        if (player != null && index < locations.size()) {
                            player.teleport(locations.get(index));
                        }
                    }
                    index++;
                } catch (TeamAlreadyExistsException e) {
                    // A team with that ID already exists, retry with next number
                    i--;
                }
            }
        }
    }
}
