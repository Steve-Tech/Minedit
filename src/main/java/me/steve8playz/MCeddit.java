package me.steve8playz;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

public class MCeddit extends JavaPlugin {
    //Final Variables to put in Config file later
    private File playerConfigFile;
    private FileConfiguration playerConfig;

    @Override
    public void onEnable() {
        getLogger().info("MCeddit " + this.getDescription().getVersion() + " has been Enabled");
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadPlayerConfig();
        getCommand("LinkReddit").setExecutor(new Commands(this));
        getCommand("GetReddit").setExecutor(new Commands(this));
        getCommand("Reddit").setExecutor(new Commands(this));
        this.getServer().getPluginManager().registerEvents(new CakeDay(this), this);
    }

    @Override
    public void onDisable() {
        saveConfig();
        savePlayerConfig();
        getLogger().info("MCeddit " + this.getDescription().getVersion() + " has been Disabled");
    }

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

    public JsonObject getRedditURL(String link) {
        StringBuilder jsonSB = new StringBuilder();
        try {
            URL url = new URL(link);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "MCeddit - A Simple Spigot Plugin to link Minecraft with Reddit");
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                jsonSB.append(inputLine);
            }
            in.close();

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonSB.toString(), JsonObject.class);
            return jsonObject.get("data").getAsJsonObject();

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

    public String getReddit(String username, String object) {
        return getRedditURL("https://www.reddit.com/user/" + username + "/about.json").get(object).getAsString();
    }

    public String[][] getSubreddit(String name, String query) {
        JsonObject data;
        if (query != null) {
            data = getRedditURL("https://www.reddit.com/r/" + name + ".json?limit=" + getConfig().getInt("PostLimit") +
                    "&" + query);
        } else {
            data = getRedditURL("https://www.reddit.com/r/" + name + ".json?limit=" + getConfig().getInt("PostLimit"));
        }
        JsonArray jsonPosts = data.get("children").getAsJsonArray();
        String[] post;
        String[][] posts = {};
        for (int i = 0; i < getConfig().getInt("PostLimit"); i++) {
            JsonObject jsonPost = jsonPosts.get(i).getAsJsonObject().get("data").getAsJsonObject();
            post = new String[]{jsonPost.get("subreddit").getAsString(), jsonPost.get("title").getAsString(),
                    jsonPost.get("score").getAsString(), jsonPost.get("author").getAsString(),
                    jsonPost.get("num_comments").getAsString(), jsonPost.get("permalink").getAsString()};

            posts = Arrays.copyOf(posts, posts.length + 1);
            posts[posts.length - 1] = post;
        }

        posts = Arrays.copyOf(posts, posts.length + 1);
        posts[posts.length - 1] = new String[]{data.get("after").getAsString()};

        return posts;
    }
}
