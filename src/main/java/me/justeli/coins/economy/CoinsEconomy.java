package me.justeli.coins.economy;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.util.Util;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.List;

/**
 * @author Eli
 * @since April 26, 2026
 */
public final class CoinsEconomy implements Economy {
    private final Coins coins;
    private final EconomyProvider provider;

    public CoinsEconomy(Coins coins, EconomyProvider provider) {
        this.coins = coins;
        this.provider = provider;
    }

    private static final EconomyResponse USERNAME_NOT_FOUND =
        new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "No account found by username");

    private static final EconomyResponse BANKS_UNSUPPORTED =
        new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks are not supported");

    @Override
    public boolean isEnabled() {
        return coins.isEnabled();
    }

    @Override
    public String getName() {
        return "Coins";
    }

    @Override
    public int fractionalDigits() {
        return Config.MONEY_DECIMALS;
    }

    @Override
    public String format(double amount) {
        return Util.toFormattedMoneyDecimals(amount);
    }

    @Override
    public String currencyNamePlural() {
        return getName();
    }

    @Override
    public String currencyNameSingular() {
        return getName();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public double getBalance(String playerName) {
        return provider.getStorage().getOfflinePlayer(playerName)
            .map(this::getBalance)
            .orElse(0D);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return provider.getStorage().getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return provider.getStorage().getOfflinePlayer(playerName)
            .map(player -> has(player, amount))
            .orElse(false);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return provider.getStorage().canAfford(player.getUniqueId(), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return provider.getStorage().getOfflinePlayer(playerName)
            .map(player -> withdrawPlayer(player, amount))
            .orElse(USERNAME_NOT_FOUND);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        provider.getStorage().withdraw(player.getUniqueId(), amount);
        return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return provider.getStorage().getOfflinePlayer(playerName)
            .map(player -> depositPlayer(player, amount))
            .orElse(USERNAME_NOT_FOUND);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        provider.getStorage().deposit(player.getUniqueId(), amount);
        return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return BANKS_UNSUPPORTED;
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }
}
