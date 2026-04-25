package me.justeli.coins.item;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.util.SkullUtil;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.logging.Level;

/**
 * @author Eli
 * @since January 30, 2022 (creation)
 */
public final class BaseCoin {
    private final Coins coins;
    public BaseCoin(Coins coins) {
        this.coins = coins;
        reload();
    }

    private MetaBuilder withdrawnCoin;
    private MetaBuilder droppedCoin;
    private MetaBuilder otherCoin;

    public void reload() {
        ItemStack baseCoin;
        if (Config.SKULL_TEXTURE == null || Config.SKULL_TEXTURE.isEmpty()) {
            baseCoin = new ItemStack(Config.COIN_ITEM);
        }
        else {
            baseCoin = Objects.requireNonNullElseGet(
                SkullUtil.fromTexture(Config.SKULL_TEXTURE),
                () -> new ItemStack(Config.COIN_ITEM)
            );
        }

        var baseCoinMeta = baseCoin.getItemMeta();
        if (baseCoinMeta != null) {
            if (Config.CUSTOM_MODEL_DATA > 0) {
                baseCoinMeta.setCustomModelData(Config.CUSTOM_MODEL_DATA);
            }

            if (Config.ENCHANTED_COIN) {
                baseCoinMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                baseCoinMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            baseCoin.setItemMeta(baseCoinMeta);
        }

        this.withdrawnCoin = coins.meta(baseCoin.clone())
            .setData(CoinMeta.COINS_TYPE, CoinMeta.TYPE_WITHDRAWN);

        MetaBuilder droppedCoinItem = coins.meta(baseCoin.clone())
            .setName(Config.DROPPED_COIN_NAME)
            .setData(CoinMeta.COINS_TYPE, CoinMeta.TYPE_DROPPED);

        if (Config.DROP_EACH_COIN) {
            droppedCoinItem.setData(CoinMeta.COINS_WORTH, 1D);
        }

        this.droppedCoin = droppedCoinItem;
        this.otherCoin = coins.meta(baseCoin.clone())
            .setName(Config.DROPPED_COIN_NAME)
            .setData(CoinMeta.COINS_TYPE, CoinMeta.TYPE_OTHER);

        coins.console(Level.INFO, "Configured coin types have been loaded.");
    }

    public MetaBuilder cloneBaseDropped() {
        return droppedCoin.clone();
    }

    public MetaBuilder cloneBaseWithdrawn() {
        return withdrawnCoin.clone();
    }

    public MetaBuilder cloneBaseOther() {
        return otherCoin.clone();
    }
}
