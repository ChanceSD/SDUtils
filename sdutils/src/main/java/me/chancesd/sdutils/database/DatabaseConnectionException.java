package me.chancesd.sdutils.database;

import java.sql.SQLException;

import org.jetbrains.annotations.Nullable;

import me.chancesd.sdutils.database.DatabaseConfigBuilder.DatabaseType;

/**
 * Thrown when a database connection can't be established. The message is
 * suitable to show to server owners without a stack trace.
 */
public class DatabaseConnectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final DatabaseType databaseType;

	public DatabaseConnectionException(final DatabaseType databaseType, final Throwable cause) {
		super(buildMessage(databaseType, cause), cause);
		this.databaseType = databaseType;
	}

	public DatabaseType getDatabaseType() {
		return databaseType;
	}

	private static String buildMessage(final DatabaseType type, final Throwable cause) {
		final Throwable root = rootCause(cause);
		final StringBuilder message = new StringBuilder("Could not connect to the ").append(type).append(" database: ")
				.append(firstLine(root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName()));
		final String hint = hint(cause);
		if (hint != null) {
			message.append(". ").append(hint);
		}
		return message.toString();
	}

	private static Throwable rootCause(final Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}

	private static String firstLine(final String text) {
		final int end = text.indexOf('\n');
		return (end < 0 ? text : text.substring(0, end)).trim();
	}

	/** Best effort hint for common failures, anything else just shows the root cause */
	@Nullable
	private static String hint(final Throwable cause) {
		for (Throwable t = cause; t != null && t.getCause() != t; t = t.getCause()) {
			if (t instanceof final SQLException sql) {
				final String state = sql.getSQLState() == null ? "" : sql.getSQLState();
				switch (sql.getErrorCode()) {
				case 1045:
					return "Check the username and password in the config";
				case 1040:
					return "The server has too many open connections";
				case 1049:
					return "The database does not exist, check the database name in the config";
				default:
					break;
				}
				if (state.startsWith("08")) {
					return "Check that the database server is running and the address and port in the config are correct";
				}
			}
		}
		return null;
	}
}
