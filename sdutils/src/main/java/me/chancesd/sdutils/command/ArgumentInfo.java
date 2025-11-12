package me.chancesd.sdutils.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

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
 * Alternatively, arguments can use an {@link ArgumentLoader} for asynchronous
 * loading of complex objects (like offline players). When a loader is provided,
 * the type-based validation is skipped in favor of the loader's validation.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>{@code
 * // Type-based argument
 * command.argument("player", ArgumentType.PLAYER)
 * 		.required()
 * 		.requirePermission("myplugin.admin")
 * 		.endArgument();
 * 
 * // Loader-based argument
 * command.argument("offlinePlayer", ArgumentLoader.of(name -> {
 *     UUID uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
 *     return playerManager.getOrLoadOffline(uuid);
 * }).build())
 * 		.required()
 * 		.endArgument();
 * }</pre>
 *
 * @see BaseCommand
 * @see ArgumentType
 * @see ArgumentLoader
 */
public class ArgumentInfo {
	/**
	 * Provider function for generating player not found error messages.
	 * Can be customized using {@link #setPlayerNotFoundMessageProvider(UnaryOperator)}.
	 */
	private static UnaryOperator<String> playerNotFoundMessageProvider = playerName -> "§cPlayer not found: §e" + playerName;

	private final String name;
	private final ArgumentType type;
	private final ArgumentLoader<?> loader;
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
		this.loader = BaseCommand.getDefaultLoader(type);
		this.parent = parent;
	}

	/**
	 * Constructor for loader-based arguments that load values asynchronously.
	 *
	 * @param name   the name of this argument
	 * @param type   the type for tab completion (validation is done by the loader)
	 * @param loader the loader to use for asynchronous value loading
	 * @param parent the parent command for fluent chaining
	 */
	public ArgumentInfo(final String name, final ArgumentType type, final ArgumentLoader<?> loader, final BaseCommand parent) {
		this.name = name;
		this.type = type;
		this.loader = loader;
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

	/**
	 * Checks if this argument uses an {@link ArgumentLoader} for async loading.
	 *
	 * @return true if this argument has a loader, false otherwise
	 */
	public boolean hasLoader() {
		return loader != null;
	}

	/**
	 * Gets the {@link ArgumentLoader} for this argument.
	 *
	 * @return the argument loader, or null if no loader is configured
	 */
	public ArgumentLoader<?> getLoader() {
		return loader;
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
			if (type == ArgumentType.PLAYER) {
				return argument.getAsPlayer() != null;
			} else if (type == ArgumentType.PLAYER_OR_ALL) {
				return argument.isWildcard() || argument.getAsPlayerOrWildcard() != null;
			} else if (type == ArgumentType.INTEGER) {
				argument.getAsInt();
				return true;
			} else if (type == ArgumentType.DOUBLE) {
				argument.getAsDouble();
				return true;
			} else if (type == ArgumentType.BOOLEAN) {
				argument.getAsBoolean();
				return true;
			} else if (type == ArgumentType.WORLD) {
				argument.getAsWorld();
				return true;
			} else if (type == ArgumentType.MATERIAL) {
				argument.getAsMaterial();
				return true;
			} else if (type == ArgumentType.DURATION) {
				argument.getAsDuration();
				return true;
			} else if (type == ArgumentType.STRING || type == ArgumentType.STRING_ARRAY) {
				return true;
			} else if (type.isEnumType()) {
				// Validate enum types by checking if the value matches any enum constant
				final String value = argument.getValue().toUpperCase();
				final Class<? extends Enum<?>> enumClass = type.getEnumClass();
				for (final Enum<?> constant : enumClass.getEnumConstants()) {
					if (constant.name().equalsIgnoreCase(value)) {
						return true;
					}
				}
				return false;
			}
			return true;
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
		if (type == ArgumentType.PLAYER) {
			return playerNotFoundMessageProvider.apply(argument.getValue());
		} else if (type == ArgumentType.PLAYER_OR_ALL) {
			return playerNotFoundMessageProvider.apply(argument.getValue());
		} else if (type == ArgumentType.INTEGER || type == ArgumentType.DOUBLE) {
			return "§c'§7" + argument.getValue() + "§c' is not a valid number.";
		} else if (type == ArgumentType.BOOLEAN) {
			return "§c'" + argument.getValue() + "' is not a valid boolean (true/false).";
		} else if (type == ArgumentType.WORLD) {
			return "§c'" + argument.getValue() + "' is not a valid world name.";
		} else if (type == ArgumentType.MATERIAL) {
			return "§c'" + argument.getValue() + "' is not a valid material name.";
		} else if (type == ArgumentType.DURATION) {
			return "§c'" + argument.getValue() + "' is not a valid duration. Use formats like '30s', '5m', '2h', '1d'.";
		} else if (type == ArgumentType.STRING || type == ArgumentType.STRING_ARRAY) {
			return "§c'" + argument.getValue() + "' is not valid for " + name + ".";
		} else if (type.isEnumType()) {
			final StringBuilder options = new StringBuilder();
			final Enum<?>[] constants = type.getEnumClass().getEnumConstants();
			for (int i = 0; i < constants.length; i++) {
				if (i > 0) options.append("&8, ");
				options.append("&e").append(constants[i].name());
			}
			return "#FF5555Invalid " + type.getEnumClass().getSimpleName() + ". &7Available options are: &e" + options;
		} else {
			// For other custom types, provide a generic message
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

		// Check if the type has custom tab completions (e.g., from enumType() or custom())
		if (type.hasTabCompletions()) {
			return getMatchingEntries(partial, type.getTabCompletions().get());
		}

		// Handle predefined types
		if (type == ArgumentType.PLAYER || type == ArgumentType.OFFLINE_PLAYER) {
			final List<String> playerNames = Bukkit.getOnlinePlayers().stream()
					.map(Player::getName)
					.toList();
			return getMatchingEntries(partial, playerNames);
		} else if (type == ArgumentType.PLAYER_OR_ALL) {
			final List<String> completions = new ArrayList<>();
			completions.add("*");
			completions.addAll(Bukkit.getOnlinePlayers().stream()
					.map(Player::getName)
					.toList());
			return getMatchingEntries(partial, completions);
		} else if (type == ArgumentType.WORLD) {
			final List<String> worldNames = Bukkit.getWorlds().stream()
					.map(org.bukkit.World::getName)
					.toList();
			return getMatchingEntries(partial, worldNames);
		} else if (type == ArgumentType.MATERIAL) {
			final List<String> materialNames = Arrays.stream(Material.values())
					.map(Material::name)
					.map(String::toLowerCase)
					.toList();
			return getMatchingEntries(partial, materialNames);
		} else if (type == ArgumentType.DURATION) {
			return getMatchingEntries(partial, Arrays.asList("30s", "5m", "1h", "1d", "1h30m"));
		} else if (type == ArgumentType.BOOLEAN) {
			return getMatchingEntries(partial, Arrays.asList("true", "false"));
		} else if (type == ArgumentType.STRING || type == ArgumentType.STRING_ARRAY) {
			return getMatchingEntries(partial, Arrays.asList(name));
		}
		
		return new ArrayList<>();
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
				.toList();
	}
}
