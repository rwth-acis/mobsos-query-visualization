package i5.las2peer.services.queryVisualization.database;

import java.sql.SQLException;

public class DoesNotExistException extends SQLException {
	private static final long serialVersionUID = -6510620628957382786L;

	public DoesNotExistException(String string) {
		super(string);
	}

	public DoesNotExistException() {
		// TODO Auto-generated constructor stub
	}
}
