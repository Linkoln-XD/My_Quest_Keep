package ru.link.questkeep.shared.persistence;

import java.sql.SQLException;

public final class PostgresSqlStates {

	public static final String EXCLUSION_VIOLATION = "23P01";
	public static final String UNIQUE_VIOLATION = "23505";
	public static final String DEADLOCK = "40P01";

	private PostgresSqlStates() {
	}

	public static String find(Throwable error) {
		Throwable current = error;
		while (current != null) {
			if (current instanceof SQLException sql && sql.getSQLState() != null) {
				return sql.getSQLState();
			}
			current = current.getCause();
		}
		return null;
	}

	public static boolean isExclusionViolation(Throwable error) {
		return EXCLUSION_VIOLATION.equals(find(error));
	}

	public static boolean isUniqueViolation(Throwable error) {
		return UNIQUE_VIOLATION.equals(find(error));
	}

	public static boolean isDeadlock(Throwable error) {
		return DEADLOCK.equals(find(error));
	}
}
