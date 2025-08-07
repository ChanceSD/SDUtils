package me.chancesd.sdutils.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Chat utilities with PlaceholderAPI support and hex color support
 */
public class ChatUtils {

	private static final Pattern HEX_PATTERN = Pattern.compile("&?(#[a-fA-F0-9]{6})");
	private static final boolean USE_PLACEHOLDERAPI = checkForPlaceholderAPI();
	private static final boolean HEX_SUPPORTED = checkForBungeeAPI();

	private ChatUtils() {
	}

	private static boolean checkForPlaceholderAPI() {
		return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
	}

	private static boolean checkForBungeeAPI() {
		try {
			net.md_5.bungee.api.ChatColor.of("#FFFFFF");
		} catch (final NoClassDefFoundError | NoSuchMethodError e) {
			return false;
		}
		return true;
	}

	/**
	 * Process placeholders in a message for a specific player
	 *
	 * @param player  The player to process placeholders for
	 * @param message The message containing placeholders
	 * @return The message with placeholders replaced
	 */
	@NotNull
	public static String setPlaceholders(@Nullable final Player player, @NotNull final String message) {
		if (USE_PLACEHOLDERAPI && player != null) {
			try {
				return PlaceholderAPI.setPlaceholders(player, message);
			} catch (final NoClassDefFoundError e) {
				// PlaceholderAPI not available at runtime
				return message;
			}
		}
		return message;
	}

	/**
	 * Colorize a message with color codes and hex colors
	 *
	 * @param message The message to colorize
	 * @return The colorized message
	 */
	@NotNull
	public static String colorize(@NotNull final String message) {
		return ChatColor.translateAlternateColorCodes('&', colorizeHex(message));
	}

	/**
	 * Colorize a message and process placeholders
	 *
	 * @param player  The player to process placeholders for
	 * @param message The message to process
	 * @return The processed message
	 */
	@NotNull
	public static String colorizeAndProcess(@Nullable final Player player, @NotNull final String message) {
		return colorize(setPlaceholders(player, message));
	}

	/**
	 * Colorize a component message
	 *
	 * @param message
	 * @return The colorized message
	 */
	@SuppressWarnings("deprecation") // from legacy is bugged, from legacy text the colors work
	@NotNull
	public static BaseComponent[] colorizeComponent(@NotNull final String message) {
		return TextComponent.fromLegacyText(colorize(message));
	}

	@NotNull
	private static String colorizeHex(@NotNull final String message) {
		final Matcher matcher = HEX_PATTERN.matcher(message);
		final StringBuffer buffer = new StringBuffer();

		while (matcher.find()) {
			final String hexColor = matcher.group(1); // Always group 1 now

			if (!HEX_SUPPORTED) {
				final char COLOR_CHAR = ChatColor.COLOR_CHAR;
				matcher.appendReplacement(buffer, COLOR_CHAR + "x"
						+ COLOR_CHAR + hexColor.charAt(0) + COLOR_CHAR + hexColor.charAt(1)
						+ COLOR_CHAR + hexColor.charAt(2) + COLOR_CHAR + hexColor.charAt(3)
						+ COLOR_CHAR + hexColor.charAt(4) + COLOR_CHAR + hexColor.charAt(5));
			} else {
				matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of(hexColor).toString());
			}
		}

		return matcher.appendTail(buffer).toString();
	}

	/**
	 * Send a message to a command sender
	 *
	 * @param sender  The sender to send the message to
	 * @param message The message to send
	 */
	public static void send(@NotNull final CommandSender sender, @NotNull final String message) {
		sender.sendMessage(colorize(message));
	}

	/**
	 * Filter a list of strings to only include entries matching a token
	 *
	 * @param token    The token to match
	 * @param toFilter The list to filter
	 * @return A new filtered list (original list is not modified)
	 */
	public static List<String> getMatchingEntries(final String token, final List<String> toFilter) {
		return toFilter.stream()
				.filter(s -> s.toLowerCase().contains(token.toLowerCase()))
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Estimates the visual length of a chat message by removing color codes
	 * and calculating approximate character width.
	 *
	 * @param message The message to measure
	 * @return Estimated visual character count
	 */
	public static int estimateVisualLength(@NotNull final String message) {
		// Remove Minecraft color codes and hex codes using ChatColor.stripColor
		// which is more efficient than regex replaceAll
		final String cleanMessage = ChatColor.stripColor(colorize(message));
		return cleanMessage.length();
	}

	/**
	 * Checks if a chat message is likely to wrap to the next line in Minecraft chat.
	 * Minecraft chat typically shows around 53-55 characters per line depending on characters used.
	 * TODO try to use MinecraftFont
	 *
	 * @param message   The message to check
	 * @param maxLength Maximum recommended length (default: 50 for safety)
	 * @return true if the message is likely to wrap
	 */
	public static boolean isLineTooLong(@NotNull final String message, final int maxLength) {
		return estimateVisualLength(message) > maxLength;
	}

	/**
	 * Checks if a chat message is likely to wrap to the next line in Minecraft chat.
	 * Uses a conservative estimate of 50 characters.
	 *
	 * @param message The message to check
	 * @return true if the message is likely to wrap
	 */
	public static boolean isLineTooLong(@NotNull final String message) {
		return isLineTooLong(message, 50);
	}

	private static final int CENTER_PX = 154;

	public static String centerMessage(final Player player, String message) {
		if (message == null || message.equals(""))
			player.sendMessage("");
		message = colorize(message);

		int messagePxSize = 0;
		boolean previousCode = false;
		boolean isBold = false;

		for (final char c : message.toCharArray()) {
			if (c == '§') {
				previousCode = true;
				continue;
			} else if (previousCode) {
				previousCode = false;
				if (c == 'l' || c == 'L') {
					isBold = true;
					continue;
				} else
					isBold = false;
			} else {
				final DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
				messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
				messagePxSize++;
			}
		}

		final int halvedMessageSize = messagePxSize / 2;
		final int toCompensate = CENTER_PX - halvedMessageSize;
		final int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
		int compensated = 0;
		final StringBuilder sb = new StringBuilder();
		while (compensated < toCompensate) {
			sb.append(" ");
			compensated += spaceLength;
		}
		return sb.toString() + message;
	}
}
