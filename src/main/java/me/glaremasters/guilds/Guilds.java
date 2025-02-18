/*
 * MIT License
 *
 * Copyright (c) 2019 Glare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.glaremasters.guilds;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFBukkitUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import lombok.Getter;
import me.glaremasters.guilds.actions.ActionHandler;
import me.glaremasters.guilds.api.GuildsAPI;
import me.glaremasters.guilds.configuration.SettingsHandler;
import me.glaremasters.guilds.configuration.sections.HooksSettings;
import me.glaremasters.guilds.configuration.sections.PluginSettings;
import me.glaremasters.guilds.cooldowns.CooldownHandler;
import me.glaremasters.guilds.database.DatabaseProvider;
import me.glaremasters.guilds.database.cooldowns.CooldownsProvider;
import me.glaremasters.guilds.database.providers.JsonProvider;
import me.glaremasters.guilds.guild.Guild;
import me.glaremasters.guilds.guild.GuildCode;
import me.glaremasters.guilds.guild.GuildHandler;
import me.glaremasters.guilds.guild.GuildRole;
import me.glaremasters.guilds.guis.GUIHandler;
import me.glaremasters.guilds.listeners.ClaimSignListener;
import me.glaremasters.guilds.listeners.EntityListener;
import me.glaremasters.guilds.listeners.EssentialsChatListener;
import me.glaremasters.guilds.listeners.PlayerListener;
import me.glaremasters.guilds.listeners.TicketListener;
import me.glaremasters.guilds.listeners.VaultBlacklistListener;
import me.glaremasters.guilds.listeners.WorldGuardListener;
import me.glaremasters.guilds.messages.Messages;
import me.glaremasters.guilds.placeholders.PlaceholderAPI;
import me.glaremasters.guilds.updater.UpdateChecker;
import me.glaremasters.guilds.utils.Constants;
import me.glaremasters.guilds.utils.StringUtils;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Getter
public final class Guilds extends JavaPlugin {

    @Getter
    private static GuildsAPI api;
    private GuildHandler guildHandler;
    private CooldownHandler cooldownHandler;
    private static TaskChainFactory taskChainFactory;
    private DatabaseProvider database;
    private CooldownsProvider cooldownsProvider;
    private SettingsHandler settingsHandler;
    private PaperCommandManager commandManager;
    private ActionHandler actionHandler;
    private GUIHandler guiHandler;
    private Economy economy;
    private Permission permissions;
    private List<String> loadedLanguages;
    private boolean successfulLoad = false;

    @Override
    public void onDisable() {
        if (checkVault()) {
            try {
                guildHandler.saveData();
                cooldownHandler.saveCooldowns();
            } catch (IOException e) {
                e.printStackTrace();
            }
            guildHandler.chatLogout();
        }
    }

    @Override
    public void onLoad() {
        try {
            BukkitLibraryManager loader = new BukkitLibraryManager(this);
            loader.addRepository("https://repo.glaremasters.me/repository/public/");
            loader.addMavenCentral();
            loadDepLibs(loader);
            successfulLoad = true;
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Implement Vault's Economy API
     */
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) economy = economyProvider.getProvider();
    }

    /**
     * Implement Vault's Permission API
     */
    private void setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) permissions = rsp.getProvider();
    }

    /**
     * Save and handle new files if needed
     */
    private void saveData() {
        File languageFolder = new File(getDataFolder(), "languages");
        if (!languageFolder.exists()) //noinspection ResultOfMethodCallIgnored
            languageFolder.mkdirs();
        try {
            final JarURLConnection connection = (JarURLConnection) Objects.requireNonNull(getClassLoader().getResource("languages")).openConnection();
            final JarFile thisJar = connection.getJarFile();
            final Enumeration<JarEntry> entries = thisJar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry current = entries.nextElement();
                if (!current.getName().startsWith("languages/") || current.getName().length() == "languages/".length()) {
                    continue;
                }
                final String name = current.getName().substring("languages/".length());
                File langFile = new File(languageFolder, name);
                if (!langFile.exists()) {
                    this.saveResource("languages/" + name, false);
                }
            }

        } catch (final IOException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Load the languages for the server from ACF BCM
     *
     * @param manager ACF BCM
     */
    public void loadLanguages(PaperCommandManager manager) {
        loadedLanguages.clear();
        try {
            File languageFolder = new File(getDataFolder(), "languages");
            for (File file : Objects.requireNonNull(languageFolder.listFiles())) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".yml")) {
                        String updatedName = file.getName().replace(".yml", "");
                        loadedLanguages.add(updatedName);
                        manager.addSupportedLanguage(Locale.forLanguageTag(updatedName));
                        manager.getLocales().loadYamlLanguageFile(new File(languageFolder, file.getName()), Locale.forLanguageTag(updatedName));
                    }
                }
            }
            manager.getLocales().setDefaultLocale(Locale.forLanguageTag(settingsHandler.getSettingsManager().getProperty(PluginSettings.MESSAGES_LANGUAGE)));
            info("Loaded successfully!");
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            info("Failed to load!");
        }
    }

    /**
     * Log any message to console with any level.
     *
     * @param level the log level to log on.
     * @param msg   the message to log.
     */
    public void log(Level level, String msg) {
        getLogger().log(level, msg);
    }

    /**
     * Log a message to console on INFO level.
     *
     * @param msg the msg you want to log.
     */
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    /**
     * Log a message to console on WARNING level.
     *
     * @param msg the msg you want to log.
     */
    public void warn(String msg) {
        log(Level.WARNING, msg);
    }

    /**
     * Log a message to console on SEVERE level.
     *
     * @param msg the msg you want to log.
     */
    public void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     * Guilds logLogo in console
     */
    private void logLogo(ConsoleCommandSender sender) {
        sender.sendMessage(ACFBukkitUtil.color("&a  ________ "));
        sender.sendMessage(ACFBukkitUtil.color("&a /  _____/ "));
        sender.sendMessage(ACFBukkitUtil.color("&a/   \\  ___ " + "  &3Guilds &8v" + getDescription().getVersion()));
        sender.sendMessage(ACFBukkitUtil.color("&a\\    \\_\\  \\" + "  &3Server Version: &8" + getServer().getVersion()));
        sender.sendMessage(ACFBukkitUtil.color("&a \\______  /"));
        sender.sendMessage(ACFBukkitUtil.color("&a        \\/ "));
    }

    @Override
    public void onEnable() {
        if (!successfulLoad) {
            warn("Dependencies could not be downloaded, shutting down to prevent file corruption.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        // Check if the server is running Vault
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            warn("It looks like you don't have Vault on your server! Stopping plugin..");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        loadedLanguages = new ArrayList<>();

        // This is really just for shits and giggles
        // A variable for checking how long startup took.
        long startingTime = System.currentTimeMillis();

        // Load up TaskChain
        taskChainFactory = BukkitTaskChainFactory.create(this);

        // Flex teh guild logLogo
        logLogo(Bukkit.getConsoleSender());

        // Load the config
        info("Loading config..");
        settingsHandler = new SettingsHandler(this);
        info("Loaded config!");

        saveData();

        // Load data here.
        try {
            info("Loading Data..");
            // This will soon be changed to an automatic storage chooser from the config
            // Load the json provider
            database = new JsonProvider(getDataFolder());
            // Load the cooldown folder
            cooldownsProvider = new CooldownsProvider(getDataFolder());
            // Load the cooldown objects
            cooldownHandler = new CooldownHandler(cooldownsProvider);
            // Load guildhandler with provider
            guildHandler = new GuildHandler(database, getCommandManager(), getPermissions(), getConfig(), settingsHandler.getSettingsManager());
            info("Loaded data!");
        } catch (IOException e) {
            severe("An error occurred loading data! Stopping plugin..");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Load Vault
        info("Hooking into Vault..");
        // Setup Vaults Economy Hook
        setupEconomy();
        // Setup Vaults Permission Hook
        setupPermissions();
        info("Hooked into Vault!");

        // If they have placeholderapi, enable it.
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPI(guildHandler).register();
        }

        info("Enabling Metrics..");
        // start bstats
        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.SingleLineChart("guilds", () -> getGuildHandler().getGuildsSize()));
        info("Enabled Metrics!");

        // Initialize the action handler for actions in the plugin
        actionHandler = new ActionHandler();
        info("Loading Commands and Language Data..");
        // Load the ACF command manager
        commandManager = new PaperCommandManager(this);
        commandManager.usePerIssuerLocale(true, false);
        // Load the languages
        loadLanguages(commandManager);
        //deprecated due to being unstable
        //noinspection deprecation
        commandManager.enableUnstableAPI("help");
        // load the custom command contexts
        loadContexts(commandManager);
        // load the custom command completions
        loadCompletions(commandManager);
        // load the dependnecies
        registerDependencies(commandManager);

        // Register all the commands
        Reflections commandClasses = new Reflections("me.glaremasters.guilds.commands");
        Set<Class<? extends BaseCommand>> commands = commandClasses.getSubTypesOf(BaseCommand.class);

        commands.forEach(c -> {
            try {
                commandManager.registerCommand(c.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        guiHandler = new GUIHandler(this, settingsHandler.getSettingsManager(), guildHandler, getCommandManager(), cooldownHandler);

        if (settingsHandler.getSettingsManager().getProperty(PluginSettings.ANNOUNCEMENTS_CONSOLE)) {
            newChain().async(() -> {
                try {
                    info(getAnnouncements());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).execute();
        }

        if (settingsHandler.getSettingsManager().getProperty(PluginSettings.UPDATE_CHECK)) {
            UpdateChecker.init(this, 66176).requestUpdateCheck().whenComplete((result, exception) -> {
                if (result.requiresUpdate()) {
                    this.getLogger().info(String.format("An update is available! Guilds %s may be downloaded on SpigotMC", result.getNewestVersion()));
                    return;
                }

                UpdateChecker.UpdateReason reason = result.getReason();
                if (reason == UpdateChecker.UpdateReason.UP_TO_DATE) {
                    this.getLogger().info(String.format("Your version of Guilds (%s) is up to date!", result.getNewestVersion()));
                } else if (reason == UpdateChecker.UpdateReason.UNRELEASED_VERSION) {
                    this.getLogger().info(String.format("Your version of Guilds (%s) is more recent than the one publicly available. Are you on a development build?", result.getNewestVersion()));
                } else {
                    this.getLogger().warning("Could not check for a new version of Guilds. Reason: " + reason);
                }
            });
        }

        // Load all the listeners
        Stream.of(new EntityListener(guildHandler, settingsHandler.getSettingsManager()), new PlayerListener(guildHandler, settingsHandler.getSettingsManager(), this, permissions), new TicketListener(this, guildHandler, settingsHandler.getSettingsManager()), new VaultBlacklistListener(this, guildHandler, settingsHandler.getSettingsManager())).forEach(l -> Bukkit.getPluginManager().registerEvents(l, this));
        // Load the optional listeners
        optionalListeners();

        info("Enabling the Guilds API..");
        // Initialize the API (probably be placed in different spot?)
        api = new GuildsAPI(getGuildHandler());
        info("Enabled API!");

        // Create cooldowns if they don't exist
        cooldownHandler.createCooldowns();

        info("Ready to go! That only took " + (System.currentTimeMillis() - startingTime) + "ms");
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, () -> {
            try {
                guildHandler.saveData();
                cooldownHandler.saveCooldowns();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 20 * 60, (20 * 60) * settingsHandler.getSettingsManager().getProperty(PluginSettings.SAVE_INTERVAL));

    }

    /**
     * Load the contexts for the server from ACF BCM
     *
     * @param manager ACF BCM
     */
    private void loadContexts(PaperCommandManager manager) {
        manager.getCommandContexts().registerIssuerOnlyContext(Guild.class, c -> {
            Guild guild = guildHandler.getGuild(c.getPlayer());
            if (guild == null)  throw new InvalidCommandArgument(Messages.ERROR__NO_GUILD);
            return guild;
        });

        manager.getCommandContexts().registerIssuerOnlyContext(GuildRole.class, c -> {
            Guild guild = guildHandler.getGuild(c.getPlayer());
            if (guild == null) return null;
            return getGuildHandler().getGuildRole(guild.getMember(c.getPlayer().getUniqueId()).getRole().getLevel());
        });
    }

    private void loadCompletions(PaperCommandManager manager) {

        manager.getCommandCompletions().registerCompletion("members", c -> {
            Guild guild = guildHandler.getGuild(c.getPlayer());
            if (guild == null) return null;
            return guild.getMembers().stream().map(member -> Bukkit.getOfflinePlayer(member.getUuid()).getName()).collect(Collectors.toList());
        });

        manager.getCommandCompletions().registerCompletion("online", c -> Bukkit.getOnlinePlayers().stream().map(member -> Bukkit.getPlayer(member.getUniqueId()).getName()).collect(Collectors.toList()));

        manager.getCommandCompletions().registerCompletion("invitedTo", c -> guildHandler.getInvitedGuilds(c.getPlayer().getUniqueId()));

        manager.getCommandCompletions().registerCompletion("joinableGuilds", c -> guildHandler.getJoinableGuild(c.getPlayer()));

        manager.getCommandCompletions().registerCompletion("guilds", c -> guildHandler.getGuildNames());

        manager.getCommandCompletions().registerAsyncCompletion("allyInvites", c -> {
           Guild guild = guildHandler.getGuild(c.getPlayer());
            if (guild == null) return null;
           if (!guild.hasPendingAllies()) return null;
           return guild.getPendingAllies().stream().map(g -> guildHandler.getNameById(g)).collect(Collectors.toList());
        });

        manager.getCommandCompletions().registerAsyncCompletion("allies", c -> {
            Guild guild = guildHandler.getGuild(c.getPlayer());
            if (guild == null) return null;
            if (!guild.hasAllies()) return null;
            return guild.getAllies().stream().map(g -> guildHandler.getNameById(g)).collect(Collectors.toList());
        });

        manager.getCommandCompletions().registerAsyncCompletion("activeCodes", c -> {
            Guild guild = guildHandler.getGuild(c.getPlayer());
            if (guild == null) return null;
            if (guild.getCodes() == null) return null;
            return guild.getCodes().stream().map(GuildCode::getId).collect(Collectors.toList());
        });

        manager.getCommandCompletions().registerAsyncCompletion("vaultAmount", c -> {
            Guild guild = guildHandler.getGuild(c.getPlayer());
            if (guild == null) return null;
            if (guild.getVaults() == null) return null;
            List<Inventory> list = guildHandler.getCachedVaults().get(guild);
            if (list == null) return null;
            return IntStream.rangeClosed(1, list.size()).mapToObj(Objects::toString).collect(Collectors.toList());
        });

        manager.getCommandCompletions().registerCompletion("languages", c -> loadedLanguages.stream().sorted().collect(Collectors.toList()));
    }

    /**
     * Check if Vault is running
     *
     * @return true or false
     */
    private boolean checkVault() {
        return Bukkit.getPluginManager().isPluginEnabled("Vault");
    }

    //todo what about a hook package with a hook manager for these 3 listeners and PlaceholderAPI?

    /**
     * Register optional listeners based off values in the config
     */
    private void optionalListeners() {
        if (settingsHandler.getSettingsManager().getProperty(HooksSettings.ESSENTIALS)) {
            getServer().getPluginManager().registerEvents(new EssentialsChatListener(guildHandler), this);
        }

        if (settingsHandler.getSettingsManager().getProperty(HooksSettings.WORLDGUARD)) {
            getServer().getPluginManager().registerEvents(new WorldGuardListener(guildHandler), this);
            getServer().getPluginManager().registerEvents(new ClaimSignListener(this, settingsHandler.getSettingsManager(), guildHandler), this);
        }
    }

    /**
     * Get the announcements for the plugin
     * @return announcements
     * @throws IOException
     */
    public String getAnnouncements() throws IOException {
        String announcement;
        URL url = new URL("https://glaremasters.me/guilds/announcements/?id=" + getDescription().getVersion());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", Constants.USER_AGENT);
        try (InputStream in = con.getInputStream()) {
            String result = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
            announcement = StringUtils.convert_html(result);
            con.disconnect();
        } catch (Exception ex) {
            announcement = "Could not fetch announcements!";
        }
        return announcement;
    }

    /**
     * Used to create a new chain of commands
     * @param <T> the type
     * @return chain
     */
    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }

    /**
     * Used to create new shared chain of commands
     * @param name the name of the chain
     * @param <T> the type of chain
     * @return shared chain
     */
    public static <T> TaskChain<T> newSharedChain(String name) {
        return taskChainFactory.newSharedChain(name);
    }

    /**
     * Register dependency annotations for the plugin
     * @param commandManager command manager
     */
    private void registerDependencies(PaperCommandManager commandManager) {
        commandManager.registerDependency(GuildHandler.class, guildHandler);
        commandManager.registerDependency(SettingsManager.class, settingsHandler.getSettingsManager());
        commandManager.registerDependency(ActionHandler.class, actionHandler);
        commandManager.registerDependency(Economy.class, economy);
        commandManager.registerDependency(Permission.class, permissions);
        commandManager.registerDependency(CooldownHandler.class, cooldownHandler);
    }

    /**
     * Load all the dependencies for the plugin
     * @param loader the loader to add to
     */
    private void loadDepLibs(BukkitLibraryManager loader) {

        loader.loadLibrary(Library.builder()
                .groupId("commons-io")
                .artifactId("commons-io")
                .version("2.6")
                .checksum("+HfTBGYKwqFC84ZbrfyXHex+1zx0fH+NXS9ROcpzZRM=")
                .relocate("org{}apache{}commons{}io", "me.glaremasters.guilds.libs.commonsio")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("co.aikar")
                .artifactId("taskchain-core")
                .version("3.7.2")
                .checksum("OpSCCN+7v6gqFpsU/LUNOOXzjImwjyE2ShHZ5xFUj/Q=")
                .relocate("co{}aikar{}taskchain", "me.glaremasters.guilds.libs.taskchain")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("co.aikar")
                .artifactId("taskchain-bukkit")
                .version("3.7.2")
                .checksum("B/O3+zWGalLs8otAr8tdNnIc/39FDRh6tN5qvNgfEaI=")
                .relocate("co{}aikar{}taskchain", "me.glaremasters.guilds.libs.taskchain")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("net.lingala.zip4j")
                .artifactId("zip4j")
                .version("1.3.2")
                .checksum("xnCY1DDFdDEUMnKOvUx8RWcvnM9cZHAutq+4gWwirQg=")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("com.github.stefvanschie.inventoryframework")
                .artifactId("IF")
                .version("0.3.1")
                .checksum("MOPOPYQSpI3jqFrhQkpTABdO2JpoN4kNqFQTxq7KB+E=")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("com.dumptruckman.minecraft")
                .artifactId("JsonConfiguration")
                .version("1.1")
                .checksum("aEEn9nIShT4mvJlF538Mnv+hbP/Yv17ANGchaaBoyCw=")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("net.minidev")
                .artifactId("json-smart")
                .version("1.1.1")
                .checksum("zr2iXDGRqkQWc8Q9elqVZ6pdhqEBAa6RWohckLzuh3E=")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("org.codemc.worldguardwrapper")
                .artifactId("worldguardwrapper")
                .version("1.1.6-SNAPSHOT")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("org.javassist")
                .artifactId("javassist")
                .version("3.21.0-GA")
                .checksum("eqWeAx+UGYSvB9rMbKhebcm9OkhemqJJTLwDTvoSJdA=")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("org.reflections")
                .artifactId("reflections")
                .version("0.9.11")
                .checksum("zKiEKPiokZ34hRBYM9Rf8HvSb5hflu5VaQVRIWtYtKE=")
                .relocate("com{}google{}common", "me.glaremasters.guilds.libs.guava")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("ch.jalu")
                .artifactId("configme")
                .version("1.1.0")
                .checksum("c3EUKZSs/xPSHwn/K0KMf9hTbN0ijRXyIBtOg5PxUnI=")
                .build());

        loader.loadLibrary(Library.builder()
                .groupId("com.google.guava")
                .artifactId("guava")
                .version("21.0")
                .checksum("lyE5cYq8ikiT+njLqM97LJA/Ncl6r0T6MDGwZplItIA=")
                .relocate("com{}google{}common", "me.glaremasters.guilds.libs.guava")
                .build());
    }

}
