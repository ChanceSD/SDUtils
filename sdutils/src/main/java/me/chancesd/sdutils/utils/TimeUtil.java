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

	public static String getDiffDuration(final TimeLangProvider lang, final long date) {
		return TimeUtil.getDiffMsg(lang, Instant.now(), Instant.now().plusMillis(date));
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

	public interface TimeLangProvider {
		public String getTime(final ChronoUnit time);
	}

}
