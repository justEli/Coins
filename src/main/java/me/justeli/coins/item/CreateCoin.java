package me.justeli.coins.item;

import me.justeli.coins.Coins;
import me.justeli.coins.component.ComponentUtil;
import me.justeli.coins.config.Config;
import org.bukkit.inventory.ItemStack;

import java.util.SplittableRandom;

/**
 * @author Eli
 * @since January 30, 2022 (creation)
 */
public final class CreateCoin {
    private final Coins coins;
    public CreateCoin(Coins coins) {
        this.coins = coins;
    }

    private static final SplittableRandom RANDOM = new SplittableRandom();

    /// @since 1.14.1
    public ItemStack createWithdrawn(double worth) {
        var plural = worth == 1? Config.WITHDRAWN_COIN_NAME_SINGULAR : Config.WITHDRAWN_COIN_NAME_PLURAL;
        return coins.getBaseCoin().cloneBaseWithdrawn()
            .setData(CoinMeta.COINS_WORTH, worth)
            .setName(ComponentUtil.replaceAmount(plural, worth)).build();
    }

    private MetaBuilder createDropBuilder() {
        MetaBuilder coin = coins.getBaseCoin().cloneBaseDropped();
        if (Config.DROP_EACH_COIN || !Config.STACK_COINS) {
            return coin.setData(CoinMeta.COINS_RANDOM, RANDOM.nextInt());
        }
        return coin;
    }

    /// @since 1.14.1
    public ItemStack createDropped() {
        return createDropBuilder().build();
    }

    /// @since 1.14.1
    public ItemStack createDropped(double increment) {
        if (increment == 1) {
            return createDropped();
        }

        MetaBuilder coin = createDropBuilder()
            .setData(CoinMeta.COINS_INCREMENT, increment);

        return coin.build();
    }

    /// @since 1.14.1
    public MetaBuilder createOther() {
        return coins.getBaseCoin().cloneBaseOther();
    }

    // other plugins still use these, i.e. LM_Items

    /// @see #createDropped()
    @Deprecated(forRemoval = true)
    public ItemStack dropped() {
        return createDropped();
    }

    /// @see #createDropped(double)
    @Deprecated(forRemoval = true)
    public ItemStack dropped(double increment) {
        return createDropped(increment);
    }
}
