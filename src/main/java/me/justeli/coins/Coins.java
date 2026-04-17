package me.justeli.coins;

import me.justeli.coins.command.CoinsCommand;
import me.justeli.coins.command.DisabledCommand;
import me.justeli.coins.command.WithdrawCommand;
import me.justeli.coins.handler.ClickPickupHandler;
import me.justeli.coins.handler.HopperHandler;
import me.justeli.coins.handler.InventoryHandler;
import me.justeli.coins.handler.InteractionHandler;
import me.justeli.coins.handler.ModificationHandler;
import me.justeli.coins.handler.UnfairMobHandler;
import me.justeli.coins.handler.listener.SpigotEventListener;
import me.justeli.coins.handler.PickupHandler;
import me.justeli.coins.handler.DropHandler;
import me.justeli.coins.handler.listener.PaperEventListener;
import me.justeli.coins.hook.mythicmobs.MMHook;
import me.justeli.coins.hook.bstats.Metrics;
import me.justeli.coins.config.Settings;
import me.justeli.coins.hook.mythicmobs.MythicMobsHook;
import me.justeli.coins.item.BaseCoin;
import me.justeli.coins.hook.Economies;
import me.justeli.coins.item.CoinMeta;
import me.justeli.coins.item.CreateCoin;
import me.justeli.coins.item.MetaBuilder;
import me.justeli.coins.util.BlockDisplayManager; // -------------------- by AllFiRE
import me.justeli.coins.util.PluginVersion;
import me.justeli.coins.util.PluginVersionUtil;
import me.justeli.coins.util.ScheduleUtil;
import me.justeli.coins.util.VersionUtil;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * @author Eli
 * @since December 13, 2016 (creation)
 */
public final class Coins extends JavaPlugin {
    // TODO
    //  - fix:   does it still /ah dupe?
    //  - fix:   https://github.com/mofucraft/Coins/commit/1338a6f22fcd5db5c58aab58421a75bb09ef3d5c
    //  - fix:   custom model data for 1.20.4+

    private static final ExecutorService ASYNC_THREAD = Executors.newSingleThreadExecutor();

    private static final String UNSUPPORTED_VERSION =
        "Coins only supports Minecraft version 1.19 and newer.";

    private static final String USING_BUKKIT = """
        You seem to be using Bukkit, but the plugin Coins requires at least Spigot! Please use Spigot, \
        Paper, or Folia. Moving from Bukkit to Spigot will NOT cause any problems with other plugins, since \
        Spigot only adds more features to Bukkit.""";

    private static final String LACKING_ECONOMY =
        "There is no proper economy installed. Please install %s.";

    @Override
    public void onEnable() {
        long current = System.currentTimeMillis();
        Locale.setDefault(Locale.US);

        if (VersionUtil.getMinecraftVersion() < 19) {
            line(Level.SEVERE);
            console(Level.SEVERE, UNSUPPORTED_VERSION);
            disablePlugin(UNSUPPORTED_VERSION);
        }

        if (VersionUtil.getPlatform() == VersionUtil.Platform.BUKKIT) {
            line(Level.SEVERE);
            console(Level.SEVERE, USING_BUKKIT);
            disablePlugin(USING_BUKKIT);
        }

        this.scheduleUtil = new ScheduleUtil(this);
        this.economy = new Economies(this);

        if (!economy.getMissingPluginNames().isEmpty()) {
            line(Level.SEVERE);
            for (String missingPlugin : economy.getMissingPluginNames()) {
                String reason = String.format(LACKING_ECONOMY, missingPlugin);
                console(Level.SEVERE, reason);
                disablePlugin(reason);
            }
        }

        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            try {
                if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
                    this.mmHook = new MythicMobsHook(this);
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

        this.pluginVersionUtil = new PluginVersionUtil(this);
        if (disabledReasons.isEmpty()) {
            this.settings = new Settings(this);
            this.baseCoin = new BaseCoin(this);
            this.coinMeta = new CoinMeta(this);
            this.createCoin = new CreateCoin(this);

            // -------------------- by AllFiRE
            BlockDisplayManager.init(this);
            // -------------------- by AllFiRE

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
            new ClickPickupHandler(this); // -------------------- by AllFiRE

            // register commands
            new CoinsCommand(this);
            new WithdrawCommand(this);
        }
        else {
            new DisabledCommand(this);
            line(Level.SEVERE);
            console(Level.SEVERE, "Plugin 'Coins' is now disabled, until the issues are fixed.");
            line(Level.SEVERE);
        }

        ASYNC_THREAD.submit(() -> {
            pluginVersionUtil.checkVersion();
            new Metrics(this);
        });

        console(Level.INFO, "Initialized in %,dms.".formatted(System.currentTimeMillis() - current));
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

    // getters from other places

    private ScheduleUtil scheduleUtil;
    public ScheduleUtil getScheduler() {
        return scheduleUtil;
    }

    private Economies economy;
    public Economies getEconomy() {
        return economy;
    }

    public MetaBuilder meta(ItemStack itemStack) {
        return new MetaBuilder(this, itemStack);
    }

    private PluginVersionUtil pluginVersionUtil;
    public Optional<PluginVersion> getLatestVersion() {
        return pluginVersionUtil.getLatestVersion();
    }

    // plugin disablement

    private final List<String> disabledReasons = new ArrayList<>();

    public List<String> getDisabledReasons() {
        return disabledReasons;
    }

    private void disablePlugin(String reason) {
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

    private PickupHandler pickupHandler;
    public PickupHandler getPickupHandler() {
        return pickupHandler;
    }

    private UnfairMobHandler unfairMobHandler;
    public UnfairMobHandler getUnfairMobHandler() {
        return unfairMobHandler;
    }

    // hooks

    private MMHook mmHook;
    public Optional<MMHook> mmHook() {
        return Optional.ofNullable(mmHook);
    }
}
