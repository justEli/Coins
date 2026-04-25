package me.justeli.coins;

import me.justeli.coins.command.CoinsCommandSpigot;
import me.justeli.coins.command.DisabledCommandPaper;
import me.justeli.coins.command.DisabledCommandSpigot;
import me.justeli.coins.command.CoinsCommandPaper;
import me.justeli.coins.command.WithdrawCommandPaper;
import me.justeli.coins.command.WithdrawCommandSpigot;
import me.justeli.coins.config.Config;
import me.justeli.coins.handler.HopperHandler;
import me.justeli.coins.handler.InventoryHandler;
import me.justeli.coins.handler.InteractionHandler;
import me.justeli.coins.handler.ModificationHandler;
import me.justeli.coins.handler.UnfairMobHandler;
import me.justeli.coins.handler.listener.SpigotEventListener;
import me.justeli.coins.handler.PickupHandler;
import me.justeli.coins.handler.DropHandler;
import me.justeli.coins.handler.listener.PaperEventListener;
import me.justeli.coins.hook.bstats.Metrics;
import me.justeli.coins.config.Settings;
import me.justeli.coins.hook.mythicmobs.MythicMobsHook;
import me.justeli.coins.item.BaseCoin;
import me.justeli.coins.hook.Economies;
import me.justeli.coins.item.CoinMeta;
import me.justeli.coins.item.CreateCoin;
import me.justeli.coins.item.MetaBuilder;
import me.justeli.coins.util.Messenger;
import me.justeli.coins.util.VersionCheck;
import me.justeli.coins.util.Scheduler;
import me.justeli.coins.util.VersionUtil;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * @author Eli
 * @since December 13, 2016 (creation)
 */
public final class Coins extends JavaPlugin {
    public static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void onEnable() {
        long startMillis = System.currentTimeMillis();
        if (VersionUtil.getMinecraftVersion() < 21) {
            disablePlugin("Coins only supports Minecraft version 1.21 and newer.");
        }

        if (VersionUtil.getPlatform() == VersionUtil.Platform.BUKKIT) {
            disablePlugin("""
                You seem to be using Bukkit, but the plugin Coins requires at least Spigot! Please use Spigot, Paper, \
                or Folia. Moving from Bukkit to Spigot will NOT cause any problems with other plugins, since Spigot \
                only adds more features to Bukkit.
                """
            );
        }

        // basic functionality
        this.scheduler = new Scheduler(this);
        this.messenger = new Messenger(this);

        // economy provider
        this.economy = new Economies(this);
        if (!economy.getMissingPluginNames().isEmpty()) {
            for (var missing : economy.getMissingPluginNames()) {
                disablePlugin("There is no proper economy installed. Please install %s.".formatted(missing));
            }
        }

        // plugin version checker and metrics
        this.versionCheck = new VersionCheck(this);
        EXECUTOR.submit(() -> versionCheck.checkVersion());

        var metrics = new Metrics(this);

        // show disabled reasons if plugin can't function
        if (!disabledReasons.isEmpty()) {
            if (VersionUtil.isPlatformAtLeast(VersionUtil.Platform.PAPER)) {
                new DisabledCommandPaper(this);
            }
            else {
                new DisabledCommandSpigot(this);
            }

            line(Level.SEVERE);
            console(Level.SEVERE, "Plugin 'Coins' is now disabled, until the issues above are resolved.");
            line(Level.SEVERE);

            EXECUTOR.submit(() -> metrics.register(true));
            return;
        }

        // mythicmobs integration
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            try {
                if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
                    new MythicMobsHook(this);
                }
            }
            catch (Exception | NoClassDefFoundError | InstantiationError exception) {
                console(Level.WARNING, """
                    Detected MythicMobs, but the version of MythicMobs you are using \
                    is not supported. If this is a newer version, please contact \
                    support of Coins: https://plugin.coins.community/discord
                    """
                );
            }
        }

        // initialize everything if there are no errors
        this.settings = new Settings(this);
        this.baseCoin = new BaseCoin(this);
        this.coinMeta = new CoinMeta(this);
        this.createCoin = new CreateCoin(this);

        // register events
        this.unfairMobHandler = new UnfairMobHandler(this);
        this.pickupHandler = new PickupHandler(this);

        if (VersionUtil.isPlatformAtLeast(VersionUtil.Platform.PAPER)) {
            new PaperEventListener(this);
        }
        else {
            new SpigotEventListener(this);
        }

        new HopperHandler(this);
        new DropHandler(this);
        new InteractionHandler(this);
        new InventoryHandler(this);
        new ModificationHandler(this);

        // register commands
        if (VersionUtil.isPlatformAtLeast(VersionUtil.Platform.PAPER)) {
            new CoinsCommandPaper(this);
            new WithdrawCommandPaper(this);
        }
        else {
            new CoinsCommandSpigot(this);
            new WithdrawCommandSpigot(this);
        }

        EXECUTOR.submit(() -> metrics.register(false));
        console(Level.INFO, "Initialized in %,dms.".formatted(System.currentTimeMillis() - startMillis));
    }

    public void parseEventHandlers(@NotNull Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    public void line(Level type) {
        console(type, "------------------------------------------------------------------");
    }

    public void console(Level type, String message) {
        getLogger().log(type, message);
    }

    public void debug(String message) {
        if (Config.DEBUG_LOGGING) {
            getLogger().log(Level.WARNING, "(Debug @ %d) ".formatted(System.currentTimeMillis()) + message);
        }
    }

    // plugin disablement

    private final List<String> disabledReasons = new ArrayList<>();

    public List<String> getDisabledReasons() {
        return disabledReasons;
    }

    private void disablePlugin(String reason) {
        line(Level.SEVERE);
        console(Level.SEVERE, reason);
        disabledReasons.add(reason);
    }

    private boolean pluginDisabled = false;

    public boolean isDisabled() {
        return pluginDisabled;
    }

    public boolean toggleDisabled() {
        this.pluginDisabled = !pluginDisabled;
        return !pluginDisabled;
    }

    // getters of classes

    private Messenger messenger;
    public Messenger getMessenger() {
        return messenger;
    }

    private Scheduler scheduler;
    public Scheduler getScheduler() {
        return scheduler;
    }

    private Economies economy;
    public Economies getEconomy() {
        return economy;
    }

    private BaseCoin baseCoin;
    public BaseCoin getBaseCoin() {
        return baseCoin;
    }

    private Settings settings;
    public Settings getSettings() {
        return settings;
    }

    private CreateCoin createCoin;
    public CreateCoin getCreateCoin() {
        return createCoin;
    }

    private CoinMeta coinMeta;
    public CoinMeta getCoinMeta() {
        return coinMeta;
    }

    private VersionCheck versionCheck;
    public VersionCheck getVersionCheck() {
        return versionCheck;
    }

    private PickupHandler pickupHandler;
    public PickupHandler getPickupHandler() {
        return pickupHandler;
    }

    private UnfairMobHandler unfairMobHandler;
    public UnfairMobHandler getUnfairMobHandler() {
        return unfairMobHandler;
    }

    // getters from other places

    public MetaBuilder meta(ItemStack itemStack) {
        return new MetaBuilder(this, itemStack);
    }
}
