package me.chancesd.sdutils.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import me.chancesd.sdutils.display.chat.ChatMenu;
import me.chancesd.sdutils.display.chat.NavigationButtons;
import me.chancesd.sdutils.display.chat.content.StaticContentProvider;
import me.chancesd.sdutils.utils.ChatUtils;

/**
 * A powerful base class for creating commands with automatic argument validation,
 * tab completion, permission checking, and subcommand support using a fluent builder pattern.
 * 
 * Commands are automatically integrated with plugin.yml definitions through PluginCommand.
 *
 * Example usage:
 *
 * <pre>
 * public class MyCommand extends BaseCommand {
 * 	public MyCommand(PluginCommand pluginCommand) {
 * 		super(pluginCommand);  // Automatically imports name and aliases from plugin.yml
 * 		description("Example command")
 * 				.displayName("My Custom Command")  // Override display name in help menus
 * 				.usage("/mycommand &lt;player&gt; [time]")
 * 				.permission("myplugin.mycommand")
 * 				.argument("target", ArgumentType.PLAYER).required().endArgument()
 * 				.argument("time", ArgumentType.DURATION).defaultValue("5m").dependsOn("target").endArgument();
 * 	}
 *
 * 	public void execute(CommandSender sender, String label, List&lt;CommandArgument&gt; args) {
 * 		CommandArgument target = getArgument(args, "target");
 * 		CommandArgument time = getArgument(args, "time");
 * 		// ... command logic
 * 	}
 * }
 * </pre>
 */
public abstract class BaseCommand implements TabExecutor {
	// Global defaults for all commands
	private static Function<CommandSender, String> noPermissionMessageProvider = sender -> "§cYou don't have permission to use this command.";
	private static Function<CommandSender, String> playerOnlyMessageProvider = sender -> "§cThis command can only be used by players.";

	private BaseCommand parentCommand;
	private PluginCommand pluginCommand;
	private final List<String> aliases = new ArrayList<>();
	private final List<BaseCommand> subCommands = new ArrayList<>();
	private final Set<String> permissions = new HashSet<>();
	private final List<ArgumentInfo> argumentInfos = new ArrayList<>();
	private String commandDisplayName;
	private String description = "";
	private String usage = "";
	private boolean playerOnly = false;

	protected BaseCommand() {
		this.commandDisplayName = getClass().getSimpleName() + " Help";
	}

	protected BaseCommand(final PluginCommand pluginCommand) {
		this();
		this.pluginCommand = pluginCommand;
		// Import aliases from plugin.yml
		alias(pluginCommand.getAliases().toArray(new String[0]));
	}

	/**
	 * Set global default messages for all commands
	 */
	public static void setGlobalDefaults(
			final Function<CommandSender, String> noPermission,
			final Function<CommandSender, String> playerOnly,
			final UnaryOperator<String> playerNotFound) {
		noPermissionMessageProvider = noPermission;
		playerOnlyMessageProvider = playerOnly;
		// Also set the provider for ArgumentInfo
		ArgumentInfo.setPlayerNotFoundMessageProvider(playerNotFound);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (playerOnly && !(sender instanceof Player)) {
			sender.sendMessage(playerOnlyMessageProvider.apply(sender));
			return true;
		}

		if (!hasPermission(sender)) {
			sender.sendMessage(noPermissionMessageProvider.apply(sender));
			return true;
		}

		// Handle subcommands first
		if (args.length > 0) {
			// Handle built-in "help" command for pagination
			if (args[0].equalsIgnoreCase("help")) {
				int page = 1;
				
				// Parse page number if provided: /command help <page>
				if (args.length > 1) {
					try {
						page = Integer.parseInt(args[1]);
						if (page < 1) page = 1;
					} catch (final NumberFormatException e) {
						sender.sendMessage("§cInvalid page number!");
						return true;
					}
				}

				showHelpMenu(sender, page);
				return true;
			}
			
			// Handle regular subcommands
			for (final BaseCommand subCommand : subCommands) {
				if (subCommand.isAlias(args[0])) {
					final String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
					return subCommand.onCommand(sender, command, args[0], subArgs);
				}
			}
		}

		final List<CommandArgument> parsedArgs = new ArrayList<>();

		// Track which arguments were actually provided by the user
		final Set<String> userProvidedArgs = new HashSet<>();
		for (int i = 0; i < Math.min(args.length, argumentInfos.size()); i++) {
			userProvidedArgs.add(argumentInfos.get(i).getName());
		}

		// Process all argument definitions
		for (int i = 0; i < argumentInfos.size(); i++) {
			final ArgumentInfo argInfo = argumentInfos.get(i);

			// Check if argument was provided
			if (i < args.length) {
				// Permission check for provided arguments
				if (argInfo.hasPermission() && !sender.hasPermission(argInfo.getPermission())) {
					sender.sendMessage(noPermissionMessageProvider.apply(sender));
					return true;
				}

				if (argInfo.getType() == ArgumentType.STRING_ARRAY) {
					final String joinedValue = String.join(" ", Arrays.copyOfRange(args, i, args.length));
					parsedArgs.add(new CommandArgument(joinedValue));
					break; // no validation needed for STRING_ARRAY
				}

				final CommandArgument argument = new CommandArgument(args[i]);
				if (!argInfo.isValid(argument)) {
					sender.sendMessage(argInfo.getValidationErrorMessage(argument));
					return true;
				}

				parsedArgs.add(argument);
			} else {
				// Argument not provided - check dependencies and defaults
				if (argInfo.isRequired()) {
					sender.sendMessage("§cMissing required argument: §e" + argInfo.getName());
					if (!usage.isEmpty()) {
						sender.sendMessage("§7Usage: §b" + usage);
					}
					return true;
				} else if (argInfo.hasDependency()) {
					// Only add if dependency was provided by user
					if (userProvidedArgs.contains(argInfo.getDependsOn()) && argInfo.getDefaultValue() != null) {
						parsedArgs.add(new CommandArgument(argInfo.getDefaultValue()));
					}
				} else if (argInfo.getDefaultValue() != null) {
					// Regular optional argument with default value
					parsedArgs.add(new CommandArgument(argInfo.getDefaultValue()));
				}
				// Note: Optional arguments without default values or unmet dependencies are not added to parsedArgs
			}
		}

		execute(sender, label, parsedArgs);
		return true;
	}

	/**
	 * Execute the command with pre-validated arguments
	 */
	public abstract void execute(CommandSender sender, String label, List<CommandArgument> args);

	/**
	 * Permission checking with inheritance/override support
	 */
	public boolean hasPermission(final CommandSender sender) {
		if (!permissions.isEmpty()) {
			return permissions.stream().anyMatch(sender::hasPermission);
		}
		if (parentCommand != null) {
			return parentCommand.hasPermission(sender);
		}
		return true;
	}

	private boolean isAlias(final String input) {
		return aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(input));
	}

	// Fluent builder methods
	public BaseCommand alias(final String... aliasNames) {
		this.aliases.addAll(Arrays.asList(aliasNames));
		return this;
	}

	public BaseCommand permission(final String... permissionNodes) {
		this.permissions.addAll(Arrays.asList(permissionNodes));
		return this;
	}

	public BaseCommand subCommand(final String name, final BaseCommand subCommand) {
		subCommand.parentCommand = this;
		subCommand.aliases.add(name);
		this.subCommands.add(subCommand);
		return this;
	}

	public BaseCommand description(final String descriptionText) {
		this.description = descriptionText;
		return this;
	}

	public BaseCommand usage(final String usageText) {
		this.usage = usageText;
		return this;
	}

	public BaseCommand displayName(final String displayNameText) {
		this.commandDisplayName = displayNameText;
		return this;
	}

	public BaseCommand playerOnly() {
		this.playerOnly = true;
		return this;
	}

	/**
	 * Start building a new argument with fluent API
	 */
	public ArgumentInfo argument(final String name, final ArgumentType type) {
		return new ArgumentInfo(name, type, this);
	}

	/**
	 * Internal method for ArgumentInfo to add itself to the command
	 */
	public void addArgumentInfo(final ArgumentInfo argumentInfo) {
		this.argumentInfos.add(argumentInfo);
	}

	/**
	 * Get argument by name from parsed arguments
	 */
	public CommandArgument getArgument(final List<CommandArgument> args, final String name) {
		for (int i = 0; i < Math.min(args.size(), argumentInfos.size()); i++) {
			if (argumentInfos.get(i).getName().equals(name)) {
				return args.get(i);
			}
		}
		return null;
	}

	/**
	 * Check if argument exists in parsed arguments
	 */
	public boolean hasArgument(final List<CommandArgument> args, final String name) {
		return getArgument(args, name) != null;
	}

	// Tab completion with permission awareness
	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
		// First check for subcommand delegation if args.length > 1
		if (args.length > 1) {
			// Handle help command tab completion: /command help <page>
			if (args[0].equalsIgnoreCase("help") && args.length == 2) {
				return ChatUtils.getMatchingEntries(args[1], Arrays.asList("1", "2", "3", "4", "5"));
			}
			
			for (final BaseCommand subCommand : subCommands) {
				if (subCommand.isAlias(args[0]) && subCommand.hasPermission(sender)) {
					final String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
					return subCommand.onTabComplete(sender, command, args[0], subArgs);
				}
			}
		}

		// If args.length == 1, prioritize subcommands if they exist
		if (args.length == 1 && !subCommands.isEmpty()) {
			final List<String> subCommandAliases = subCommands.stream()
					.filter(sub -> sub.hasPermission(sender))
					.flatMap(sub -> sub.aliases.stream())
					.collect(Collectors.toList());
			
			// Add "help" as a built-in subcommand
			subCommandAliases.add("help");
			
			return ChatUtils.getMatchingEntries(args[0], subCommandAliases);
		}

		// Provide tab completion for argument values (with permission checking)
		final int argIndex = args.length - 1;
		if (argIndex < argumentInfos.size()) {
			final ArgumentInfo argInfo = argumentInfos.get(argIndex);
			// Only provide tab completion if user has permission to use this argument
			if (!argInfo.hasPermission() || sender.hasPermission(argInfo.getPermission())) {
				return argInfo.getTabCompletions(args[argIndex]);
			}
		}
		return new ArrayList<>();
	}

	/**
	 * Get the description of this command
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the usage string of this command
	 */
	public String getUsage() {
		return usage;
	}

	/**
	 * Get the aliases of this command
	 */
	public List<String> getAliases() {
		return new ArrayList<>(aliases);
	}

	/**
	 * Get the subcommands of this command
	 */
	public List<BaseCommand> getSubCommands() {
		return new ArrayList<>(subCommands);
	}

	/**
	 * Get the permissions required for this command
	 */
	public Set<String> getPermissions() {
		return new HashSet<>(permissions);
	}

	/**
	 * Check if this command requires a player
	 */
	public boolean isPlayerOnly() {
		return playerOnly;
	}

	/**
	 * Generate an interactive help menu for this command and its subcommands.
	 * Only shows subcommands the sender has permission to use.
	 * 
	 * @param sender The command sender (for permission filtering)
	 * @return A ChatMenu displaying available subcommands and their descriptions
	 */
	public ChatMenu generateHelpMenu(final CommandSender sender) {
		final StaticContentProvider contentProvider = new StaticContentProvider();
		
		// Header with command description
		if (!description.isEmpty()) {
			contentProvider.addLine("#FFEB3B&l► " + description, null, null);
			contentProvider.addLine("", null, null);
		}
		
		// Show usage if available
		if (!usage.isEmpty()) {
			contentProvider.addLine("#9E9E9E&lUsage: &8" + usage, null, null);
			contentProvider.addLine("", null, null);
		}
		
		// Show available subcommands (filtered by permissions)
		final List<BaseCommand> availableSubCommands = subCommands.stream()
				.filter(subCmd -> subCmd.hasPermission(sender))
				.collect(Collectors.toList());

		final String mainCommandName = aliases.isEmpty() ? pluginCommand.getName().toLowerCase() : aliases.get(0);
		
		if (!availableSubCommands.isEmpty()) {
			contentProvider.addLine("#4CAF50&l► Available Commands:", null, null);
			
			for (final BaseCommand subCmd : availableSubCommands) {
				final String subCommandName = subCmd.aliases.isEmpty() ? "unknown" : subCmd.aliases.get(0);
				final String subDescription = subCmd.getDescription().isEmpty() ? "No description available" : subCmd.getDescription();
				final String subUsage = subCmd.usage.isEmpty() ? "" : subCmd.usage;
				
				// Build command line with description
				final String commandLine = "  #4CAF50/" + mainCommandName + " " + subCommandName + " #9E9E9E- &f" + subDescription;
				
				// Build hover text with permissions and usage
				final String hoverText = buildSubcommandHoverText(subDescription, subUsage, subCmd.getPermissions());
				
				// Add suggest command for clicking
				final String suggestCommand = "/" + mainCommandName + " " + subCommandName;
				
				contentProvider.addLine(commandLine, suggestCommand, hoverText);
			}
		} else {
			contentProvider.addLine("#FF5722No subcommands available or no permissions.", null, null);
		}
		
		// Build the menu
		return ChatMenu.builder()
				.header("#607D8B&l╔════════ #FFEB3B&l" + commandDisplayName + " #9E9E9E(Page {page}/{total}) #607D8B&l════════╗")
				.footer("#607D8B&l╚══════════════════════════════╝")
				.linesPerPage(14)
				.contentProvider(contentProvider)
				.navigation(NavigationButtons.builder()
						.navigationPrefix("/" + mainCommandName + " help")
						.previousText("#9E9E9E[#FF5722« Previous Page#9E9E9E]")
						.nextText("#9E9E9E[#FF5722Next Page »#9E9E9E]")
						.build())
				.build();
	}
	
	/**
	 * Show the auto-generated help menu to a command sender
	 * 
	 * @param sender The command sender to show the help menu to
	 * @param page The page number to show (defaults to 1)
	 */
	public void showHelpMenu(final CommandSender sender, final int page) {
		generateHelpMenu(sender).show(sender, page);
	}
	
	/**
	 * Show the auto-generated help menu to a command sender (first page)
	 * 
	 * @param sender The command sender to show the help menu to
	 */
	public void showHelpMenu(final CommandSender sender) {
		showHelpMenu(sender, 1);
	}
	

	
	/**
	 * Build hover text for subcommands in the help menu
	 */
	private String buildSubcommandHoverText(final String description, final String usage, final Set<String> permissions) {
		final StringBuilder hover = new StringBuilder();
		
		hover.append("#9E9E9EClick to see command usage\n");
		hover.append("#9E9E9E&o").append(description);
		
		boolean hasUsage = !usage.isEmpty();
		boolean hasPermissions = !permissions.isEmpty();

		if (hasUsage || hasPermissions) {
			hover.append("\n");
		}

		if (hasUsage) {
			hover.append("\n#4CAF50&lUsage: &8").append(usage);
		}

		if (hasPermissions) {
			hover.append("\n#FF5722&lRequires: #c98d81").append(String.join(", ", permissions));
		}
		
		return hover.toString();
	}
}
