package me.justeli.coins.hook;

import me.justeli.coins.hook.vault.VaultEconomyHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * @author Eli
 * @since February 01, 2022 (creation)
 */
public final class Economies implements EconomyHook {
    private final Plugin plugin;
    private final Set<String> missingPlugins = new LinkedHashSet<>();
    private final Set<String> supportedHooks = new LinkedHashSet<>();

    private EconomyHook economy = null;

    public Economies(Plugin plugin) {
        this.plugin = plugin;

        hookIfInstalled(VaultEconomyHook.VAULT, () ->
            Optional.ofNullable(plugin.getServer().getServicesManager().getRegistration(Economy.class))
                .map(registration -> new VaultEconomyHook(plugin, registration.getProvider()))
        );

        if (economy == null && missingPlugins.isEmpty()) {
            missingPlugins.add("'" + String.join("' or '", supportedHooks) + "'");
        }
    }

    private void hookIfInstalled(String name, Supplier<Optional<EconomyHook>> hooker) {
        supportedHooks.add(name);
        if (economy != null) {
            return; // already hooked
        }

        if (!plugin.getServer().getPluginManager().isPluginEnabled(name)) {
            return;
        }

        try {
            this.economy = hooker.get().orElse(null);
        }
        catch (NullPointerException | NoClassDefFoundError ignored) {}

        if (economy == null) {
            missingPlugins.add("an economy providing plugin for '" + name + "'");
        }
        else {
            plugin.getLogger().log(Level.INFO, "Using '%s' as the economy provider.".formatted(name));
            missingPlugins.clear();
        }
    }

    public Set<String> getMissingPluginNames() {
        return missingPlugins;
    }

    @Override
    public void balance(UUID uuid, DoubleConsumer balance) {
        if (economy != null) {
            economy.balance(uuid, balance);
        }
    }

    @Override
    public void canAfford(UUID uuid, double amount, Consumer<Boolean> canAfford) {
        if (economy != null) {
            economy.canAfford(uuid, amount, canAfford);
        }
    }

    @Override
    public void withdraw(UUID uuid, double amount, Runnable success) {
        if (economy != null) {
            economy.withdraw(uuid, amount, success);
        }
    }

    @Override
    public void deposit(UUID uuid, double amount, Runnable success) {
        if (economy != null) {
            economy.deposit(uuid, amount, success);
        }
    }

    @Override
    public Optional<String> name() {
        if (economy == null) {
            return Optional.empty();
        }

        return economy.name();
    }
}
