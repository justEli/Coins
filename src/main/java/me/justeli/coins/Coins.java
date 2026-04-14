package me.justeli.coins;

import me.justeli.coins.command.CoinsCommand;
import me.justeli.coins.command.DisabledCommand;
import me.justeli.coins.command.WithdrawCommand;
import me.justeli.coins.handler.HopperHandler;
import me.justeli.coins.handler.InventoryHandler;
import me.justeli.coins.handler.InteractionHandler;
import me.justeli.coins.handler.ModificationHandler;
import me.justeli.coins.handler.UnfairMobHandler;
import me.justeli.coins.handler.listener.BukkitEventListener;
import me.justeli.coins.handler.PickupHandler;
import me.justeli.coins.handler.DropHandler;
import me.justeli.coins.handler.listener.PaperEventListener;
import me.justeli.coins.hook.mythicmobs.MMHook;
import me.justeli.coins.hook.bstats.Metrics;
import me.justeli.coins.config.Config;
import me.justeli.coins.config.Settings;
import me.justeli.coins.hook.mythicmobs.MythicMobsHook;
import me.justeli.coins.item.BaseCoin;
import me.justeli.coins.hook.Economies;
import me.justeli.coins.item.CoinUtil;
import me.justeli.coins.item.CreateCoin;
import me.justeli.coins.item.MetaBuilder;
import me.justeli.coins.util.VersionChecker;
import me.justeli.coins.util.Util;
import me.justeli.coins.util.VersionLib;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/* Eli @ December 13, 2016 (creation) */
public final class Coins
    extends JavaPlugin
{
    // TODO
    //  - fix:   you do the command "/withdraw 1 64" and then try to drop only one of the coins, 63 coins of the stack will be consumed
    //  - fix:   does it still /ah dupe?

    private static final ExecutorService ASYNC_THREAD = Executors.newSingleThreadExecutor();

    private static final String UNSUPPORTED_VERSION =
        "Coins only supports Minecraft version 1.19 and newer.";

    private static final String USING_BUKKIT = """
        You seem to be using Bukkit, but the plugin Coins \
        requires at least Spigot! Please use Spigot or Paper. Moving from Bukkit to \
        Spigot will NOT cause any problems with other plugins, since Spigot only adds \
        more features to Bukkit.""";

    private static final String LACKING_ECONOMY = "There is no proper economy installed. Please install %s.";

    @Override
    public void onEnable ()
    {
        long current = System.currentTimeMillis();
        Locale.setDefault(Locale.US);

        if (VersionLib.getMinecraftVersion() < 19)
        {
            line(Level.SEVERE);
            console(Level.SEVERE, UNSUPPORTED_VERSION);
            disablePlugin(UNSUPPORTED_VERSION);
        }

        if (!VersionLib.isSpigot() && !VersionLib.isPaper())
        {
            line(Level.SEVERE);
            console(Level.SEVERE, USING_BUKKIT);
            disablePlugin(USING_BUKKIT);
        }
        
        this.economy = new Economies(this);
        for (String missingPlugin : this.economy.getMissingPluginNames())
        {
            noEconomySupport(missingPlugin);
        }

        if (!VersionLib.isPaper())
        {
            console(Level.WARNING, "Players with a full inventory will be able to pick up coins when Paper is installed.");
        }

        if (getServer().getPluginManager().isPluginEnabled("MythicMobs"))
        {
            Optional<Plugin> mm = Optional.ofNullable(getServer().getPluginManager().getPlugin("MythicMobs"));
            try
            {
                if (mm.isPresent())
                {
                    this.mmHook = new MythicMobsHook(this);
                }
            }
            catch (Exception | NoClassDefFoundError | InstantiationError exception)
            {
                console(Level.WARNING, "Detected MythicMobs, but the version of MythicMobs you are using is not " +
                    "supported. If this is a newer version, please contact support of Coins: https://discord.gg/fVwCETj");
            }
        }

        if (this.disabledReasons.isEmpty())
        {
            this.settings = new Settings(this);
            reload();

            registerEvents();
            registerCommands();

            ASYNC_THREAD.submit(() ->
            {
                versionChecker();
                new Metrics(this).register();
            });
        }
        else
        {
            DisabledCommand disabledCommand = new DisabledCommand(this);
            for (PluginCommand command : disabledCommand.commands())
            {
                command.setExecutor(disabledCommand);
            }

            line(Level.SEVERE);
            console(Level.SEVERE, "Plugin 'Coins' is now disabled, until the issues are fixed.");
            line(Level.SEVERE);
        }
        console(Level.INFO, "Initialized in " + (System.currentTimeMillis() - current) + "ms.");
    }

    public void reload ()
    {
        if (!this.disabledReasons.isEmpty())
        {
            line(Level.SEVERE);
            console(Level.SEVERE, "Plugin 'Coins' is disabled, until issues are fixed and the server is rebooted (see start-up log of Coins).");
            line(Level.SEVERE);
            return;
        }

        Util.resetMultiplier();

        this.settings.resetWarningCount();
        this.settings.parseConfig();
        this.settings.reloadLanguage();

        this.baseCoin = new BaseCoin(this);
        this.createCoin = new CreateCoin(this);
        this.coinUtil = new CoinUtil(this);

        if (this.settings.getWarningCount() != 0)
        {
            console(Level.WARNING, "Loaded the config of Coins with " + this.settings.getWarningCount() + " warnings. Check above here for details.");
        }
    }

    private void noEconomySupport (String kind)
    {
        line(Level.SEVERE);

        String reason = String.format(LACKING_ECONOMY, kind);

        console(Level.SEVERE, reason);
        disablePlugin(reason);
    }

    private void line (Level type)
    {
        console(type, "------------------------------------------------------------------");
    }

    private void disablePlugin (String reason)
    {
        this.disabledReasons.add(reason);
    }

    private void versionChecker ()
    {
        if (!Config.CHECK_FOR_UPDATES)
            return;

        VersionChecker checker = new VersionChecker("JustEli/Coins");
        if (checker.latestVersion().isEmpty())
            return;

        this.latestVersion = checker.latestVersion().get();
        String currentVersion = getDescription().getVersion();

        if (!currentVersion.equals(this.latestVersion.tag()) && !this.latestVersion.preRelease())
        {
            line(Level.WARNING);
            console(Level.WARNING, "  Detected an outdated version of Coins (" + currentVersion + " is installed).");
            console(Level.WARNING, "  The latest version is " + this.latestVersion.tag() + ", released on "
                + Util.DATE_FORMAT.format(new Date(this.latestVersion.time())) + ".");
            console(Level.WARNING, "  Download: " + getDescription().getWebsite());
            line(Level.WARNING);
        }
    }

    private void registerEvents ()
    {
        PluginManager manager = getServer().getPluginManager();

        manager.registerEvents(VersionLib.isPaper()? new PaperEventListener(this) : new BukkitEventListener(this), this);

        this.unfairMobHandler = new UnfairMobHandler(this);
        this.pickupHandler = new PickupHandler(this);

        manager.registerEvents(new HopperHandler(this), this);
        manager.registerEvents(this.unfairMobHandler, this);
        manager.registerEvents(this.pickupHandler, this);
        manager.registerEvents(new DropHandler(this), this);
        manager.registerEvents(new InteractionHandler(this), this);
        manager.registerEvents(new InventoryHandler(this), this);
        manager.registerEvents(new ModificationHandler(this), this);

        if (mmHook().isPresent())
        {
            manager.registerEvents(this.mmHook, this);
        }
    }

    private void registerCommands ()
    {
        CoinsCommand coinsCommand = new CoinsCommand(this);

        coinsCommand.command().setExecutor(coinsCommand);
        coinsCommand.command().setTabCompleter(coinsCommand);

        if (Config.ENABLE_WITHDRAW)
        {
            WithdrawCommand withdrawCommand = new WithdrawCommand(this);

            withdrawCommand.command().setExecutor(withdrawCommand);
            withdrawCommand.command().setTabCompleter(withdrawCommand);
        }
    }

    public void sync (final int ticks, final Runnable runnable)
    {
        getServer().getScheduler().runTaskLater(this, runnable, ticks);
    }

    public void console (Level type, String message)
    {
        getLogger().log(type, message);
    }

    private Economies economy;
    public Economies economy ()
    {
        return this.economy;
    }

    private VersionChecker.Version latestVersion;
    public Optional<VersionChecker.Version> latestVersion ()
    {
        return Optional.ofNullable(this.latestVersion);
    }

    private final List<String> disabledReasons = new ArrayList<>();
    public List<String> disabledReasons ()
    {
        return this.disabledReasons;
    }

    private boolean pluginDisabled = false;

    public boolean isDisabled ()
    {
        return this.pluginDisabled;
    }

    public boolean toggleDisabled ()
    {
        this.pluginDisabled = !this.pluginDisabled;
        return !this.pluginDisabled;
    }

    private MMHook mmHook;

    public Optional<MMHook> mmHook ()
    {
        return Optional.ofNullable(this.mmHook);
    }


    private BaseCoin baseCoin;

    public BaseCoin getBaseCoin ()
    {
        return baseCoin;
    }

    private Settings settings;

    public Settings settings ()
    {
        return settings;
    }

    public MetaBuilder meta (ItemStack itemStack)
    {
        return new MetaBuilder(this, itemStack);
    }

    private CreateCoin createCoin;

    public CreateCoin getCreateCoin ()
    {
        return createCoin;
    }

    private CoinUtil coinUtil;

    public CoinUtil getCoinUtil ()
    {
        return coinUtil;
    }

    private PickupHandler pickupHandler;

    public PickupHandler getPickupHandler ()
    {
        return pickupHandler;
    }

    private UnfairMobHandler unfairMobHandler;

    public UnfairMobHandler getUnfairMobHandler ()
    {
        return unfairMobHandler;
    }
}
