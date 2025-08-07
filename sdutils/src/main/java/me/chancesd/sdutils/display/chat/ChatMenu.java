package me.chancesd.sdutils.display.chat;

import me.chancesd.sdutils.display.chat.content.ContentProvider;
import me.chancesd.sdutils.display.chat.content.StaticContentProvider;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A flexible and easy-to-use chat menu system for Bukkit plugins with support for pagination, clickable
 * lines, and dynamic content.
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * ChatMenu menu = ChatMenu.builder()
 * 		.header("&6=== Plugin Help &7(Page {page}/{total}) &6===")
 * 		.footer("&6=====================================")
 * 		.linesPerPage(8)
 * 		.contentProvider(new StaticContentProvider() {
 * 			{
 * 				addLine("&a/command1 &7- Description", "/command1", "Click to execute");
 * 				addLine("&a/command2 &7- Description", "/command2", "Click to execute");
 * 			}
 * 		})
 * 		.navigation(NavigationButtons.builder()
 * 				.navigationPrefix("/menu")
 * 				.build())
 * 		.build();
 *
 * // Show menu to player
 * menu.show(player);
 *
 * // Handle page navigation in command
 * if (args.length >= 1) {
 * 	try {
 * 		int page = Integer.parseInt(args[0]);
 * 		menu.show(player, page); // Direct page navigation: /menu 1, /menu 2, etc.
 * 	} catch (NumberFormatException e) {
 * 		// Invalid page number
 * 		player.sendMessage("Invalid page number!");
 * 	}
 * }
 * </pre>
 *
 * <p>
 * Available placeholders in header/footer:
 * </p>
 * <ul>
 * <li>{@code {page}} - Current page number</li>
 * <li>{@code {total}} - Total pages</li>
 * <li>{@code {player}} - Player name</li>
 * </ul>
 *
 * <p>
 * Navigation Features:
 * </p>
 * <ul>
 * <li>Direct page access: {@code /command 1}, {@code /command 2}, etc.</li>
 * <li>Clickable navigation buttons with page numbers</li>
 * <li>Footer always displays alongside navigation</li>
 * <li>Compact single-line navigation layout</li>
 * </ul>
 */
public class ChatMenu {
    private final String header;
    private final String footer;
    private final int linesPerPage;
    private final NavigationButtons navigation;
    private final ContentProvider contentProvider;

    private ChatMenu(final Builder builder) {
        this.header = builder.header;
        this.footer = builder.footer;
        this.linesPerPage = builder.linesPerPage;
        this.navigation = builder.navigation;
        this.contentProvider = builder.contentProvider != null ? builder.contentProvider : new StaticContentProvider();
    }

    /**
	 * Shows the menu to a command sender starting from page 1
	 *
	 * @param sender The command sender to show the menu to
	 */
    public void show(@NotNull final CommandSender sender) {
        show(sender, 1);
    }

    /**
	 * Shows a specific page of the menu to a command sender
	 *
	 * @param sender The command sender to show the menu to
	 * @param page   The page number to show
	 */
    public void show(@NotNull final CommandSender sender, final int page) {
        // Create temporary session just for this display, don't save it
        final MenuSession session = new MenuSession(sender, this);
        session.showPage(page);
    }

    @NotNull
    public List<ChatLine> getContent(@NotNull final CommandSender sender) {
        return contentProvider.getContent(sender);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public String getHeader() {
        return header;
    }

    @Nullable
    public String getFooter() {
        return footer;
    }

    public int getLinesPerPage() {
        return linesPerPage;
    }

    @Nullable
    public NavigationButtons getNavigation() {
        return navigation;
    }

    @NotNull
    public ContentProvider getContentProvider() {
        return contentProvider;
    }

    public static class Builder {
        private String header;
        private String footer;
        private int linesPerPage = 10;
        private NavigationButtons navigation;
        private ContentProvider contentProvider;

        /**
         * Sets the menu header with optional placeholders
         * @param header The header text with color codes
         */
        public Builder header(final String header) {
			this.header = header;
            return this;
        }

        /**
         * Sets the menu footer with optional placeholders
         * @param footer The footer text with color codes
         */
        public Builder footer(final String footer) {
			this.footer = footer;
            return this;
        }

        /**
         * Sets how many content lines to show per page
         * @param linesPerPage Number of lines
         */
        public Builder linesPerPage(final int linesPerPage) {
            this.linesPerPage = linesPerPage;
            return this;
        }

        /**
         * Sets the navigation button configuration
         * @param navigation The navigation buttons setup
         */
        public Builder navigation(final NavigationButtons navigation) {
            this.navigation = navigation;
            return this;
        }

        /**
         * Sets a custom content provider for dynamic content
         * @param provider The content provider implementation
         */
        public Builder contentProvider(final ContentProvider provider) {
            this.contentProvider = provider;
            return this;
        }

        public ChatMenu build() {
            return new ChatMenu(this);
        }
    }
}
