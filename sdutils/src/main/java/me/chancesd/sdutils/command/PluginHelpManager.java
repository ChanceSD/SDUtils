package me.chancesd.sdutils.command;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import me.chancesd.sdutils.display.chat.ChatMenu;
import me.chancesd.sdutils.display.chat.NavigationButtons;
import me.chancesd.sdutils.display.chat.content.StaticContentProvider;
import me.chancesd.sdutils.plugin.SDPlugin;
import me.chancesd.sdutils.utils.ChatUtils;

/**
 * Manages plugin-wide help menu generation by automatically discovering
 * all registered BaseCommands and creating comprehensive help displays.
 */
public class PluginHelpManager {

	private static final String DEFAULT_PLUGIN_COLOR_CODE = "4";
	
	private final SDPlugin plugin;
	private final String pluginName;
	private final String pluginVersion;

	public PluginHelpManager(final SDPlugin plugin) {
		this.plugin = plugin;
		this.pluginName = plugin.getName();
		this.pluginVersion = plugin.getDescription().getVersion();
	}

	/**
	 * Generate a comprehensive help menu for all registered commands in the plugin.
	 * Only shows commands the sender has permission to use.
	 * 
	 * @param sender The command sender (for permission filtering)
	 * @param navigationCommand The command prefix for page navigation (e.g., "/pmr help")
	 * @return A ChatMenu displaying all available plugin commands
	 */
	public ChatMenu generatePluginHelp(final CommandSender sender, final String navigationCommand) {
		return generatePluginHelp(sender, navigationCommand, 15); // Default 15 lines per page
	}

	/**
	 * Generate a comprehensive help menu for all registered commands in the plugin.
	 * Only shows commands the sender has permission to use.
	 * 
	 * @param sender The command sender (for permission filtering)
	 * @param navigationCommand The command prefix for page navigation (e.g., "/pmr help")
	 * @param linesPerPage Number of lines per page
	 * @return A ChatMenu displaying all available plugin commands
	 */
	public ChatMenu generatePluginHelp(final CommandSender sender, final String navigationCommand, final int linesPerPage) {
		final StaticContentProvider contentProvider = new StaticContentProvider();

		// Get all registered commands and filter by permissions
		final Collection<BaseCommand> allCommands = plugin.getRegisteredCommands();
		final List<BaseCommand> availableCommands = allCommands.stream()
				.filter(cmd -> cmd.hasPermission(sender))
				.collect(Collectors.toList());

		if (availableCommands.isEmpty()) {
			contentProvider.addLine("&7No commands available.", null, null);
		} else {
			contentProvider.addLine("#4CAF50&l► Available Commands:", null, null);
			
			for (final BaseCommand command : availableCommands) {
				addCommandToContent(contentProvider, command);
			}
		}

		// Generate header with plugin info
		final String header = String.format("#607D8B&l╔══════ §%s§l%s #f7e758&lv%s #9E9E9E(Page {page}/{total}) #607D8B&l══════╗",
				getPluginColorCode(), pluginName, pluginVersion);

		return ChatMenu.builder()
				.header(header)
				.footer("#607D8B&l╚══════════════════════════════╝")
				.linesPerPage(linesPerPage)
				.contentProvider(contentProvider)
				.navigation(NavigationButtons.builder()
						.navigationPrefix(navigationCommand)
						.previousText("&7[&c« Previous Page&7]")
						.nextText("&7[&cNext Page »&7]")
						.build())
				.build();
	}

	/**
	 * Add a command to the content provider with proper formatting and hover text.
	 */
	private void addCommandToContent(final StaticContentProvider contentProvider, final BaseCommand command) {
		// Get primary command name/alias
		final List<String> aliases = command.getAliases();
		final String commandName = aliases.isEmpty() ? "unknown" : aliases.get(0);
		
		// Build command display text
		final String usage = command.getUsage();
		final String description = command.getDescription();
		
		// Format: "  /command - Description"
		String displayText;
		if (usage != null && !usage.isEmpty()) {
			displayText = String.format("  #4CAF50%s #9E9E9E- &f%s", usage, description);
		} else {
			displayText = String.format("  #4CAF50/%s #9E9E9E- &f%s", commandName, description);
		}

		// Generate click command (use usage if available, otherwise just command name)
		String clickCommand;
		if (usage != null && !usage.isEmpty()) {
			clickCommand = usage.split(" ")[0]; // Extract just the command part
		} else {
			clickCommand = "/" + commandName;
		}

		// Generate hover text similar to Help.java
		final String hoverText = buildHoverText(command, description);

		contentProvider.addLine(displayText, clickCommand, hoverText);
	}

	/**
	 * Build hover text for a command with permissions and description.
	 * Similar to the pattern used in Help.java but auto-generated.
	 */
	private String buildHoverText(final BaseCommand command, final String description) {
		final StringBuilder hover = new StringBuilder();
		
		// Determine action text based on whether command needs parameters
		final String usage = command.getUsage();
		final boolean needsParameters = usage != null && usage.contains("<") || usage != null && usage.contains("[");
		
		if (needsParameters) {
			hover.append("&7Click to see command usage");
		} else {
			hover.append("&7Click to execute command");
		}
		
		// Add description
		if (description != null && !description.isEmpty()) {
			hover.append("\n&7&o").append(description);
		}
		
		// Add permissions if any
		final Set<String> permissions = command.getPermissions();
		if (!permissions.isEmpty()) {
			hover.append("\n&#992222&lRequires: &#c98d81").append(String.join(", ", permissions));
		}
		
		return ChatUtils.colorize(hover.toString());
	}

	/**
	 * Get color code for plugin name in header.
	 * Can be customized per plugin or use default.
	 */
	private String getPluginColorCode() {
		// Default to red "4" like PvPManager, but this could be made configurable
		return DEFAULT_PLUGIN_COLOR_CODE;
	}

	/**
	 * Create a default plugin help menu with standard navigation command.
	 * Uses the plugin name to generate navigation command (e.g., "/pluginname help").
	 */
	public ChatMenu generateDefaultPluginHelp(final CommandSender sender) {
		final String defaultNavCommand = "/" + pluginName.toLowerCase() + " help";
		return generatePluginHelp(sender, defaultNavCommand);
	}
}
