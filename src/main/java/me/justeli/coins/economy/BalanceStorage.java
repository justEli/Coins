package me.justeli.coins.economy;

import me.justeli.coins.Coins;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Eli
 * @since April 26, 2026
 */
public final class BalanceStorage {
    private final Coins coins;
    private final EconomyProvider provider;

    public BalanceStorage(Coins coins, EconomyProvider provider) {
        this.coins = coins;
        this.provider = provider;

        Path balances = coins.getDataFolder().toPath().resolve("balances");
        try { Files.createDirectories(balances); }
        catch (IOException ignored) {}
    }

    private final Map<String, UUID> usernameToUuids = new ConcurrentHashMap<>();
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();

    private static final ExecutorService SINGLE_EXECUTOR = Executors.newSingleThreadExecutor();

    public Optional<OfflinePlayer> getOfflinePlayer(String username) {
        if (username == null) {
            return Optional.empty();
        }

        UUID uuid = usernameToUuids.get(username.toLowerCase());
        if (uuid == null) {
            return Optional.empty();
        }

        return Optional.of(coins.getServer().getOfflinePlayer(uuid));
    }

    public double getBalance(UUID uuid) {
        if (uuid == null) {
            return 0D;
        }

        return balances.computeIfAbsent(uuid, empty -> 0D);
    }

    public boolean canAfford(UUID uuid, double amount) {
        if (uuid == null) {
            return false;
        }

        return amount >= 0 && getBalance(uuid) >= amount;
    }

    public void withdraw(UUID uuid, double amount) {
        if (uuid == null) {
            return;
        }

        balances.put(uuid, getBalance(uuid) - amount);
        // todo storage
    }

    public void deposit(UUID uuid, double amount) {
        if (uuid == null) {
            return;
        }

        balances.put(uuid, getBalance(uuid) + amount);
        // todo storage
    }
}
