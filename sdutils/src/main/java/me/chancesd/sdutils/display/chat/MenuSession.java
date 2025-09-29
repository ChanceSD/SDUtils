package me.chancesd.sdutils.display.chat;

import me.chancesd.sdutils.utils.ChatUtils;
import me.chancesd.sdutils.utils.MCVersion;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a command sender's session with a chat menu
 */
public class MenuSession {
    private final CommandSender sender;
    private final ChatMenu menu;

    public MenuSession(@NotNull final CommandSender sender, @NotNull final ChatMenu menu) {
        this.sender = sender;
        this.menu = menu;
    }

    /**
     * Send a ChatLine to the command sender, using appropriate method for Player vs Console
     */
    private void sendMessage(final ChatLine chatLine) {
		if (sender instanceof final Player player && MCVersion.isAtLeast(MCVersion.V1_16_5)) {
			if (MCVersion.isAtLeast(MCVersion.V1_20_4)) {
				player.spigot().sendMessage(chatLine.toComponent());
			} else {
				player.spigot().sendMessage(chatLine.toComponentArray());
			}
        } else {
            sender.sendMessage(ChatUtils.colorize(chatLine.getPlainText()));
        }
    }

    /**
     * Send an empty line
     */
    private void sendEmptyLine() {
		if (sender instanceof final Player player) {
			player.spigot().sendMessage();
        } else {
            sender.sendMessage("");
        }
    }

    public void showPage(final int page) {
        final List<ChatLine> content = menu.getContent(sender);
        final int totalPages = calculateTotalPages(content.size());

        if (page < 1 || page > totalPages) {
            return;
        }

        final int startIndex = (page - 1) * menu.getLinesPerPage();
        final int endIndex = Math.min(startIndex + menu.getLinesPerPage(), content.size());

        // Send header
        if (menu.getHeader() != null) {
            final String header = replacePlaceholders(menu.getHeader(), page, totalPages);
            sendMessage(new ChatLine(header));
            sendEmptyLine();
        }

        // Send content lines
        for (int i = startIndex; i < endIndex; i++) {
            sendMessage(content.get(i));
        }

        // Send empty line for spacing
        sendEmptyLine();

        // Send footer with navigation if paginated
        if (menu.getNavigation() != null && totalPages > 1) {
            final ChatLine navigationLine = buildNavigationLine(page, totalPages);
            sendMessage(navigationLine);
        }

        // Always send footer if present
        if (menu.getFooter() != null) {
            final String footer = replacePlaceholders(menu.getFooter(), page, totalPages);
            sendMessage(new ChatLine(footer));
        }
    }

    private int calculateTotalPages(final int contentSize) {
        return (int) Math.ceil((double) contentSize / menu.getLinesPerPage());
    }

    private String replacePlaceholders(final String text, final int page, final int totalPages) {
        final String playerName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        return ChatUtils.colorize(text.replace("{page}", String.valueOf(page))
                .replace("{total}", String.valueOf(totalPages))
                .replace("{player}", playerName));
    }

    private ChatLine buildNavigationLine(final int currentPage, final int totalPages) {
        final NavigationButtons nav = menu.getNavigation();

        if (nav == null) {
            return new ChatLine("");
        }

        // Use the new ChatLine builder for multi-component navigation
        final ChatLine.Builder builder = ChatLine.builder();

        // Add previous page button if available
        if (currentPage > 1) {
            final String prevText = ChatUtils.colorize(nav.generatePrevious(currentPage - 1));
            final String prevCommand = nav.getNavigationPrefix() + " " + (currentPage - 1);
            final String prevHover = ChatUtils.colorize("&7Click to go to page " + (currentPage - 1));

            builder.addClickable(prevText, ChatLine.ClickAction.runCommand(prevCommand), prevHover);
        }

        // Add spacing if we have previous button
        if (currentPage > 1) {
            builder.add("  ");
        }

        // Add page indicator (non-clickable)
        final String pageIndicator = ChatUtils.colorize("&7Page " + currentPage + "/" + totalPages);
        builder.add(pageIndicator);

        // Add spacing if we have next button
        if (currentPage < totalPages) {
            builder.add("  ");
        }

        // Add next page button if available
        if (currentPage < totalPages) {
            final String nextText = ChatUtils.colorize(nav.generateNext(currentPage + 1));
            final String nextCommand = nav.getNavigationPrefix() + " " + (currentPage + 1);
            final String nextHover = ChatUtils.colorize("&7Click to go to page " + (currentPage + 1));

            builder.addClickable(nextText, ChatLine.ClickAction.runCommand(nextCommand), nextHover);
        }

        return builder.build();
    }

    public CommandSender getSender() {
        return sender;
    }

}
