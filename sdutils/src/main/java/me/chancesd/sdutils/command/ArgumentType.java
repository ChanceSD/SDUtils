package me.chancesd.sdutils.command;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Represents a command argument type that determines validation and tab completion behavior.
 * <p>
 * This class provides predefined constants for common types (STRING, PLAYER, INTEGER, etc.)
 * as well as factory methods for creating custom types, including enum-based types.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Using predefined type
 * .argument("player", ArgumentType.PLAYER)
 * 
 * // Using enum type
 * .argument("locale", ArgumentType.enumType(Locale.class))
 * 
 * // Using fully custom type
 * .argument("custom", ArgumentType.custom("MY_TYPE", 
 *     () -> Arrays.asList("option1", "option2")))
 * }</pre>
 */
public class ArgumentType {
	/** A simple string argument */
	public static final ArgumentType STRING = new ArgumentType("STRING");

	/** A player name argument that must match an online player */
	public static final ArgumentType PLAYER = new ArgumentType("PLAYER");

	/** An integer number argument */
	public static final ArgumentType INTEGER = new ArgumentType("INTEGER");

	/** A double precision number argument */
	public static final ArgumentType DOUBLE = new ArgumentType("DOUBLE");

	/** A boolean argument (true/false, yes/no, on/off) */
	public static final ArgumentType BOOLEAN = new ArgumentType("BOOLEAN");

	/** A player name argument that accepts "*" wildcard or online player names */
	public static final ArgumentType PLAYER_OR_ALL = new ArgumentType("PLAYER_OR_ALL");

	/** A string array that captures all remaining arguments as a single string */
	public static final ArgumentType STRING_ARRAY = new ArgumentType("STRING_ARRAY");

	/** A world name argument that must match an existing world */
	public static final ArgumentType WORLD = new ArgumentType("WORLD");

	/** A material name argument that must match a valid Bukkit Material */
	public static final ArgumentType MATERIAL = new ArgumentType("MATERIAL");

	/** A duration argument (e.g., "30s", "5m", "2h", "1d") */
	public static final ArgumentType DURATION = new ArgumentType("DURATION");

	/** A player name argument that supports both online and offline players */
	public static final ArgumentType OFFLINE_PLAYER = new ArgumentType("OFFLINE_PLAYER");

	private final String name;
	private final Supplier<List<String>> tabCompletions;
	private final Class<? extends Enum<?>> enumClass;

	/**
	 * Private constructor for predefined types.
	 */
	private ArgumentType(final String name, final Supplier<List<String>> tabCompletions, final Class<? extends Enum<?>> enumClass) {
		this.name = name;
		this.tabCompletions = tabCompletions;
		this.enumClass = enumClass;
	}

	private ArgumentType(final String name) {
		this(name, null, null);
	}

	/**
	 * Creates a custom argument type with the specified name and tab completions.
	 *
	 * @param name           the unique name for this argument type
	 * @param tabCompletions supplier that provides tab completion options
	 * @return a new custom ArgumentType
	 */
	public static ArgumentType custom(final String name, final Supplier<List<String>> tabCompletions) {
		return new ArgumentType(name, tabCompletions, null);
	}

	/**
	 * Creates an enum-based argument type with automatic tab completions.
	 * The tab completions will be the names of all enum constants.
	 *
	 * @param <E>       the enum type
	 * @param enumClass the class object of the enum
	 * @return a new ArgumentType configured for the enum
	 */
	public static <E extends Enum<E>> ArgumentType enumType(final Class<E> enumClass) {
		return new ArgumentType("ENUM_" + enumClass.getSimpleName(),
				() -> Arrays.stream(enumClass.getEnumConstants())
						.map(Enum::name)
						.toList(),
				enumClass);
	}

	/**
	 * Gets the name of this argument type.
	 *
	 * @return the type name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the tab completion supplier for this type.
	 *
	 * @return the tab completion supplier, or null if none is defined
	 */
	public Supplier<List<String>> getTabCompletions() {
		return tabCompletions;
	}

	/**
	 * Checks if this type has custom tab completions.
	 *
	 * @return true if tab completions are defined
	 */
	public boolean hasTabCompletions() {
		return tabCompletions != null;
	}

	/**
	 * Gets the enum class if this is an enum type.
	 *
	 * @return the enum class, or null if this is not an enum type
	 */
	public Class<? extends Enum<?>> getEnumClass() {
		return enumClass;
	}

	/**
	 * Checks if this is an enum type.
	 *
	 * @return true if this type represents an enum
	 */
	public boolean isEnumType() {
		return enumClass != null;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final ArgumentType that = (ArgumentType) obj;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
