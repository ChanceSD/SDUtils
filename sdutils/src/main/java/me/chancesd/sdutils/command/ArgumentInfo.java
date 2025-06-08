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
 */
public class ArgumentInfo {
	private static UnaryOperator<String> playerNotFoundMessageProvider = playerName -> "§cPlayer not found: §e" + playerName;
	
	private final String name;
	private final ArgumentType type;
	private boolean required = false; // Optional by default
	private String defValue = null;
	private String perm = null;
	private List<String> customTabCompletions = null;
	private String dependsOn = null;
	private BaseCommand parent; // For fluent return

	/**
	 * Constructor for builder pattern - only requires name and type
	 */
	public ArgumentInfo(final String name, final ArgumentType type, final BaseCommand parent) {
		this.name = name;
		this.type = type;
		this.parent = parent;
	}

	// Fluent builder methods
	public ArgumentInfo required() {
		this.required = true;
		return this;
	}

	public ArgumentInfo defaultValue(final String defaultValue) {
		this.defValue = defaultValue;
		return this;
	}

	public ArgumentInfo requirePermission(final String permission) {
		this.perm = permission;
		return this;
	}

	public ArgumentInfo tabComplete(final String... completions) {
		this.customTabCompletions = Arrays.asList(completions);
		return this;
	}

	public ArgumentInfo tabComplete(final List<String> completions) {
		this.customTabCompletions = new ArrayList<>(completions);
		return this;
	}

	public ArgumentInfo dependsOn(final String argumentName) {
		this.dependsOn = argumentName;
		return this;
	}

	public BaseCommand endArgument() {
		if (parent != null) {
			parent.addArgumentInfo(this);
		}
		return parent;
	}

	/**
	 * Sets the global message provider for player not found errors.
	 */
	public static void setPlayerNotFoundMessageProvider(final UnaryOperator<String> provider) {
		playerNotFoundMessageProvider = provider;
	}

	public String getName() {
		return name;
	}

	public ArgumentType getType() {
		return type;
	}

	public boolean isRequired() {
		return required;
	}

	public String getDefaultValue() {
		return defValue;
	}

	public boolean hasPermission() {
		return perm != null && !perm.isEmpty();
	}

	public String getPermission() {
		return perm;
	}

	public String getDependsOn() {
		return dependsOn;
	}

	public boolean hasDependency() {
		return dependsOn != null && !dependsOn.isEmpty();
	}

	/**
	 * Validates whether the given argument is valid for this argument type.
	 */
	public boolean isValid(final CommandArgument argument) {
		try {
			switch (type) {
			case PLAYER:
				return argument.getAsPlayerOrNull() != null;
			case PLAYER_OR_ALL:
				return argument.isWildcard() || argument.getAsPlayerOrNull() != null;
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
			case STRING:
			case STRING_ARRAY:
			default:
				return true;
			}
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * Gets an appropriate error message for when validation fails.
	 */
	public String getValidationErrorMessage(final CommandArgument argument) {
		switch (type) {
		case PLAYER:
			return playerNotFoundMessageProvider.apply(argument.getValue());
		case PLAYER_OR_ALL:
			return playerNotFoundMessageProvider.apply(argument.getValue());
		case INTEGER:
			return "§c'" + argument.getValue() + "' is not a valid integer.";
		case DOUBLE:
			return "§c'" + argument.getValue() + "' is not a valid number.";
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
	 */
	public List<String> getTabCompletions(final String partial) {
		if (customTabCompletions != null && !customTabCompletions.isEmpty()) {
			return getMatchingEntries(partial, customTabCompletions);
		}

		switch (type) {
		case PLAYER:
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
		case STRING:
		case STRING_ARRAY:
			return getMatchingEntries(partial, Arrays.asList(name));
		default:
			return new ArrayList<>();
		}
	}

	private static List<String> getMatchingEntries(final String token, final List<String> toFilter) {
		return toFilter.stream()
				.filter(s -> s.toLowerCase().contains(token.toLowerCase()))
				.collect(Collectors.toList());
	}
}
