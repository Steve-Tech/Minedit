package me.steve8playz;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

public class Commands implements CommandExecutor {
    private final MCeddit plugin;

    public Commands(MCeddit plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = (Player) sender;

        String messagePrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("PluginPrefix") + " ");

        // TODO: Make the linked account only to be changed once without admin permissions
        if ((cmd.getName().equalsIgnoreCase("LinkReddit")) && (sender.hasPermission("mceddit.link"))) {
            if (args.length == 1) {
                if (sender instanceof Player) {
                    plugin.setPlayerConfig(player.getUniqueId().toString(), args[0].replace("u/", ""));
                    sender.sendMessage(messagePrefix + ChatColor.GREEN + "User Linked!");
                } else plugin.getLogger().warning("You need to be a player to run this command.");
            } else if (args.length == 2 && sender.hasPermission("mceddit.link.players")) {
                if (Bukkit.getPlayer(args[0]).isOnline()) {
                    plugin.setPlayerConfig(args[1], Bukkit.getPlayer(args[0]).getUniqueId());
                    sender.sendMessage(messagePrefix + ChatColor.GREEN + "User Linked!");
                } else sender.sendMessage(messagePrefix + ChatColor.RED + "Error: Player not Online.");
            } else {
                sender.sendMessage(messagePrefix + ChatColor.RED + "Usage: /LinkReddit <reddit username> [player]");
            }
        }
        if ((cmd.getName().equalsIgnoreCase("GetReddit")) && (sender.hasPermission("mceddit.get"))) {
            if (args.length == 1) {
                if (sender instanceof Player) {
                    if (plugin.getPlayerConfig().contains(player.getUniqueId().toString())) {
                        String message = plugin.getReddit(plugin.getConfig().getString(player.getUniqueId().toString()), args[0]);
                        if (message != null) sender.sendMessage(message);
                        else
                            sender.sendMessage(messagePrefix + ChatColor.DARK_RED + "An Error Occurred. It is likely that you mistyped an Object in the API or there is no internet.");
                    } else sender.sendMessage(messagePrefix + "Link your Reddit account first");
                } else plugin.getLogger().warning("You need to be a player to run this command.");
            } else if (args.length == 2 && sender.hasPermission("mceddit.get.players")) {
                if (Bukkit.getPlayer(args[1]).isOnline()) {
                    String message = plugin.getReddit(plugin.getConfig().getString(Bukkit.getPlayer(args[1]).getUniqueId().toString()), args[0]);
                    if (message != null) sender.sendMessage(message);
                    else
                        sender.sendMessage(messagePrefix + ChatColor.DARK_RED + "An Error Occurred. " + ChatColor.RED + "It is likely that you mistyped an Object in the API or there is no internet.");
                } else sender.sendMessage(messagePrefix + ChatColor.RED + "Error: Player not Online.");
            } else {
                sender.sendMessage(messagePrefix + ChatColor.RED + "Usage: /GetReddit <API Object> [player]");
            }
        }

        if ((cmd.getName().equalsIgnoreCase("reddit")) && (sender.hasPermission("mceddit.view"))) {
            if (sender instanceof Player) {
                String[][] posts;
                String subreddit;
                if (args.length == 1) {
                    subreddit = args[0];
                    posts = plugin.getSubreddit(subreddit, null);
                } else if (args.length == 2) {
                    subreddit = args[0];
                    posts = plugin.getSubreddit(subreddit, args[1]);
                } else {
                    subreddit = plugin.getConfig().getString("DefaultSubreddit");
                    posts = plugin.getSubreddit(subreddit, null);
                }

                TextComponent border = new TextComponent(new String(new char[50]).replace('\0', '-'));
                border.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);

                player.spigot().sendMessage(border);

                for (int i = 0; i < plugin.getConfig().getInt("PostLimit"); i++) {
                    String[] post = posts[i];

                    TextComponent title = new TextComponent(post[1]);
                    title.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.reddit.com" + post[5]));
                    title.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                    title.setUnderlined(true);

                    TextComponent text = new TextComponent("submitted by " + post[3] + " to r/" + post[0] + "\n" +
                            post[2] + " upvotes with " + post[4] + " comments");
                    text.setColor(net.md_5.bungee.api.ChatColor.GRAY);

                    player.spigot().sendMessage(title);
                    player.spigot().sendMessage(text);
                }

                TextComponent nextButton = new TextComponent("[Next]");
                nextButton.setBold(true);

                nextButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reddit " +
                        subreddit + " after=" + posts[posts.length - 1][0]));
                    player.spigot().sendMessage(nextButton);
                    player.spigot().sendMessage(border);
            } else plugin.getLogger().warning("You need to be a player to run this command.");
        }
        return true;
    }
}
