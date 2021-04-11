package me.steve8playz.mceddit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.unbescape.html.HtmlEscape;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

public class MCeddit extends JavaPlugin {
    // Create Player Storage File Variables
    private File playerConfigFile;
    private FileConfiguration playerConfig;

    // Reddit requires a custom user agent to not get throttled
    private final String userAgent = String.format("%1$s:%2$s:%3$s (by /u/SteveTech_)",
            getServer().getVersion(), getDescription().getName(), getDescription().getVersion());

    // Stores Temporary sessions (for upvotes)
    public HashMap<Player, Object[]> playerSessions = new HashMap<>();

    @Override
    public void onEnable() {
        // Setup Configs
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadPlayerConfig();
        // Set Command Executors
        getCommand("Reddit").setExecutor(new Commands(this));
        getCommand("RedditPost").setExecutor(new Commands(this));
        getCommand("LinkReddit").setExecutor(new Commands(this));
        getCommand("RedditVote").setExecutor(new Commands(this));
        // Register Events
        getServer().getPluginManager().registerEvents(new CakeDay(this), this);

        getLogger().info(getDescription().getName() + ' ' + getDescription().getVersion() + " has been Enabled");
    }

    @Override
    public void onDisable() {
        // Save Configs
        saveConfig();
        savePlayerConfig();

        getLogger().info(getDescription().getName() + ' ' + getDescription().getVersion() + " has been Disabled");
    }

    // Player Storage Files
    public FileConfiguration getPlayerConfig() {
        return playerConfig;
    }

    public void setPlayerConfig(String arg1, Object arg2) {
        playerConfig.set(arg1, arg2);
    }

    private void loadPlayerConfig() {
        playerConfigFile = new File(getDataFolder(), "players.yml");
        if (!playerConfigFile.exists()) {
            playerConfigFile.getParentFile().mkdirs();
            saveResource("players.yml", false);
        }

        playerConfig = new YamlConfiguration();
        try {
            playerConfig.load(playerConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void savePlayerConfig() {
        playerConfigFile = new File(getDataFolder(), "players.yml");
        try {
            playerConfig.save(playerConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Convert Markdown to ChatColor
    public String markDown(String input, ChatColor color) {
        String ending = "$1" + ChatColor.RESET + color;
        return input.replaceAll("\\*\\*(.*?)\\*\\*", ChatColor.BOLD + ending)
                .replaceAll("__(.*?)__", ChatColor.BOLD + ending)
                .replaceAll("\\*(.*?)\\*", ChatColor.ITALIC + ending)
                .replaceAll("_(.*?)_", ChatColor.ITALIC + ending)
                .replaceAll("~~(.*?)~~", ChatColor.STRIKETHROUGH + ending);
    }

    // Error Messages
    private void exceptionMessage(Exception e) {
        if (e.getMessage() == null) {
            getLogger().severe("An Unknown Error Occurred while getting to Reddit or Reading the Reddit API.");
        } else if (e.getMessage().equals("www.reddit.com") || e.getMessage().equals("oauth.reddit.com")) {
            getLogger().severe("An Error Occurred while getting to Reddit, it is very likely that there is no internet.");
        } else {
            getLogger().severe("An Error Occurred while getting to Reddit: " + e.getMessage());
        }
        getLogger().warning("If there are any following errors from this plugin that is a result of this.");
    }

    // Get Reddit API as a String
    public StringBuilder getRedditURL(String link) {
        StringBuilder jsonSB = new StringBuilder();
        try {
            URL url = new URL(link);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", userAgent);
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                jsonSB.append(inputLine);
            }
            in.close();

            return jsonSB;

        } catch (Exception e) {
            exceptionMessage(e);
            return null;
        }
    }

    // Convert String to JSON
    public JsonObject getRedditURLData(String link) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(getRedditURL(link).toString(), JsonObject.class);
        return jsonObject.get("data").getAsJsonObject();
    }

    public JsonArray getRedditURLArr(String link) {
        Gson gson = new Gson();
        return gson.fromJson(getRedditURL(link).toString(), JsonArray.class);
    }

    // Get a User's Reddit
    public String getReddit(String username, String object) {
        return getRedditURLData("https://www.reddit.com/user/" + username + "/about.json").get(object).getAsString();
    }

    // Get Posts in a Subreddit
    public String[][] getSubreddit(String name, String query) {
        JsonObject data;
        if (query != null) {
            data = getRedditURLData("https://www.reddit.com/r/" + name + ".json?limit=" + getConfig().getInt("PostLimit") +
                    "&" + query);
        } else {
            data = getRedditURLData("https://www.reddit.com/r/" + name + ".json?limit=" + getConfig().getInt("PostLimit"));
        }
        JsonArray jsonPosts = data.get("children").getAsJsonArray();
        String[] post;
        String[][] posts = {};
        for (int i = 0; i < getConfig().getInt("PostLimit"); i++) {
            JsonObject jsonPost = jsonPosts.get(i).getAsJsonObject().get("data").getAsJsonObject();
            post = new String[]{jsonPost.get("subreddit").getAsString(), jsonPost.get("title").getAsString(),
                    jsonPost.get("score").getAsString(), jsonPost.get("author").getAsString(),
                    jsonPost.get("num_comments").getAsString(), jsonPost.get("permalink").getAsString(),
                    jsonPost.get("name").getAsString()};

            posts = Arrays.copyOf(posts, posts.length + 1);
            posts[posts.length - 1] = post;
        }

        posts = Arrays.copyOf(posts, posts.length + 1);
        posts[posts.length - 1] = new String[]{data.get("after").getAsString()};

        return posts;
    }

    // Get Post Data [[Title, Content, Type], [Subreddit, Title, Score, Author, No. of Comments, Link], [Comments:[Author, Body, Score]]
    public String[][] getPost(String permalink) {
        JsonArray data = getRedditURLArr("https://www.reddit.com" + permalink + ".json");
        JsonObject postOnly = data.get(0).getAsJsonObject().get("data").getAsJsonObject().get("children").getAsJsonArray().get(0).getAsJsonObject().get("data").getAsJsonObject();
        JsonArray jsonComments = data.get(1).getAsJsonObject().get("data").getAsJsonObject().get("children").getAsJsonArray();

        String[] postData = new String[]{postOnly.get("title").getAsString(), null, null, postOnly.get("name").getAsString()};

        if (postOnly.has("selftext") && !postOnly.get("selftext").getAsString().isEmpty()) {
            postData[1] = postOnly.get("selftext").getAsString();
        } else if (postOnly.has("url") && !postOnly.get("url").getAsString().isEmpty()) {
            postData[1] = postOnly.get("url").getAsString();
        } else {
            postData[1] = "";
        }

        if (postOnly.has("post_hint")) {
            postData[2] = postOnly.get("post_hint").getAsString();
        } else {
            postData[2] = "";
        }

        String[] postText = new String[]{postOnly.get("subreddit").getAsString(), postOnly.get("title").getAsString(),
                postOnly.get("score").getAsString(), postOnly.get("author").getAsString(),
                postOnly.get("num_comments").getAsString(), postOnly.get("permalink").getAsString()};
        String[] postComments = new String[jsonComments.size() + 1];
        for (int i = 0; i < jsonComments.size(); i++) {
            try {
                JsonObject jsonComment = jsonComments.get(i).getAsJsonObject().get("data").getAsJsonObject();
                postComments[i] = jsonComment.get("author").getAsString() + ",\t" +
                        HtmlEscape.unescapeHtml(jsonComment.get("body").getAsString().replace('\u00a0', ' ').replace('\n', ' ')) + ",\t" + // Replace &nbsp; with a space
                        jsonComment.get("score").getAsString() + ",\t";
            } catch (NullPointerException ignored) {
                postComments[i] = "";
            }
        }

        return new String[][]{postData, postText, postComments};
    }


    // Convert Auth Code to Access Token
    public Object[] getToken(String code) {
        try {
            URL url = new URL("https://www.reddit.com/api/v1/access_token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Authorization", "Basic " +
                    new String(Base64.getEncoder().encode((getConfig().getString("redditClientID") + ':' + getConfig().getString("redditClientSecret")).getBytes())));
            String postData = "grant_type=authorization_code&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8.toString()) + "&redirect_uri=" + getConfig().getString("redditRedirectURI");
            OutputStreamWriter writer = new OutputStreamWriter(
                    conn.getOutputStream());
            writer.write(postData);
            writer.flush();
            conn.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content.toString(), JsonObject.class);

            return new Object[]{jsonObject.get("access_token").getAsString(), jsonObject.get("expires_in").getAsLong()};
        } catch (Exception e) {
            exceptionMessage(e);
            return null;
        }
    }

    // Convert Access Token to Username
    public String tokenToUsername(String token) {
        StringBuilder jsonSB = new StringBuilder();
        try {
            URL url = new URL("https://oauth.reddit.com/api/v1/me");
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Authorization", "bearer " + token);
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                jsonSB.append(inputLine);
            }
            in.close();

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonSB.toString(), JsonObject.class);

            return jsonObject.get("name").getAsString();

        } catch (Exception e) {
            exceptionMessage(e);
            return null;
        }
    }

    // Upvote / Downvote using the Access Token
    public void tokenToVote(String token, String direction, String fullname) {
        StringBuilder jsonSB = new StringBuilder();
        try {
            URL url = new URL("https://oauth.reddit.com/api/vote");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Authorization", "bearer " + token);
            String postData = "dir=" + direction + "&id=" + URLEncoder.encode(fullname, StandardCharsets.UTF_8.toString()) + "&rank=1";
            OutputStreamWriter writer = new OutputStreamWriter(
                    conn.getOutputStream());
            writer.write(postData);
            writer.flush();
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                jsonSB.append(inputLine);
            }
            in.close();

        } catch (Exception e) {
            exceptionMessage(e);
        }
    }
}
