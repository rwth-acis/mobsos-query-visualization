package i5.las2peer.services.queryVisualization.database;

import java.sql.SQLException;

public class DBDoesNotExistException extends SQLException {
	private static final long serialVersionUID = -6510620628957382786L;

	public DBDoesNotExistException(String string) {
		super(string);
	}

	public DBDoesNotExistException() {
		// TODO Auto-generated constructor stub
	}
}
