package me.steve8playz.mceddit;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class CakeDay implements Listener {
    private final MCeddit plugin;

    public CakeDay(MCeddit plugin) {
        this.plugin = plugin;
    }

    // Keeps track of the redeemed players so they don't get multiple cakes
    private ArrayList<UUID> redeemedPlayers = new ArrayList<UUID>();

    private static ItemStack bakeCake(String itemName, String itemLore) {
        // Create and return a cake with custom names and lores
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
        // Create and launch a firework at the player's location
        Location loc = player.getLocation();
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta data = fw.getFireworkMeta();
        Color color1 = Color.RED;
        Color color2 = Color.AQUA;
        data.addEffects(FireworkEffect.builder().withColor(color1, color2).with(FireworkEffect.Type.STAR).build());
        fw.setFireworkMeta(data);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("mceddit.cakeday") &&
                plugin.getPlayerConfig().contains(player.getUniqueId().toString())) {
            long redditCreated = (long) Double.parseDouble(plugin.getReddit(plugin.getPlayerConfig().getString(player.getUniqueId().toString()), "created"));
            if (compareUnix(System.currentTimeMillis() / 1000, redditCreated, "MM-dd")) {
                if (!redeemedPlayers.contains(player.getUniqueId())) {
                    // Add player to an array if they've been given cake
                    redeemedPlayers.add(player.getUniqueId());

                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "It's " + player.getName() + "'s Reddit Cake Day Today!");
                    if (!player.hasPermission("mceddit.cakeday.givecake"))
                        player.sendMessage(ChatColor.GOLD + "Happy Cake Day " + player.getName() + "!");
                    else {
                        player.sendMessage(ChatColor.GOLD + "Happy Cake Day " + player.getName() + "! Here's a Cake to Celebrate!");
                        player.getInventory().addItem(bakeCake(player.getName() + "'s Cake", "A Cake for your Reddit Cake Day"));
                    }
                    if (player.hasPermission("mceddit.cakeday.firework")) firework(player);
                }
            } else if (redeemedPlayers.contains(player.getUniqueId())){
                // Remove from array if it's no longer their cakeday
                redeemedPlayers.remove(player.getUniqueId());
            }
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
}
