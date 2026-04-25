package me.justeli.coins.util;

import me.justeli.coins.Coins;
import me.justeli.coins.config.MessagePosition;
import me.justeli.coins.language.FormatEntry;
import me.justeli.coins.language.Language;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eli
 * @since April 21, 2026
 */
public final class Messenger {
    private final Coins coins;
    private MessageAudience audience;

    public Messenger(Coins coins) {
        this.coins = coins;
        if (!VersionUtil.isPlatformAtLeast(VersionUtil.Platform.PAPER)) {
            this.audience = new MessageAudience();
        }
    }

    // formatting example
    // final Component broadcastMessage = MiniMessage.miniMessage().deserialize(
    //    "<red><bold>BROADCAST</red> <name> <dark_gray>»</dark_gray> <message>",
    //    Placeholder.component("name", name),      // i.e. currency symbol
    //    Placeholder.unparsed("message", message)  // i.e. amount of money
    // );

    private static final TextComponent EMPTY = Component.empty();
    private static final Title.Times TITLE_DURATION = Title.Times.times(
        Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)
    );

    public void sendMessage(CommandSender sender, Component component) {
        sendMessage(sender, MessagePosition.CHAT, component);
    }

    public void sendMessage(CommandSender sender, FormatEntry entry) {
        sendMessage(sender, entry.getComponent());
    }

    public void sendMessage(CommandSender sender, MessagePosition position, Component component) {
        Audience audience = getAudience(sender);
        switch (position) {
            case CHAT -> audience.sendMessage(component);
            case ACTIONBAR -> audience.sendActionBar(component);
            case TITLE -> audience.showTitle(Title.title(component, EMPTY, TITLE_DURATION));
            case SUBTITLE -> audience.showTitle(Title.title(EMPTY, component, TITLE_DURATION));
        }
    }

    private Audience getAudience(CommandSender sender) {
        if (VersionUtil.isPlatformAtLeast(VersionUtil.Platform.PAPER)) {
            return sender;
        }

        return audience.sender(sender);
    }

    private static final int PAGE_SIZE = 8;
    private static final Sound PAGE_TURN = Sound.ITEM_BOOK_PAGE_TURN;
    private static final Component HEADER_LINE =
        Component.text("     ", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH);

    public Component getHeader(@Nullable String title) {
        var builder = Component.text()
            .append(HEADER_LINE)
            .appendSpace()
            .append(ComponentUtil.BRAND_COMPONENT)
            .appendSpace()
            .append(Component.text(coins.getVersionCheck().getPluginVersion(), ColorResolver.COINS));

        if (title != null) {
            builder.appendSpace().append(Component.text(title, ColorResolver.PRIMARY));
        }

        return builder.appendSpace().append(HEADER_LINE).build();
    }

    public void sendHeader(CommandSender sender, @Nullable String title) {
        sendMessage(sender, getHeader(title));
    }

    private static List<Component> getPage(List<Component> allLines, int number) {
        if (number <= 0) {
            return List.of();
        }

        List<Component> pages = new ArrayList<>();
        for (int i = (number - 1) * PAGE_SIZE; i < number * PAGE_SIZE; i++) {
            if (allLines.size() <= i) {
                break;
            }

            pages.add(allLines.get(i));
        }

        return pages;
    }

    /// @param pageNumber starts at 1
    /// @param command has to start with a slash
    public void sendPage(CommandSender sender, List<Component> allLines, int pageNumber, @Nullable String title, String command) {
        if (pageNumber <= 0 || allLines.isEmpty()) {
            sendMessage(sender, Language.PAGE_NOT_FOUND);
            return;
        }

        List<Component> pageLines = getPage(allLines, pageNumber);
        if (pageLines.isEmpty()) {
            sendMessage(sender, Language.PAGE_NOT_FOUND);
            return;
        }

        TextComponent.Builder component = Component.text()
            .append(getHeader(title));

        Component page = Language.WORD_PAGE.getCapitalizedComponent();
        int previousPage = pageNumber - 1;
        if (previousPage > 0) {
            component.appendSpace().append(
                Component.text("««", ColorResolver.USER)
                    .hoverEvent(HoverEvent.showText(
                        Component.text().append(page).appendSpace().append(Component.text(previousPage)).color(ColorResolver.VAR)
                    ))
                    .clickEvent(ClickEvent.runCommand(command + " " + previousPage))
            );
        }

        int totalPages = allLines.size() / PAGE_SIZE + Math.min(allLines.size() % PAGE_SIZE, 1);
        component.appendSpace().append(
            Component.empty()
                .append(Component.text(pageNumber, NamedTextColor.GRAY))
                .append(Component.text("/", NamedTextColor.DARK_GRAY))
                .append(Component.text(totalPages, NamedTextColor.GRAY))
        );

        int nextPage = pageNumber + 1;
        if (nextPage <= totalPages) {
            component.appendSpace().append(
                Component.text("»»", ColorResolver.USER)
                    .hoverEvent(HoverEvent.showText(
                        Component.text().append(page).appendSpace().append(Component.text(nextPage)).color(ColorResolver.VAR)
                    ))
                    .clickEvent(ClickEvent.runCommand("%s %d".formatted(command, nextPage)))
            );
        }

        for (Component line : pageLines) {
            component.appendNewline();
            component.append(line);
        }

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), PAGE_TURN, 1F, 1F);
        }

        sendMessage(sender, component.build());
    }

    // to prevent BukkitAudiences being a field in upper class, causes problems on Paper
    private class MessageAudience {
        private final BukkitAudiences audiences;
        public MessageAudience() {
            this.audiences = BukkitAudiences.create(coins);
        }

        public Audience sender(CommandSender sender) {
            return audiences.sender(sender);
        }
    }
}
