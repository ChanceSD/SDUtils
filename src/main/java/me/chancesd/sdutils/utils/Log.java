package me.chancesd.sdutils.utils;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

public final class Log {

	private static Logger logger;
	private static boolean debug;
	private static boolean silent;
	private static String prefixMsg;

	private Log() {
	}

	public static void setup(final Logger log, final String pluginPrefix) {
		Log.logger = log;
		Log.prefixMsg = pluginPrefix;
	}

	public static void infoColor(final String message) {
		if (!silent)
			Bukkit.getConsoleSender().sendMessage(prefixMsg + " " + message);
	}

	public static void info(final String message) {
		if (!silent)
			logger.info(message);
	}

	public static void severe(final String message) {
		logger.severe(message);
	}

	public static void severe(final String message, final Throwable thrown) {
		logger.log(Level.SEVERE, message, thrown);
	}

	public static void warning(final String message) {
		logger.warning(message);
	}

	public static void warning(final String message, final Throwable thrown) {
		logger.log(Level.WARNING, message, thrown);
	}

	public static void debug(final String message) {
		if (debug) {
			logger.info(message);
		}
	}

	public static void debugLazy(final Supplier<String> string) {
		if (debug) {
			logger.info(string.get());
		}
	}

	public static void setDebug(final boolean debug) {
		Log.debug = debug;
	}

	public static void setSilent(final boolean silent) {
		Log.silent = silent;
	}

}
