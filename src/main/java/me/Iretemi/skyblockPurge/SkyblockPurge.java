package me.Iretemi.skyblockPurge;

import gg.lode.leadapi.ILeadAPI;
import io.papermc.paper.ban.BanListType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

import static me.Iretemi.skyblockPurge.PlayerDistribution.checkPlayers;
import me.Iretemi.skyblockPurge.Strike;
import org.bukkit.util.StringUtil;


public final class SkyblockPurge extends JavaPlugin implements CommandExecutor, Listener, TabCompleter {

    public boolean joinsLocked = false;
    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("SkyblockPurge").setExecutor(this);
        getCommand("Strike").setExecutor(new Strike());
        getCommand("Strike").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1 && command.getName().equalsIgnoreCase("SkyblockPurge")) {
            return List.of("start", "end", "reload");
        }

        if (args.length == 1 && command.getName().equalsIgnoreCase("Strike")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return StringUtil.copyPartialMatches(args[0], names, new ArrayList<>());
        }

        return Collections.emptyList();
    }

    JavaPlugin plugin = this;
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player send) || !sender.isOp()) {
            sender.sendMessage("You must be an operator to use this command.");
            return true;
        }
        World world = send.getWorld();
        Title.Times titleTimer = Title.Times.times(Duration.ofSeconds(2),Duration.ofSeconds(7),Duration.ofSeconds(3));
        PotionEffect effect = new PotionEffect(PotionEffectType.BLINDNESS, 600, 3);
        PotionEffect effect2 = new PotionEffect(PotionEffectType.SLOWNESS, 600, 10);
        int maxSize = getConfig().getInt("maxTeamsize");
        // Start event
        // nameable_teams to false
        if (args[0].equalsIgnoreCase("start")) {
            joinsLocked = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    joinsLocked = false;
                    sender.sendMessage("Joins Enabled!");
                }
            }.runTaskLater(plugin, 20L * 60 * 3);

            for (Player eplayer : Bukkit.getOnlinePlayers()) {
                eplayer.playSound(
                        eplayer.getLocation(),
                        Sound.ENTITY_ENDER_DRAGON_GROWL,
                        1.0f, // volume
                        1.0f  // pitch
                );
                eplayer.showTitle(Title.title(
                        Component.text("Skyblock Purge").color(NamedTextColor.RED),
                        Component.text("Starting...."),
                        titleTimer
                ));
                eplayer.addPotionEffect(effect);
                eplayer.addPotionEffect(effect2);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    checkPlayers(plugin, world, maxSize);
                }
            }.runTaskLater(this, 20L * 10);

            return true;
        }


        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("Config reloaded!");
            return true;
        }


        if (args[0].equalsIgnoreCase("end")) {
            for (Player eplayer : Bukkit.getOnlinePlayers()) {
                eplayer.playSound(
                        eplayer.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE,
                        1.0f, // volume
                        1.0f  // pitch
                );
                eplayer.showTitle(Title.title(
                        Component.text("Congratulations!").color(NamedTextColor.GOLD),
                        Component.text("You have survived the Skyblock purge!"),
                        titleTimer
                ));
            }

            new BukkitRunnable() {
                int count = 0;
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()){
                        spawnFirework(player);
                    }

                    if (count > 20) {
                        cancel();
                    }else{
                        count++;
                    }
                }
            }.runTaskTimer(this, 40L, 10L);;

            for (Player p : Bukkit.getOnlinePlayers()) {
                PersistentDataContainer pdc = p.getPersistentDataContainer();
                if (pdc.has(MAX_HEALTH_KEY, PersistentDataType.DOUBLE)) {
                    pdc.remove(MAX_HEALTH_KEY);
                }

                AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (attr != null) {
                    double defaultMax = 20.0;
                    attr.setBaseValue(defaultMax);
                    p.setHealth(Math.min(p.getHealth(), defaultMax));
                }
            }

            return true;
        }

            return true;
        }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        double newMax = attr.getBaseValue() - 2.0; // 1 heart = 2 health

        if (newMax < 2.0) {banplayer(player);}
        attr.setBaseValue(newMax);
        saveMaxHealth(player, newMax);
    }

    private final NamespacedKey MAX_HEALTH_KEY =
            new NamespacedKey(this, "permanent_max_health");

    private void saveMaxHealth(Player player, double value) {
        player.getPersistentDataContainer().set(
                MAX_HEALTH_KEY,
                PersistentDataType.DOUBLE,
                value
        );
    }

    private void applyStoredHealth(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        if (!pdc.has(MAX_HEALTH_KEY, PersistentDataType.DOUBLE)) return;

        double value = pdc.get(MAX_HEALTH_KEY, PersistentDataType.DOUBLE);

        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(value);
            player.setHealth(Math.min(player.getHealth(), value));}}



    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyStoredHealth(player);
    }



    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(this, () -> {

            Player player = event.getPlayer();
            applyStoredHealth(player);
            Location deathLoc = player.getLastDeathLocation().clone();
            deathLoc.setY(getConfig().getInt("respawnheight"));

            player.teleport(deathLoc);
            PotionEffect effect = new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 4);
            player.addPotionEffect(effect);
        });
    }



    // Ban system
    public void banplayer(Player player){
        player.showTitle(Title.title(
                Component.text("YOU DIED!").color(NamedTextColor.RED),
                Component.text("Thanks for playing the Skyblock Purge!")
        ));

        PotionEffect effect = new PotionEffect(PotionEffectType.DARKNESS, 700, 2);
        player.addPotionEffect(effect);

        new BukkitRunnable() {
            @Override
            public void run() {
                long oneDayMs = 24L * 60 * 60 * 1000;
                Date unbanDate = new Date(System.currentTimeMillis() + oneDayMs);
                Bukkit.getBanList(BanListType.PROFILE).addBan(player.getPlayerProfile(),"You Died", unbanDate,null);
                player.kick(Component.text("You have been disconnected."));
            }
        }.runTaskLater(this, 20L * 5);

    }

    public void spawnFirework(Player player) {
        Location loc = player.getLocation();

        Firework firework = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.RED, Color.ORANGE, Color.YELLOW)
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(true)
                .build());

        meta.setPower(1); // flight duration (0–3)
        firework.setFireworkMeta(meta);
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!joinsLocked) return;

        event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                Component.text("Player distribution in progress. Try again later.")
        );
    }

    @EventHandler
    public void onCobbleForm(BlockFormEvent event) {
        if (event.getNewState().getType() != Material.COBBLESTONE) return;
        Block block = event.getBlock();
        block.setMetadata("gen_cobble", new FixedMetadataValue(this, true));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.COBBLESTONE) return;

        if (block.hasMetadata("gen_cobble")) {
            block.removeMetadata("gen_cobble", this);
            event.setDropItems(true);

            Material drop = getWeightedRandomDrop();

            block.getWorld().dropItemNaturally(
                    block.getLocation().add(0.5, 0.5, 0.5),
                    new ItemStack(drop, 1)
            );


        }
    }

    private static final Random RANDOM = new Random();
    private Material getWeightedRandomDrop() {
        int roll = RANDOM.nextInt(100); // 0–99
        if  (roll < 25) return Material.WHEAT;             // 25%
        if (roll < 45) return Material.COAL;              // 20%
        if (roll < 65) return Material.IRON_INGOT;        // 20%
        if (roll < 80) return Material.GOLD_INGOT;        // 15%
        if (roll < 88) return Material.DIAMOND;           // 8%
        if (roll < 93) return Material.OBSIDIAN;           // 5%
        if (roll < 98) return Material.LAPIS_LAZULI;      // 5%
        return Material.EXPERIENCE_BOTTLE;                // 2%
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
