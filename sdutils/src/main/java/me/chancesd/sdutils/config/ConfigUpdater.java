package me.chancesd.sdutils.config;

import com.google.common.base.Preconditions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigUpdater {

	// Used for separating keys in the keyBuilder inside parseComments method
	private static final char SEPARATOR = '.';

	public static void update(final Plugin plugin, final String resourceName, final File toUpdate, final String... ignoredSections) throws IOException {
		update(plugin, resourceName, toUpdate, Arrays.asList(ignoredSections), Collections.emptyList());
	}

	public static void update(final Plugin plugin, final String resourceName, final File toUpdate, final List<String> ignoredSections,
			final List<String> overrideSections) throws IOException {
		Preconditions.checkArgument(toUpdate.exists(), "The toUpdate file doesn't exist!");

		final FileConfiguration defaultConfig = YamlConfiguration
				.loadConfiguration(new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8));
		final FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(toUpdate);
		final Map<String, String> comments = parseComments(plugin, resourceName, defaultConfig);
		final Map<String, String> ignoredSectionsValues = parseIgnoredSections(toUpdate, comments,
				ignoredSections == null ? Collections.emptyList() : ignoredSections);
		final List<String> overrideSectionsArrayList = new ArrayList<>(overrideSections);
		// ignoredSections can ONLY contain configurations sections
		overrideSectionsArrayList.removeIf(overrideSection -> defaultConfig.get(overrideSection) == null);

		// will write updated config file "contents" to a string
		final StringWriter writer = new StringWriter();
		write(defaultConfig, currentConfig, new BufferedWriter(writer), comments, ignoredSectionsValues, overrideSectionsArrayList);
		final String value = writer.toString(); // config contents

		final Path toUpdatePath = toUpdate.toPath();
		if (!value.equals(new String(Files.readAllBytes(toUpdatePath), StandardCharsets.UTF_8))) { // if updated contents are not the same as current file
																									// contents, update
			Files.write(toUpdatePath, value.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static void write(final FileConfiguration defaultConfig, final FileConfiguration currentConfig, final BufferedWriter writer,
			final Map<String, String> comments, final Map<String, String> ignoredSectionsValues, final List<String> ignoredSections) throws IOException {
		// Used for converting objects to yaml, then cleared
		final FileConfiguration parserConfig = new YamlConfiguration();

		outer: for (final String fullKey : defaultConfig.getKeys(true)) {
			final String indents = KeyUtils.getIndents(fullKey, SEPARATOR);

			if (!ignoredSectionsValues.isEmpty()) {
				if (writeIgnoredSectionValueIfExists(ignoredSectionsValues, writer, fullKey))
					continue;
			}
			writeCommentIfExists(comments, writer, fullKey, indents);
			Object currentValue = currentConfig.get(fullKey);
			final Object defaultValue = defaultConfig.get(fullKey);

			if (currentValue == null || !defaultValue.getClass().equals(currentValue.getClass()))
				currentValue = defaultConfig.get(fullKey);

			final String[] splitFullKey = fullKey.split("[" + SEPARATOR + "]");
			final String trailingKey = splitFullKey[splitFullKey.length - 1];

			for (final String ignoredSection : ignoredSections) {
				if (fullKey.startsWith(ignoredSection)) {
					final Object newObj = defaultConfig.get(fullKey);
					if (newObj instanceof ConfigurationSection) {
						writeConfigurationSection(writer, indents, trailingKey, (ConfigurationSection) newObj);
					} else if (newObj != null) {
						writeYamlValue(parserConfig, writer, indents, trailingKey, newObj);
					}
					continue outer;
				}
			}

			if (currentValue instanceof ConfigurationSection) {
				writeConfigurationSection(writer, indents, trailingKey, (ConfigurationSection) currentValue);
				continue;
			}
			writeYamlValue(parserConfig, writer, indents, trailingKey, currentValue);
		}

		final String danglingComments = comments.get(null);

		if (danglingComments != null)
			writer.write(danglingComments);

		writer.close();
	}

	// Returns a map of key comment pairs. If a key doesn't have any comments it won't be included in the map.
	private static Map<String, String> parseComments(final Plugin plugin, final String resourceName, final FileConfiguration defaultConfig) throws IOException {
		// keys are in order
		final List<String> keys = new ArrayList<>(defaultConfig.getKeys(true));
		final BufferedReader reader = new BufferedReader(new InputStreamReader(plugin.getResource(resourceName)));
		final Map<String, String> comments = new LinkedHashMap<>();
		final StringBuilder commentBuilder = new StringBuilder();
		final KeyBuilder keyBuilder = new KeyBuilder(defaultConfig, SEPARATOR);
		String currentValidKey = null;

		String line;
		while ((line = reader.readLine()) != null) {
			final String trimmedLine = line.trim();
			// Only getting comments for keys. A list/array element comment(s) not supported
			if (trimmedLine.startsWith("-"))
				continue;

			if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {// Is blank line or is comment
				commentBuilder.append(trimmedLine).append("\n");
			} else {// is a valid yaml key
				// This part verifies if it is the first non-nested key in the YAML file and then stores the result as the
				// next non-nested value.
				if (!line.startsWith(" ")) {
					keyBuilder.clear();// add clear method instead of create new instance.
					currentValidKey = trimmedLine;
				}

				keyBuilder.parseLine(trimmedLine, true);
				final String key = keyBuilder.toString();

				// If there is a comment associated with the key it is added to comments map and the commentBuilder is reset
				if (commentBuilder.length() > 0) {
					comments.put(key, commentBuilder.toString());
					commentBuilder.setLength(0);
				}

				final int nextKeyIndex = keys.indexOf(keyBuilder.toString()) + 1;
				if (nextKeyIndex < keys.size()) {

					final String nextKey = keys.get(nextKeyIndex);
					while (!keyBuilder.isEmpty() && !nextKey.startsWith(keyBuilder.toString())) {
						keyBuilder.removeLastKey();
					}
					// If all keys are cleared in a loop, then the first key from the nested keys in the YAML file is assigned to
					// this keyBuilder instance.
					// If the file contains multiple non-nested keys, the next first non-nested key will be used.
					if (keyBuilder.isEmpty()) {
						keyBuilder.parseLine(currentValidKey, false);
					}
				}
			}
		}
		reader.close();

		if (commentBuilder.length() > 0)
			comments.put(null, commentBuilder.toString());

		return comments;
	}

	private static Map<String, String> parseIgnoredSections(final File toUpdate, final Map<String, String> comments, final List<String> ignoredSections)
			throws IOException {
		final Map<String, String> ignoredSectionValues = new LinkedHashMap<>(ignoredSections.size());

		final DumperOptions options = new DumperOptions();
		options.setLineBreak(DumperOptions.LineBreak.UNIX);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		final Yaml yaml = new Yaml(new YamlConstructor(), new YamlRepresenter(), options);

		final Map<Object, Object> root = (Map<Object, Object>) yaml.load(new FileReader(toUpdate));
		ignoredSections.forEach(section -> {
			final String[] split = section.split("[" + SEPARATOR + "]");
			final String key = split[split.length - 1];
			final Map<Object, Object> map = getSection(section, root);
			if (map.isEmpty())
				return;

			final StringBuilder keyBuilder = new StringBuilder();
			for (int i = 0; i < split.length; i++) {
				if (i != split.length - 1) {
					if (keyBuilder.length() > 0)
						keyBuilder.append(SEPARATOR);

					keyBuilder.append(split[i]);
				}
			}

			ignoredSectionValues.put(section, buildIgnored(key, map, comments, keyBuilder, new StringBuilder(), yaml));
		});

		return ignoredSectionValues;
	}

	private static Map<Object, Object> getSection(final String fullKey, final Map<Object, Object> root) {
		final String[] keys = fullKey.split("[" + SEPARATOR + "]", 2);
		final String key = keys[0];
		final Object value = root.get(getKeyAsObject(key, root));
		if (!(value instanceof Map))
			return Collections.emptyMap();

		if (keys.length == 1) {
			if (value instanceof Map)
				return root;
			/*
			 * if (value == null) {
			 * Map<Object, Object> map= new HashMap<>();
			 * map.put(key,"{}");
			 * System.out.println("key " + key);
			 * return map;
			 * }
			 */
			throw new IllegalArgumentException("Ignored sections must be a ConfigurationSection not a value!");
		}

		if (!(value instanceof Map))
			throw new IllegalArgumentException("Invalid ignored ConfigurationSection specified!");

		return getSection(keys[1], (Map<Object, Object>) value);
	}

	private static String buildIgnored(final String fullKey, final Map<Object, Object> ymlMap, final Map<String, String> comments, StringBuilder keyBuilder,
			final StringBuilder ignoredBuilder, final Yaml yaml) {
		// 0 will be the next key, 1 will be the remaining keys
		final String[] keys = fullKey.split("[" + SEPARATOR + "]", 2);
		final String key = keys[0];
		final Object originalKey = getKeyAsObject(key, ymlMap);

		if (keyBuilder.length() > 0)
			keyBuilder.append(".");

		keyBuilder.append(key);

		if (!ymlMap.containsKey(originalKey)) {
			if (keys.length == 1)
				throw new IllegalArgumentException("Invalid ignored section: " + keyBuilder);

			throw new IllegalArgumentException("Invalid ignored section: " + keyBuilder + "." + keys[1]);
		}

		final String comment = comments.get(keyBuilder.toString());
		final String indents = KeyUtils.getIndents(keyBuilder.toString(), SEPARATOR);

		if (comment != null)
			ignoredBuilder.append(addIndentation(comment, indents)).append("\n");

		ignoredBuilder.append(addIndentation(key, indents)).append(":");
		final Object obj = ymlMap.get(originalKey);

		if (obj instanceof Map) {
			final Map<Object, Object> map = (Map<Object, Object>) obj;

			if (map.isEmpty()) {
				ignoredBuilder.append(" {}\n");
			} else {
				ignoredBuilder.append("\n");
			}

			final StringBuilder preLoopKey = new StringBuilder(keyBuilder);

			for (final Object o : map.keySet()) {
				buildIgnored(o.toString(), map, comments, keyBuilder, ignoredBuilder, yaml);
				keyBuilder = new StringBuilder(preLoopKey);
			}
		} else {
			writeIgnoredValue(yaml, obj, ignoredBuilder, indents);
		}

		return ignoredBuilder.toString();
	}

	private static void writeIgnoredValue(final Yaml yaml, final Object toWrite, final StringBuilder ignoredBuilder, final String indents) {
		final String yml = yaml.dump(toWrite);
		if (toWrite instanceof Collection && !((Collection<?>) toWrite).isEmpty()) {
			ignoredBuilder.append("\n").append(addIndentation(yml, indents)).append("\n");
		} else {
			ignoredBuilder.append(" ").append(yml);
		}
	}

	private static String addIndentation(final String s, final String indents) {
		final StringBuilder builder = new StringBuilder();
		final String[] split = s.split("\n");

		for (final String value : split) {
			if (builder.length() > 0)
				builder.append("\n");

			builder.append(indents).append(value);
		}

		return builder.toString();
	}

	private static void writeCommentIfExists(final Map<String, String> comments, final BufferedWriter writer, final String fullKey, final String indents)
			throws IOException {
		final String comment = comments.get(fullKey);

		// Comments always end with new line (\n)
		if (comment != null)
			// Replaces all '\n' with '\n' + indents except for the last one
			writer.write(indents + comment.substring(0, comment.length() - 1).replace("\n", "\n" + indents) + "\n");
	}

	// Will try to get the correct key by using the sectionContext
	private static Object getKeyAsObject(final String key, final Map<Object, Object> sectionContext) {
		if (sectionContext.containsKey(key))
			return key;

		try {
			final Float keyFloat = Float.parseFloat(key);

			if (sectionContext.containsKey(keyFloat))
				return keyFloat;
		} catch (final NumberFormatException ignored) {
		}

		try {
			final Double keyDouble = Double.parseDouble(key);

			if (sectionContext.containsKey(keyDouble))
				return keyDouble;
		} catch (final NumberFormatException ignored) {
		}

		try {
			final Integer keyInteger = Integer.parseInt(key);

			if (sectionContext.containsKey(keyInteger))
				return keyInteger;
		} catch (final NumberFormatException ignored) {
		}

		try {
			final Long longKey = Long.parseLong(key);

			if (sectionContext.containsKey(longKey))
				return longKey;
		} catch (final NumberFormatException ignored) {
		}

		return null;
	}

	/**
	 * Writes the current value with the provided trailing key to the provided writer.
	 *
	 * @param parserConfig   The parser configuration to use for writing the YAML value.
	 * @param bufferedWriter The writer to write the value to.
	 * @param indents        The string representation of the indentation.
	 * @param trailingKey    The trailing key for the YAML value.
	 * @param currentValue   The current value to write as YAML.
	 * @throws IOException If an I/O error occurs while writing the YAML value.
	 */
	private static void writeYamlValue(final FileConfiguration parserConfig, final BufferedWriter bufferedWriter, final String indents,
			final String trailingKey, final Object currentValue) throws IOException {
		if (currentValue instanceof List) {
			final List<String> list = (List<String>) currentValue;
			writeList(list, trailingKey, indents, new Yaml(), bufferedWriter);
			return;
		}
		parserConfig.set(trailingKey, currentValue);
		String yaml = parserConfig.saveToString();
		yaml = yaml.substring(0, yaml.length() - 1).replace("\n", "\n" + indents);
		final String toWrite = indents + yaml + "\n";
		parserConfig.set(trailingKey, null);
		bufferedWriter.write(toWrite);
	}

	// Writes a list of any object
	private static void writeList(final List<?> list, final String actualKey, final String prefixSpaces, final Yaml yaml, final BufferedWriter writer)
			throws IOException {
		writer.write(getListAsString(list, actualKey, prefixSpaces, yaml));
	}

	private static String getListAsString(final List<?> list, final String actualKey, final String prefixSpaces, final Yaml yaml) {
		final StringBuilder builder = new StringBuilder(prefixSpaces).append(actualKey).append(":");

		if (list.isEmpty()) {
			builder.append(" []\n");
			return builder.toString();
		}

		builder.append("\n");

		for (int i = 0; i < list.size(); i++) {
			final Object o = list.get(i);

			if (o instanceof String || o instanceof Character) {
				builder.append(prefixSpaces).append("  - '").append(o).append("'");
			} else if (o instanceof List) {
				builder.append(prefixSpaces).append("- ").append(yaml.dump(o));
			} else {
				builder.append(prefixSpaces).append("- ").append(o);
			}

			if (i != list.size()) {
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	/**
	 * Writes the value associated with the ignored section to the provided writer,
	 * if it exists in the ignoredSectionsValues map.
	 *
	 * @param ignoredSectionsValues The map containing the ignored section-value mappings.
	 * @param bufferedWriter        The writer to write the value to.
	 * @param fullKey               The full key to search for in the ignoredSectionsValues map.
	 * @throws IOException If an I/O error occurs while writing the value.
	 */
	private static boolean writeIgnoredSectionValueIfExists(final Map<String, String> ignoredSectionsValues, final BufferedWriter bufferedWriter,
			final String fullKey) throws IOException {
		final String ignored = ignoredSectionsValues.get(fullKey);
		if (ignored != null) {
			bufferedWriter.write(ignored);
			return true;
		}
		for (final Map.Entry<String, String> entry : ignoredSectionsValues.entrySet()) {
			if (KeyUtils.isSubKeyOf(entry.getKey(), fullKey, SEPARATOR)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Writes a configuration section with the provided trailing key and the current value to the provided writer.
	 *
	 * @param bufferedWriter       The writer to write the configuration section to.
	 * @param indents              The string representation of the indentation level.
	 * @param trailingKey          The trailing key for the configuration section.
	 * @param configurationSection The current value of the configuration section.
	 * @throws IOException If an I/O error occurs while writing the configuration section.
	 */
	private static void writeConfigurationSection(final BufferedWriter bufferedWriter, final String indents, final String trailingKey,
			final ConfigurationSection configurationSection) throws IOException {
		bufferedWriter.write(indents + trailingKey + ":");
		if (!configurationSection.getKeys(false).isEmpty()) {
			bufferedWriter.write("\n");
		} else {
			bufferedWriter.write(" {}\n");
		}
	}
}