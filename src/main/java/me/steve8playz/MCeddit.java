package me.steve8playz;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MCeddit extends JavaPlugin implements Listener {
    //Final Variables to put in Config file later
    private final String messagePrefix = ChatColor.GOLD + "[MCeddit] ";

    @Override
    public void onEnable() {
        getLogger().info("MCeddit v1.0 has been Enabled");
        getConfig().options().copyDefaults(true);
        saveConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("MCeddit v1.0 has been Disabled");
    }


    // Keeps track of the cooldowns for multiple players
    private HashMap<String, Long> cooldowns = new HashMap<String, Long>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();


        if (player.hasPermission("mceddit.cakeday") && getConfig().contains(player.getUniqueId().toString())) {
            long redditCreated = (long) Double.parseDouble(getReddit(getConfig().getString(player.getUniqueId().toString()), "created"));
            if (compareUnix(System.currentTimeMillis() / 1000, redditCreated, "MM-dd")) {

                // Cooldown to stop this from executing twice
                // TODO: Find a better way of doing this
                // TODO: Put cooldownTime into config
                int cooldownTime = 86400;
                if(cooldowns.containsKey(player.getName())) {
                    long secondsLeft = ((cooldowns.get(player.getName())/1000)+cooldownTime) - (System.currentTimeMillis()/1000);
                    if(secondsLeft>0) {
                        // player.sendMessage("You cant use that commands for another "+ secondsLeft +" seconds!");
                        return;
                    }
                }
                cooldowns.put(player.getName(), System.currentTimeMillis());
                // END Cooldown Code

                getServer().broadcastMessage(ChatColor.GOLD + "It's " + player.getName() + "'s Reddit Cake Day Today!");
                if (!player.hasPermission("mceddit.cakeday.givecake"))
                    player.sendMessage(ChatColor.GOLD + "Happy Cake Day " + player.getName() + "!");
                else {
                    player.sendMessage(ChatColor.GOLD + "Happy Cake Day " + player.getName() + "! Here's a Cake to Celebrate!");
                    player.getInventory().addItem(bakeCake(player.getName() + "'s Cake", "A Cake for your Reddit Cake Day"));
                }
                if (player.hasPermission("mceddit.cakeday.firework")) firework(player);
            }
        }
    }

    public String getReddit(String username, String object) {
        try {
            String jsonS = "";
            URL url = new URL("https://www.reddit.com/user/" + username + "/about.json");
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "MCeddit - A Simple Spigot Plugin to link Minecraft with Reddit");
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while((inputLine = in.readLine()) != null) {
                jsonS+=inputLine;
            }
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonS, JsonObject.class);
            JsonObject data = jsonObject.get("data").getAsJsonObject();
            String result = data.get(object).getAsString();

            in.close();

            return result;
        } catch (Exception e) {
            if (e.getMessage() == null) {
                getLogger().severe("An Unknown Error Occurred while getting to Reddit or Reading the Reddit API, it is possibly because of a bad JSON Object.");
                getLogger().warning("If there are any following errors from this plugin that is a result of this.");
            } else if (e.getMessage() == "www.reddit.com") {
                getLogger().severe("An Error Occurred while getting to Reddit, it is very likely that there is no internet.");
                getLogger().warning("If there are any following errors from this plugin that is a result of this.");
            } else {
                getLogger().severe("An Error Occurred while getting to Reddit: " + e.getMessage());
                getLogger().warning("If there are any following errors from this plugin that is a result of this.");
            }
            return null;
        }

    }

    private boolean compareUnix(long unixSeconds, long compareUnixSeconds, String comparePattern) {
        // Convert seconds to milliseconds
        Date date = new java.util.Date(unixSeconds * 1000L);
        Date dateCompare = new java.util.Date(compareUnixSeconds * 1000L);
        // Format the date
        SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(comparePattern);
        SimpleDateFormat dateFormatCompare = new java.text.SimpleDateFormat(comparePattern);
        // Compare
        return ((dateFormat.format(date)).equals(dateFormatCompare.format(dateCompare)));
    }

    private static ItemStack bakeCake(String itemName, String itemLore) {
        ItemStack item = new ItemStack(Material.CAKE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(itemName);
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(itemLore);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void firework(Player player) {
        Location loc = player.getLocation();
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta data = fw.getFireworkMeta();
        Color color1 = Color.RED;
        Color color2 = Color.AQUA;
        data.addEffects(new FireworkEffect[] { FireworkEffect.builder().withColor(color1, color2).with(FireworkEffect.Type.STAR).build() });
        fw.setFireworkMeta(data);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        Player player = (Player) sender;
        PlayerInventory inventory = player.getInventory();
        // TODO: Make the linked account only to be changed once without admin permissions
        if ((cmd.getName().equalsIgnoreCase("LinkReddit")) && (sender.hasPermission("mceddit.link"))) {
            if (args.length == 1) {
                if (sender instanceof Player) {
                    getConfig().set(player.getUniqueId().toString(), args[0].replace("u/", ""));
                    sender.sendMessage(messagePrefix + ChatColor.GREEN + "User Linked!");
                } else getLogger().warning("You need to be a player to run this command.");
            } else if (args.length == 2 && sender.hasPermission("mceddit.link.players")) {
                if (Bukkit.getPlayer(args[0]).isOnline()) {
                    getConfig().set(args[1], Bukkit.getPlayer(args[0]).getUniqueId());
                    sender.sendMessage(messagePrefix + ChatColor.GREEN + "User Linked!");
                } else sender.sendMessage(messagePrefix + ChatColor.RED + "Error: Player not Online.");
            } else {
                sender.sendMessage(messagePrefix + ChatColor.RED + "Usage: /LinkReddit <reddit username> [player]");
            }
        }
        if ((cmd.getName().equalsIgnoreCase("GetReddit")) && (sender.hasPermission("mceddit.get"))) {
            if (args.length == 1) {
                if (sender instanceof Player) {
                    if (getConfig().contains(player.getUniqueId().toString())) {
                        String message = getReddit(getConfig().getString(player.getUniqueId().toString()), args[0]);
                        if (message != null) sender.sendMessage(message);
                        else sender.sendMessage(messagePrefix + ChatColor.DARK_RED + "An Error Occurred. It is likely that you mistyped an Object in the API or there is no internet.");
                    } else sender.sendMessage(messagePrefix + "Link your Reddit account first");
                } else getLogger().warning("You need to be a player to run this command.");
            } else if (args.length == 2 && sender.hasPermission("mceddit.get.players")) {
                if (Bukkit.getPlayer(args[1]).isOnline()) {
                    String message = getReddit(getConfig().getString(Bukkit.getPlayer(args[1]).getUniqueId().toString()), args[0]);
                    if (message != null) sender.sendMessage(message);
                    else sender.sendMessage(messagePrefix + ChatColor.DARK_RED + "An Error Occurred. " + ChatColor.RED + "It is likely that you mistyped an Object in the API or there is no internet.");
                } else sender.sendMessage(messagePrefix + ChatColor.RED + "Error: Player not Online.");
            } else {
                sender.sendMessage(messagePrefix + ChatColor.RED + "Usage: /GetReddit <API Object> [player]");
            }
        }
        return true;
    }
}