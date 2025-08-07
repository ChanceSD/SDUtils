package me.chancesd.sdutils.display.chat.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.chancesd.sdutils.display.chat.ChatLine;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * A simple content provider that returns a fixed list of chat lines
 */
public class StaticContentProvider implements ContentProvider {
    private final List<ChatLine> lines;

    public StaticContentProvider() {
        this.lines = new ArrayList<>();
    }

    public StaticContentProvider(final List<ChatLine> lines) {
        this.lines = new ArrayList<>(lines);
    }

    public void addLine(@NotNull final ChatLine line) {
        this.lines.add(line);
    }

    public void addLine(@NotNull final String text) {
		this.lines.add(new ChatLine(text));
    }

    public void addLine(@NotNull final String text, final String command, final String hoverText) {
		this.lines.add(new ChatLine(text,
                command != null ? ChatLine.ClickAction.runCommand(command) : null,
                hoverText != null ? ChatLine.HoverAction.showText(hoverText) : null));
    }

    public void clear() {
        this.lines.clear();
    }

    @NotNull
    @Override
    public List<ChatLine> getContent(final CommandSender sender) {
        return Collections.unmodifiableList(lines);
    }
}
