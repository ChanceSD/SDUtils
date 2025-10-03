package me.chancesd.sdutils.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Stores information about a command argument including its type, validation rules,
 * and tab completion options.
 *
 * <p>
 * This class is used in a fluent builder pattern to define command arguments
 * for {@link BaseCommand}. Each argument has a name, type, and various optional
 * properties like default values, permissions, and custom tab completions.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>{@code
 * command.argument("player", ArgumentType.PLAYER)
 * 		.required()
 * 		.requirePermission("myplugin.admin")
 * 		.endArgument();
 * }</pre>
 *
 * @see BaseCommand
 * @see ArgumentType
 */
public class ArgumentInfo {
	/**
	 * Provider function for generating player not found error messages.
	 * Can be customized using {@link #setPlayerNotFoundMessageProvider(UnaryOperator)}.
	 */
	private static UnaryOperator<String> playerNotFoundMessageProvider = playerName -> "§cPlayer not found: §e" + playerName;

	private final String name;
	private final ArgumentType type;
	private boolean required = false; // Optional by default
	private String defValue = null;
	private String perm = null;
	private List<String> customTabCompletions = null;
	private String dependsOn = null;
	private final BaseCommand parent; // For fluent return

	/**
	 * Constructor for builder pattern - only requires name and type.
	 *
	 * @param name   the name of this argument
	 * @param type   the type of this argument (determines validation and tab completion)
	 * @param parent the parent command for fluent chaining
	 */
	public ArgumentInfo(final String name, final ArgumentType type, final BaseCommand parent) {
		this.name = name;
		this.type = type;
		this.parent = parent;
	}

	// === Fluent Builder Methods ===

	/**
	 * Marks this argument as required. Required arguments must be provided
	 * by the user and will cause command execution to fail if missing.
	 *
	 * @return this ArgumentInfo for method chaining
	 */
	public ArgumentInfo required() {
		this.required = true;
		return this;
	}

	/**
	 * Sets a default value for this argument. If the user doesn't provide
	 * this argument, the default value will be used instead.
	 *
	 * @param defaultValue the default value to use when argument is not provided
	 * @return this ArgumentInfo for method chaining
	 */
	public ArgumentInfo defaultValue(final String defaultValue) {
		this.defValue = defaultValue;
		return this;
	}

	/**
	 * Requires a specific permission to use this argument. If the user doesn't
	 * have the permission, the argument will not appear in tab completions
	 * and will be rejected during validation.
	 *
	 * @param permission the permission required to use this argument
	 * @return this ArgumentInfo for method chaining
	 */
	public ArgumentInfo requirePermission(final String permission) {
		this.perm = permission;
		return this;
	}

	/**
	 * Sets custom tab completion values for this argument. This overrides
	 * the default tab completions that would be generated based on the argument type.
	 *
	 * @param completions the custom tab completion values
	 * @return this ArgumentInfo for method chaining
	 */
	public ArgumentInfo tabComplete(final String... completions) {
		this.customTabCompletions = Arrays.asList(completions);
		return this;
	}

	/**
	 * Sets custom tab completion values for this argument. This overrides
	 * the default tab completions that would be generated based on the argument type.
	 *
	 * @param completions the list of custom tab completion values
	 * @return this ArgumentInfo for method chaining
	 */
	public ArgumentInfo tabComplete(final List<String> completions) {
		this.customTabCompletions = new ArrayList<>(completions);
		return this;
	}

	/**
	 * Makes this argument depend on another argument. Dependent arguments
	 * may have different behavior based on the value of their dependency.
	 *
	 * @param argumentName the name of the argument this one depends on
	 * @return this ArgumentInfo for method chaining
	 */
	public ArgumentInfo dependsOn(final String argumentName) {
		this.dependsOn = argumentName;
		return this;
	}

	/**
	 * Completes the argument definition and returns to the parent command
	 * for further configuration. This method adds this ArgumentInfo to
	 * the parent command's argument list.
	 *
	 * @return the parent BaseCommand for continued fluent configuration
	 */
	public BaseCommand endArgument() {
		if (parent != null) {
			parent.addArgumentInfo(this);
		}
		return parent;
	}

	/**
	 * Sets the global message provider for player not found errors.
	 * This allows customization of error messages across all ArgumentInfo instances.
	 *
	 * @param provider function that takes a player name and returns an error message
	 */
	public static void setPlayerNotFoundMessageProvider(final UnaryOperator<String> provider) {
		playerNotFoundMessageProvider = provider;
	}

	// === Getter Methods ===

	/**
	 * Gets the name of this argument.
	 *
	 * @return the argument name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the type of this argument.
	 *
	 * @return the argument type
	 */
	public ArgumentType getType() {
		return type;
	}

	/**
	 * Checks if this argument is required.
	 *
	 * @return true if this argument is required, false if optional
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * Gets the default value for this argument.
	 *
	 * @return the default value, or null if no default is set
	 */
	public String getDefaultValue() {
		return defValue;
	}

	/**
	 * Checks if this argument has a permission requirement.
	 *
	 * @return true if a permission is required, false otherwise
	 */
	public boolean hasPermission() {
		return perm != null && !perm.isEmpty();
	}

	/**
	 * Gets the permission required for this argument.
	 *
	 * @return the required permission, or null if no permission is required
	 */
	public String getPermission() {
		return perm;
	}

	/**
	 * Gets the name of the argument this one depends on.
	 *
	 * @return the dependency argument name, or null if no dependency
	 */
	public String getDependsOn() {
		return dependsOn;
	}

	/**
	 * Checks if this argument has a dependency on another argument.
	 *
	 * @return true if this argument depends on another, false otherwise
	 */
	public boolean hasDependency() {
		return dependsOn != null && !dependsOn.isEmpty();
	}

	// === Validation Methods ===

	/**
	 * Validates whether the given argument is valid for this argument type.
	 * This method attempts to parse the argument value according to the
	 * argument type and returns whether the parsing was successful.
	 *
	 * @param argument the command argument to validate
	 * @return true if the argument is valid for this type, false otherwise
	 */
	public boolean isValid(final CommandArgument argument) {
		try {
			switch (type) {
			case PLAYER:
				return argument.getAsPlayer() != null;
			case PLAYER_OR_ALL:
				return argument.isWildcard() || argument.getAsPlayerOrWildcard() != null;
			case INTEGER:
				argument.getAsInt();
				return true;
			case DOUBLE:
				argument.getAsDouble();
				return true;
			case BOOLEAN:
				argument.getAsBoolean();
				return true;
			case WORLD:
				argument.getAsWorld();
				return true;
			case MATERIAL:
				argument.getAsMaterial();
				return true;
			case DURATION:
				argument.getAsDuration();
				return true;
			case STRING, STRING_ARRAY:
			default:
				return true;
			}
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * Gets an appropriate error message for when validation fails.
	 * This method returns a user-friendly error message explaining
	 * why the argument validation failed, specific to the argument type.
	 *
	 * @param argument the command argument that failed validation
	 * @return a formatted error message explaining the validation failure
	 */
	public String getValidationErrorMessage(final CommandArgument argument) {
		switch (type) {
		case PLAYER:
			return playerNotFoundMessageProvider.apply(argument.getValue());
		case PLAYER_OR_ALL:
			return playerNotFoundMessageProvider.apply(argument.getValue());
		case INTEGER:
		case DOUBLE:
			return "§c'§7" + argument.getValue() + "§c' is not a valid number.";
		case BOOLEAN:
			return "§c'" + argument.getValue() + "' is not a valid boolean (true/false).";
		case WORLD:
			return "§c'" + argument.getValue() + "' is not a valid world name.";
		case MATERIAL:
			return "§c'" + argument.getValue() + "' is not a valid material name.";
		case DURATION:
			return "§c'" + argument.getValue() + "' is not a valid duration. Use formats like '30s', '5m', '2h', '1d'.";
		case STRING:
		case STRING_ARRAY:
		default:
			return "§c'" + argument.getValue() + "' is not valid for " + name + ".";
		}
	}

	/**
	 * Gets appropriate tab completions for this argument type.
	 * This method returns tab completion suggestions based on the argument type,
	 * taking into account any custom tab completions that may have been set.
	 *
	 * <p>
	 * For built-in types like PLAYER, WORLD, MATERIAL, etc., this method
	 * will return appropriate suggestions from the server (online players,
	 * world names, material names, etc.).
	 * </p>
	 *
	 * @param partial the partial input from the user to filter completions
	 * @return a list of possible tab completions matching the partial input
	 */
	public List<String> getTabCompletions(final String partial) {
		if (customTabCompletions != null && !customTabCompletions.isEmpty()) {
			return getMatchingEntries(partial, customTabCompletions);
		}

		switch (type) {
		case PLAYER, OFFLINE_PLAYER:
			final List<String> playerNames = Bukkit.getOnlinePlayers().stream()
					.map(Player::getName)
					.collect(Collectors.toList());
			return getMatchingEntries(partial, playerNames);
		case PLAYER_OR_ALL:
			final List<String> completions = new ArrayList<>();
			completions.add("*");
			completions.addAll(Bukkit.getOnlinePlayers().stream()
					.map(Player::getName)
					.collect(Collectors.toList()));
			return getMatchingEntries(partial, completions);
		case WORLD:
			final List<String> worldNames = Bukkit.getWorlds().stream()
					.map(org.bukkit.World::getName)
					.collect(Collectors.toList());
			return getMatchingEntries(partial, worldNames);
		case MATERIAL:
			final List<String> materialNames = Arrays.stream(Material.values())
					.map(Material::name)
					.map(String::toLowerCase)
					.collect(Collectors.toList());
			return getMatchingEntries(partial, materialNames);
		case DURATION:
			return getMatchingEntries(partial, Arrays.asList("30s", "5m", "1h", "1d", "1h30m"));
		case BOOLEAN:
			return getMatchingEntries(partial, Arrays.asList("true", "false"));
		case STRING, STRING_ARRAY:
			return getMatchingEntries(partial, Arrays.asList(name));
		default:
			return new ArrayList<>();
		}
	}

	/**
	 * Filters a list of strings to only include those that contain the given token.
	 * This is used internally for tab completion filtering and performs case-insensitive matching.
	 *
	 * @param token    the text to search for within each string
	 * @param toFilter the list of strings to filter
	 * @return a filtered list containing only strings that contain the token
	 */
	private static List<String> getMatchingEntries(final String token, final List<String> toFilter) {
		return toFilter.stream()
				.filter(s -> s.toLowerCase().contains(token.toLowerCase()))
				.collect(Collectors.toList());
	}
}
