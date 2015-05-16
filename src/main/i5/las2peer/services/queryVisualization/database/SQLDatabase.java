package i5.las2peer.services.queryVisualization.database;

import i5.las2peer.security.Context;
import i5.las2peer.services.queryVisualization.query.Query;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * SQLDatabase.java
 *<br>
 * Provides access to SQL Queries.
 * Instances of this class are only loaded in the SQLDatabaseManager if they are currently connected.
 * 
 */
public class SQLDatabase {
	private Connection connection = null;
	private boolean isConnected = false;
	
	private SQLDatabaseType jdbcInfo = null;
	private String username = null;
	private String password = null;
	private String database = null;
	private String host = null;
	private int port = -1;
	private String key = null;
	
	public SQLDatabase(SQLDatabaseType jdbcInfo, String username, String password, String database, String host, int port, String key) {

		//TODO: check parameters
		
		this.jdbcInfo = jdbcInfo;
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		this.database = database;
		this.key = key;
	}
	
	public SQLDatabase(SQLDatabaseSettings settings) {
		this.jdbcInfo = settings.getJdbcInfo();
		this.username = settings.getUsername();
		this.password = settings.getPassword();
		this.host = settings.getHost();
		this.port = settings.getPort();
		this.database = settings.getDatabase();
		this.key = settings.getKey();
	}

	public SQLDatabase(Query query) {
		this.jdbcInfo = query.getJdbcInfo();
		this.username = query.getUsername();
		this.password = query.getPassword();
		this.host = query.getHost();
		this.port = query.getPort();
		this.database = query.getDatabaseName();
		this.key = query.getKey();
	}
	
	public boolean connect() throws Exception {
		try {
			Class.forName(jdbcInfo.getDriverName()).newInstance();
			this.connection = DriverManager.getConnection(jdbcInfo.getJDBCurl(this.host, this.database, this.port), this.username, this.password);
			
			if(!this.connection.isClosed()) {
				this.isConnected = true;
				return true;
			}
			else {
				throw new Exception("Failed to connect to database!");
			}
		} 
		catch (ClassNotFoundException e) {
			Context.logMessage(this, e.getMessage());
			throw new Exception("JDBC-Driver for requested database type not found! Make sure the library is defined in the settings and is placed in the library folder! ", e);
		}
		catch (SQLException e) {
			Context.logMessage(this, e.getMessage());
			throw e;
		}
		catch(Exception e) {
			Context.logMessage(this, e.getMessage());
			throw e;
		}
	}
	
	public boolean disconnect() {
		try {
			this.connection.close();
			this.isConnected = false;
			this.connection = null;
			
			return true;
		} 
		catch (SQLException e) {
			e.printStackTrace();
			this.isConnected = false;
			this.connection = null;
			Context.logMessage(this, e.getMessage());
		}
		
		return false;
	}
	
	public boolean isConnected() {
		try {
			return (this.isConnected && this.connection != null && !this.connection.isClosed());
		} catch (SQLException e) {
			e.printStackTrace();
			Context.logMessage(this, e.getMessage());
			return false;
		}
	}
	
	public ResultSet executeQuery(String sqlQuery) throws Exception  {

		// I don't allow escape characters...
		// at least some very basic escape checking
		// (I don't think it is sufficient for a real attack though....)
		sqlQuery = sqlQuery.replace("\\", "\\\\");
		sqlQuery = sqlQuery.replace("\0", "\\0");
		sqlQuery = sqlQuery.replace(";", "");
		
		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(sqlQuery);
			
			return resultSet;
		}
		catch(Exception e) {
			Context.logMessage(this, e.getMessage());
			throw e;
		}
	}
	
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return connection.prepareStatement(sql);
	}

	public String getUser()  {
		return this.username;
	}
	
	public String getPassword()  {
		return this.password;
	}
	
	public String getDatabase()  {
		return this.database;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public int getPort() {
		return this.port;
	}

	public String getKey() {
		return this.key;
	}
	
	public SQLDatabaseType getJdbcInfo() {
		return jdbcInfo;
	}

}
