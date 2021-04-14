package me.stevetech.minedit;

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
import java.time.Instant;
import java.util.Arrays;

public class Commands implements CommandExecutor {
    private final Minedit plugin;

    public Commands(Minedit plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {
        final Player player = sender instanceof Player ? (Player) sender : null;

        final String messagePrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("PluginPrefix") + " ");

        if (player != null) { // Player only commands
            if ((cmd.getName().equalsIgnoreCase("LinkReddit")) && (sender.hasPermission("minedit.link"))) {
                if (args.length == 1) {
                    sender.sendMessage(messagePrefix + ChatColor.AQUA + "Loading...");
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        String token = (String) plugin.getToken(args[0])[0];
                        if (token != null) {
                            String username = plugin.tokenToUsername(token);
                            plugin.setPlayerConfig(player.getUniqueId().toString(), username);
                            sender.sendMessage(messagePrefix + ChatColor.GREEN + "Successfully linked with " + username + '!');
                        } else {
                            sender.sendMessage(messagePrefix + ChatColor.RED + "Invalid Code! Please try again...");
                            ((Player) sender).performCommand("linkreddit");
                        }
                    });
                } else {
                    TextComponent link = new TextComponent(messagePrefix + ChatColor.AQUA +
                            ChatColor.BOLD + ChatColor.UNDERLINE + "Click here to link your Reddit account");
                    link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                            "https://www.reddit.com/api/v1/authorize?client_id=" + plugin.getConfig().getString("redditClientID") + "&response_type=code&state=" +
                                    ((Player) sender).getDisplayName() + ",LinkReddit&redirect_uri=" + plugin.getConfig().getString("redditRedirectURI") + "&duration=temporary&scope=identity"));
                    player.spigot().sendMessage(link);
                    TextComponent suggest = new TextComponent(messagePrefix + ChatColor.AQUA +
                            "Then type /LinkReddit <code> with the code given.");
                    suggest.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/linkreddit "));
                    player.spigot().sendMessage(suggest);
                }
            }

            if ((cmd.getName().equalsIgnoreCase("reddit")) && (sender.hasPermission("minedit.view"))) {
                sender.sendMessage(messagePrefix + ChatColor.AQUA + "Loading...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
                });
            }

            if ((cmd.getName().equalsIgnoreCase("redditpost")) && (sender.hasPermission("minedit.view"))) {
                if (args.length == 1 || args.length == 2) {
                    sender.sendMessage(messagePrefix + ChatColor.AQUA + "Loading...");
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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

                        TextComponent subtitle = new TextComponent();

                        if (sender.hasPermission("minedit.vote")) {
                            TextComponent[] votes = {new TextComponent(" \u2191 "), new TextComponent(" \u2022 "), new TextComponent(" \u2193 ")};

                            votes[0].setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redditvote 1 " + post[0][3]));
                            votes[1].setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redditvote 0 " + post[0][3]));
                            votes[2].setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redditvote -1 " + post[0][3]));

                            votes[0].setColor(ChatColor.WHITE.asBungee());
                            votes[1].setColor(ChatColor.WHITE.asBungee());
                            votes[2].setColor(ChatColor.WHITE.asBungee());

                            subtitle.addExtra(votes[0]);
                            subtitle.addExtra(votes[1]);
                            subtitle.addExtra(votes[2]);
                            subtitle.addExtra(" ");
                        }

                        subtitle.addExtra("submitted by " + post[1][3] + " to " + subreddit + "\n" +
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

                    });
                } else sender.sendMessage(messagePrefix + ChatColor.RED + "Usage: /RedditPost <permalink>");
            }


            if ((cmd.getName().equalsIgnoreCase("redditvote")) && (sender.hasPermission("minedit.vote"))) {
                plugin.getLogger().info(Arrays.toString(plugin.playerSessions.get(player)) + Instant.now().getEpochSecond());
                if (args.length == 2) {
                    if (plugin.playerSessions.containsKey(player) &&
                            (Long) plugin.playerSessions.get(player)[1] > Instant.now().getEpochSecond()) {
                        plugin.tokenToVote((String) plugin.playerSessions.get(player)[0], args[0], args[1]);
                        switch (args[0]) {
                            case "1":
                                sender.sendMessage(messagePrefix + ChatColor.GOLD + "Upvoted");
                                break;
                            case "0":
                                sender.sendMessage(messagePrefix + ChatColor.YELLOW + "Removed Vote");
                                break;
                            case "-1":
                                sender.sendMessage(messagePrefix + ChatColor.BLUE + "Downvoted");
                                break;
                            default:
                                sender.sendMessage(messagePrefix + ChatColor.RED + "Error Voting");
                                return false;
                        }
                    } else {
                        TextComponent start = new TextComponent(messagePrefix + ChatColor.AQUA +
                                ChatColor.BOLD + ChatColor.UNDERLINE + "Click here to start a session");
                        start.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                "https://www.reddit.com/api/v1/authorize?client_id=" + plugin.getConfig().getString("redditClientID") + "&response_type=code&state=" +
                                        ((Player) sender).getDisplayName() + ",RedditVote&redirect_uri=" + plugin.getConfig().getString("redditRedirectURI") + "&duration=temporary&scope=vote"));
                        player.spigot().sendMessage(start);
                        TextComponent suggest = new TextComponent(messagePrefix + ChatColor.AQUA +
                                "Then type /RedditVote <code> with the code given.");
                        suggest.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/redditvote "));
                        player.spigot().sendMessage(suggest);
                    }
                } else if (args.length == 1) {
                    sender.sendMessage(messagePrefix + ChatColor.AQUA + "Loading...");
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        Object[] token = plugin.getToken(args[0]);
                        if (token[0] != null) {
                            plugin.playerSessions.put(player, new Object[]{token[0], (Long) token[1] + Instant.now().getEpochSecond()});
                            sender.sendMessage(messagePrefix + ChatColor.GREEN + "Successfully started session!");
                        } else {
                            sender.sendMessage(messagePrefix + ChatColor.RED + "Invalid Code! Please try again...");
                            ((Player) sender).performCommand("linkreddit");
                        }
                    });
                } else {
                    sender.sendMessage(messagePrefix + ChatColor.RED + "Usage: /RedditVote <code|direction> <fullname>");
                }
            }
        } else {
            plugin.getLogger().warning("You need to be a player to run this command.");
        } // End of player only commands
        
        return true;
    }
}
