package i5.las2peer.services.mobsos.queryVisualization.dal;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class QVDatabase implements Serializable {
	private static final long serialVersionUID = 1L;

	@NotNull
	private String db_code;
	@NotNull
	private String username = "root";
	private String password = "";
	@NotNull
	private String database;
	@NotNull
	private String dbhost = "localhost";
	@NotNull
	private int port = 3306;

	public String getDb_code() {
		return db_code;
	}

	public void setDb_code(String db_code) {
		this.db_code = db_code;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getDbhost() {
		return dbhost;
	}

	public void setDbhost(String dbhost) {
		this.dbhost = dbhost;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
