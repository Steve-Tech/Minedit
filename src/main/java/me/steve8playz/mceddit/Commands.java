package me.steve8playz.mceddit;

import com.bobacadodl.imgmessage.ImageChar;
import com.bobacadodl.imgmessage.ImageMessage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class Commands implements CommandExecutor {
    private final MCeddit plugin;

    public Commands(MCeddit plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {
        final Player player = (Player) sender;

        final String messagePrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("PluginPrefix") + " ");

        // TODO: Make this use reddit's oauth api
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

        if ((cmd.getName().equalsIgnoreCase("reddit")) && (sender.hasPermission("mceddit.view"))) {
            if (sender instanceof Player) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        String subreddit; // Optional Arg 0
                        String[][] posts; // Optional Arg 1

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

                        TextComponent border = new TextComponent(new String( // Creates a border of a variable length
                                new char[plugin.getConfig().getInt("BorderLength")]).replace('\0', '-'));
                        border.setColor(ChatColor.DARK_GRAY.asBungee());

                        player.spigot().sendMessage(border);

                        for (int i = 0; i < plugin.getConfig().getInt("PostLimit") && i < posts.length; i++) {
                            String[] post = posts[i]; // Select the post

                            TextComponent title = new TextComponent(post[1]);
                            title.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redditpost " + post[5]));
                            title.setColor(ChatColor.WHITE.asBungee());
                            title.setUnderlined(true);

                            TextComponent text = new TextComponent("submitted by " + post[3] + " to r/" + post[0] + "\n" +
                                    post[2] + " upvotes with " + post[4] + " comments");
                            text.setColor(ChatColor.GRAY.asBungee());

                            player.spigot().sendMessage(title);
                            player.spigot().sendMessage(text);
                        }

                        TextComponent nextButton = new TextComponent("[Next]");
                        nextButton.setBold(true);

                        nextButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reddit " +
                                subreddit + " after=" + posts[posts.length - 1][0]));
                        player.spigot().sendMessage(nextButton);
                        player.spigot().sendMessage(border);
                    }
                });
            } else plugin.getLogger().warning("You need to be a player to run this command.");
        }

        if ((cmd.getName().equalsIgnoreCase("redditpost")) && (sender.hasPermission("mceddit.view"))) {
            if (sender instanceof Player) {
                if (args.length == 1 || args.length == 2) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            String[][] post = plugin.getPost(args[0]);

                            TextComponent border = new TextComponent(new String( // Creates a border of a variable length
                                    new char[plugin.getConfig().getInt("BorderLength")]).replace('\0', '-'));
                            border.setColor(ChatColor.DARK_GRAY.asBungee());

                            TextComponent title = new TextComponent(post[0][0]);
                            title.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.reddit.com" + post[1][5]));
                            title.setColor(ChatColor.WHITE.asBungee());
                            title.setBold(true);
                            title.setUnderlined(true);

                            String subreddit;

                            if (post[1][0].startsWith("u_")) {
                                subreddit = "u/" + post[1][0].substring(2);
                            } else {
                                subreddit = "r/" + post[1][0];
                            }

                            TextComponent subtitle = new TextComponent("submitted by " + post[1][3] + " to " + subreddit + "\n" +
                                    post[1][2] + " upvotes with " + post[1][4] + " comments");
                            subtitle.setColor(ChatColor.GRAY.asBungee());

                            TextComponent body;

                            if (post[0][2] != null && post[0][2].equals("image")) {
                                // Handles Images
                                BufferedImage imageToSend = null;
                                try {
                                    imageToSend = ImageIO.read(new URL(post[0][1]));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                String[] image = new ImageMessage(imageToSend, plugin.getConfig().getInt("ImageHeight"),
                                        ImageChar.MEDIUM_SHADE.getChar()).getLines();

                                StringBuilder imageNL = new StringBuilder();

                                for (String s : image) {
                                    imageNL.append(s).append('\n');
                                }

                                body = new TextComponent(imageNL.toString());
                                body.setColor(ChatColor.WHITE.asBungee());
                                body.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, post[0][1]));
                            } else if (post[0][2] != null && post[0][2].equals("link")) {
                                // Handles Links
                                body = new TextComponent(post[0][1]);
                                body.setColor(ChatColor.BLUE.asBungee());
                                body.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, post[0][1]));
                                body.setUnderlined(true);

                            } else if (!post[0][1].isEmpty()) {
                                // Handles text posts
                                body = new TextComponent(plugin.markDown(post[0][1], ChatColor.WHITE));
                                body.setColor(ChatColor.WHITE.asBungee());
                            } else {
                                // Handles other posts
                                body = new TextComponent("Unsupported Post");
                                body.setColor(ChatColor.YELLOW.asBungee());
                            }

                            StringBuilder commentsFormatted = new StringBuilder();
                            StringBuilder commentsFormattedShort = new StringBuilder();

                            for (int i = 0; i < post[2].length - 1 && i < plugin.getConfig().getInt("CommentLimit"); i++) {
                                String[] comment = (post[2][i].split(",\t"));

                                String commentMD = plugin.markDown(comment[1], ChatColor.GRAY);
                                //String commentMD = comment[1];

                                // Shorten the comment if its too long
                                String commentShort;
                                int commentLimit = plugin.getConfig().getInt("CommentLength");
                                if (comment[1].length() > commentLimit) {
                                    commentShort = commentMD.substring(0, commentLimit) + ChatColor.RESET + "...";
                                } else {
                                    commentShort = commentMD;
                                }

                                commentsFormatted.append("\n" + ChatColor.WHITE + comment[0] + ChatColor.GRAY + " (" + comment[2] + ")" + ChatColor.WHITE + ": " +
                                        ChatColor.GRAY + commentMD);
                                commentsFormattedShort.append("\n" + ChatColor.WHITE + comment[0] + ChatColor.GRAY + " (" + comment[2] + ")" + ChatColor.WHITE + ": " +
                                        ChatColor.GRAY + commentShort);
                            }

                            TextComponent comments;

                            if (args.length == 2 && args[1].equals("true")) {
                                comments = new TextComponent(commentsFormatted.toString());
                            } else {
                                comments = new TextComponent(commentsFormattedShort.toString());
                                // Click to show whole comment:
                                comments.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redditpost " + args[0] + " true"));
                            }
                            comments.setColor(ChatColor.GRAY.asBungee());

                            player.spigot().sendMessage(border);

                            player.spigot().sendMessage(title);
                            player.spigot().sendMessage(subtitle);
                            player.spigot().sendMessage(body);
                            player.spigot().sendMessage(comments);

                            player.spigot().sendMessage(border);

                        }
                    });
                } else sender.sendMessage(messagePrefix + ChatColor.RED + "Usage: /RedditPost <Permalink>");
            } else plugin.getLogger().warning("You need to be a player to run this command.");
        }
        return true;
    }
}
