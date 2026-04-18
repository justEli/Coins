package me.justeli.coins.hook.mythicmobs;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.core.drops.droppables.VanillaItemDrop;
import me.justeli.coins.Coins;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;

/**
 * works for MythicMobs 5.6 and up
 * @author Eli
 */
public final class MythicMobsHook implements MMHook {
    private final Coins coins;

    // todo move mythic mobs hook to separate plugin
    public MythicMobsHook(Coins coins) {
        this.coins = coins;
        coins.parseEventHandlers(this);
    }

    private static final BukkitAPIHelper BUKKIT_API_HELPER = new BukkitAPIHelper();

    @Override
    public boolean isMythicMob(Entity entity) {
        return BUKKIT_API_HELPER.isMythicMob(entity);
    }

    @EventHandler
    void onMythicDropLoadEvent(MythicDropLoadEvent event) {
        if (!event.getDropName().equalsIgnoreCase("coins")) {
            return;
        }

        VanillaItemDrop drop = new VanillaItemDrop(
            event.getConfig().getLine(),
            event.getConfig(),
            new BukkitItemStack(coins.getCreateCoin().createDropped())
        );

        event.register(drop);
    }
}
