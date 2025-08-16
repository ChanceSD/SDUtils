package me.chancesd.sdutils.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class TimeUtil {

	private static final ChronoUnit[] types = { ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS };

	private TimeUtil() {
	}

	public static String getDiffUntil(final TimeLangProvider lang, final long date) {
		return TimeUtil.getDiffMsg(lang, Instant.now(), Instant.ofEpochMilli(date));
	}

	/**
	 * Gets a formatted time difference using translations from an enum class.
	 * 
	 * @param langClass the enum class that implements TimeLangProvider
	 * @param date the target date in milliseconds since epoch
	 * @return formatted time difference string using the class's translations
	 */
	public static String getDiffUntil(Class<? extends Enum<? extends TimeLangProvider>> langClass, final long date) {
		TimeLangProvider provider = getProviderFromClass(langClass);
		return getDiffUntil(provider, date);
	}

	public static String getDiffDuration(final TimeLangProvider lang, final long date) {
		return TimeUtil.getDiffMsg(lang, Instant.now(), Instant.now().plusMillis(date));
	}

	/**
	 * Gets a formatted duration string using translations from an enum class.
	 * 
	 * @param langClass the enum class that implements TimeLangProvider
	 * @param durationMs the duration in milliseconds
	 * @return formatted duration string using the class's translations
	 */
	public static String getDiffDuration(Class<? extends Enum<? extends TimeLangProvider>> langClass, final long durationMs) {
		TimeLangProvider provider = getProviderFromClass(langClass);
		return getDiffDuration(provider, durationMs);
	}

	public static String getDiffMsg(final TimeLangProvider lang, final Instant from, final Instant to) {
		boolean future = false;
		if (to.equals(from))
			return lang.getTime(ChronoUnit.FOREVER);
		if (to.isAfter(from)) {
			future = true;
		}
		final NCDuration duration = NCDuration.between(from, to).plusMillis(future ? 50 : -50);
		final StringBuilder sb = new StringBuilder();
		int accuracy = 0;
		for (int i = 0; i < types.length; i++) {
			if (accuracy > 2) {
				break;
			}
			final int value = duration.get(types[i]);
			if (value > 0) {
				accuracy++;
				sb.append(" ").append(value).append(" ").append(lang.getTime(types[i]));
			}
		}
		if (sb.length() == 0)
			return lang.getTime(ChronoUnit.FOREVER);
		return sb.toString().trim();
	}

	/**
	 * Helper method to get a TimeLangProvider from an enum class
	 */
	private static TimeLangProvider getProviderFromClass(Class<? extends Enum<? extends TimeLangProvider>> langClass) {
		try {
			Enum<? extends TimeLangProvider>[] constants = langClass.getEnumConstants();
			if (constants != null && constants.length > 0) {
				return (TimeLangProvider) constants[0];
			}
		} catch (Exception e) {
			// Fallback to default if anything goes wrong
		}
		return null;
	}

	public interface TimeLangProvider {
		public String getTime(final ChronoUnit time);
	}

}
