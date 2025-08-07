package me.chancesd.sdutils.display.chat;

import org.jetbrains.annotations.NotNull;

/**
 * Configuration for navigation buttons in a paginated chat menu.
 * 
 * <p>Supports modern page number navigation (e.g., "/command 1", "/command 2").</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * NavigationButtons nav = NavigationButtons.builder()
 *     .navigationPrefix("/menu")  // Creates commands like "/menu 1", "/menu 2"
 *     .previousText("&7[&c← Prev&7]")
 *     .nextText("&7[&cNext →&7]")
 *     .build();
 * </pre>
 */
public class NavigationButtons {
    private final String previousText;
    private final String nextText;
    private final String navigationPrefix;

    private NavigationButtons(String previousText, String nextText, String navigationPrefix) {
        this.previousText = previousText;
        this.nextText = nextText;
        this.navigationPrefix = navigationPrefix;
    }

    /**
     * Get the prefix used for navigation commands
     */
    @NotNull
    public String getNavigationPrefix() {
        return navigationPrefix;
    }

    /**
     * Generate a previous page button for the given page number
     */
    public String generatePrevious(int previousPage) {
        return previousText.replace("{page}", String.valueOf(previousPage));
    }

    /**
     * Generate a next page button for the given page number
     */
    public String generateNext(int nextPage) {
        return nextText.replace("{page}", String.valueOf(nextPage));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String previousText = "&7[&c← Prev&7]";
        private String nextText = "&7[&cNext →&7]";
        private String navigationPrefix = "/chatmenu";

        public Builder previousText(String text) {
            this.previousText = text;
            return this;
        }

        public Builder nextText(String text) {
            this.nextText = text;
            return this;
        }

        public Builder navigationPrefix(String prefix) {
            this.navigationPrefix = prefix;
            return this;
        }

        public NavigationButtons build() {
            return new NavigationButtons(previousText, nextText, navigationPrefix);
        }
    }
}
