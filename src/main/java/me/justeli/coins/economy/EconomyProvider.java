package me.justeli.coins.economy;

import me.justeli.coins.Coins;

/**
 * @author Eli
 * @since April 26, 2026
 */
public final class EconomyProvider {
    // todo
    //  coins.getServer().getServicesManager().register(Economy.class, economy, coins, ServicePriority.Highest);
    //  https://github.com/survival-rocks/survival.rocks-v1-modules/blob/map-reset/plugins/co-storage/src/main/java/me/justeli/common/storage

    private final Coins coins;
    public EconomyProvider(Coins coins) {
        this.coins = coins;

        this.economy = new CoinsEconomy(coins, this);
        this.storage = new BalanceStorage(coins, this);
    }

    private final CoinsEconomy economy;

    public CoinsEconomy getEconomy() {
        return economy;
    }

    private final BalanceStorage storage;

    public BalanceStorage getStorage() {
        return storage;
    }
}
