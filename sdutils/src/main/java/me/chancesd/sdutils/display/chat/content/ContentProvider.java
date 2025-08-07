package me.chancesd.sdutils.display.chat.content;

import java.util.List;

import me.chancesd.sdutils.display.chat.ChatLine;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for providing content to a chat menu.
 */
public interface ContentProvider {
    /**
     * Get the current content for this provider
     *
     * @param sender The sender to get content for
     * @return List of chat lines to display
     */
    @NotNull
    List<ChatLine> getContent(final CommandSender sender);
}
