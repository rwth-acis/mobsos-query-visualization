package i5.las2peer.services.mobsos.queryVisualization.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import i5.las2peer.api.Service;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.Agent;

/**
 * 
 * SQLDatabaseManager.java <br>
 * The Manager of the SQLDatabases. This Class provides methods to handle the users databases.
 * 
 */
public class SQLDatabaseManager {
	// used to store (and retrieve during execution) the settings for the users'
	// databases
	private HashMap<String, SQLDatabaseSettings> userDatabaseMap = new HashMap<String, SQLDatabaseSettings>();
	private HashMap<String, SQLDatabase> loadedDatabases = new HashMap<String, SQLDatabase>();

	private SQLDatabase storageDatabase = null;
	private Service service = null;

	private boolean initializeUser() {
		try {
			Connection c = storageDatabase.getConnection();
			PreparedStatement p = c.prepareStatement("SELECT DISTINCT ID FROM USERS WHERE ID = ?");
			p.setLong(1, getActiveAgent().getId());
			ResultSet s = p.executeQuery();
			if (!s.next()) {
				p = c.prepareStatement("REPLACE INTO USERS (ID) VALUES (?)");
				p.setLong(1, getActiveAgent().getId());
				p.executeUpdate();
			}
			c.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*************** "service" helper methods *************************/

	/**
	 * get the current l2p thread
	 * 
	 * @return the L2pThread we're currently running in
	 */

	/**
	 * get the currently active agent
	 * 
	 * @return active agent
	 */
	protected Agent getActiveAgent() {
		return service.getContext().getMainAgent();
	}

	/**
	 * write a log message
	 * 
	 * @param message Message that will be logged
	 */
	protected void logMessage(String message) {
		getActiveNode().observerNotice(Event.SERVICE_MESSAGE, this.getClass().getName() + ": " + message);
	}

	/**
	 * get the currently active l2p node (from the current thread context)
	 * 
	 * @return the currently active las2peer node
	 */
	protected Node getActiveNode() {
		return service.getContext().getLocalNode();
	}

	/**************************
	 * end of service helper methods
	 ************************************/

	/**
	 * get an id String for the envelope stored for an user
	 * 
	 * @param user UserAgent
	 * @return String with the user id
	 */
	public static String getEnvelopeId(Agent user) {
		return "userDBs-" + user.getId();
	}

	public SQLDatabaseManager(Service service, SQLDatabase storageDatabase) {
		this.service = service;
		this.storageDatabase = storageDatabase;
		// get the user's security object which contains the database
		// information

		initializeUser();

		SQLDatabaseSettings[] settings = null;

		try {
			Connection c = storageDatabase.getConnection();
			PreparedStatement p = c.prepareStatement("SELECT * FROM DATABASE_CONNECTIONS WHERE USER = ?;");
			p.setLong(1, getActiveAgent().getId());
			ResultSet databases = p.executeQuery();
			settings = SQLDatabaseSettings.fromResultSet(databases);
			c.close();
		} catch (Exception e) {
			logMessage("Failed to get the users' SQL settings. " + e.getMessage());
		}

		for (SQLDatabaseSettings setting : settings)
			userDatabaseMap.put(setting.getKey(), setting);
	}

	// add database to users' security object
	public boolean addDatabase(String key, SQLDatabaseType jdbcInfo, String username, String password, String database,
			String host, int port) throws Exception {
		try {
			// TODO: sanity checks for the parameters
			if (databaseExists(key)) {
				throw new Exception("Database with key " + key + " already exists!");
			}

			SQLDatabaseSettings databaseSettings = new SQLDatabaseSettings(key, jdbcInfo, username, password, database,
					host, port);

			Connection c = storageDatabase.getConnection();
			PreparedStatement p = c
					.prepareStatement("REPLACE INTO `DATABASE_CONNECTIONS`(`JDBCINFO`, `KEY`, `USERNAME`, `PASSWORD`,"
							+ "`DATABASE`, `HOST`, `PORT`, `USER`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			p.setInt(1, databaseSettings.getJdbcInfo().getCode());
			p.setString(2, databaseSettings.getKey());
			p.setString(3, databaseSettings.getUsername());
			p.setString(4, databaseSettings.getPassword());
			p.setString(5, databaseSettings.getDatabase());
			p.setString(6, databaseSettings.getHost());
			p.setInt(7, databaseSettings.getPort());
			p.setLong(8, getActiveAgent().getId());
			p.executeUpdate();
			userDatabaseMap.put(databaseSettings.getKey(), databaseSettings);
			c.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}

	// removes a database from the hashmap and the security objects
	public boolean removeDatabase(String key) throws Exception {
		try {
			if (!databaseExists(key)) {
				// throw new Exception("Database with key " + key + " does not
				// exists!");
				return false;
			}

			if (userDatabaseMap != null && userDatabaseMap.containsKey(key)) {
				// delete from hash map and database
				removeDB(key);
				userDatabaseMap.remove(key);
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}

	public boolean databaseExists(String key) {
		try {
			return (userDatabaseMap.get(key) != null);
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		return false;
	}

	public int getDatabaseCount() {
		return this.userDatabaseMap.size();
	}

	// get a list of the names of all databases of the user
	public List<String> getDatabaseKeyList() {
		try {
			LinkedList<String> keyList = new LinkedList<String>();
			Iterator<String> iterator = this.userDatabaseMap.keySet().iterator();
			while (iterator.hasNext()) {
				keyList.add(iterator.next());
			}

			return keyList;
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		return null;
	}

	// returns a list of all database settings elements
	public List<SQLDatabaseSettings> getDatabaseSettingsList() {
		try {
			LinkedList<SQLDatabaseSettings> settingsList = new LinkedList<SQLDatabaseSettings>();
			Iterator<SQLDatabaseSettings> iterator = this.userDatabaseMap.values().iterator();
			while (iterator.hasNext()) {
				settingsList.add(iterator.next());
			}

			return settingsList;
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		return null;
	}

	public String getDatabaseIdString(String databaseKey) throws Exception {
		try {
			SQLDatabaseSettings databaseSettings = userDatabaseMap.get(databaseKey);

			if (databaseSettings == null) {
				return null;
			}

			return databaseSettings.getHost() + ":" + databaseSettings.getPort() + "/" + databaseSettings.getDatabase();
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}

	/**
	 * Get Settings of a database by its database name
	 * 
	 * @param databaseName Name of the database
	 * @return Settings of the database
	 * @throws Exception Exception
	 */
	public SQLDatabaseSettings getDatabaseByName(String databaseName) throws Exception {
		for (SQLDatabaseSettings db : userDatabaseMap.values()) {
			if (db.getDatabase().equals(databaseName)) {
				return db;
			}
		}
		return null;
	}

	/**
	 * Get Settings of a database by its database name
	 * 
	 * @param databaseKey Key of the database
	 * @return Settings of the database
	 * @throws Exception Exception
	 */
	public SQLDatabaseSettings getDatabaseSettings(String databaseKey) throws Exception {
		return userDatabaseMap.get(databaseKey);
	}

	// get a instance of a SQL database (JDBC based)
	public SQLDatabase getDatabaseInstance(String databaseKey) throws Exception {
		try {
			SQLDatabase sqlDatabase = loadedDatabases.get(databaseKey);

			if (sqlDatabase != null) {
				// TODO: check that the database is still open/valid
			} else {
				SQLDatabaseSettings databaseSettings = userDatabaseMap.get(databaseKey);

				if (databaseSettings == null) {
					// the requested database is not known
					String dbKeyListString = "";
					Iterator<String> iterator = getDatabaseKeyList().iterator();
					while (iterator.hasNext()) {
						dbKeyListString += " " + iterator.next();
					}

					throw new Exception("The requested database is not known/configured! (Requested:" + databaseKey
							+ ", Available: " + dbKeyListString + ")");
				}

				sqlDatabase = new SQLDatabase(databaseSettings);

				// try to connect ...
				// sqlDatabase.connect();
			}
			return sqlDatabase;
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}

	/**
	 * Remove given database from the database
	 */
	private void removeDB(String databaseKey) throws SQLException {
		try {

			Connection c = storageDatabase.getConnection();
			PreparedStatement s = c
					.prepareStatement("DELETE FROM `DATABASE_CONNECTIONS` WHERE `KEY` = ? AND `USER` = ?");
			s.setString(1, databaseKey);
			s.setLong(2, getActiveAgent().getId());
			s.executeUpdate();
			c.close();
		} catch (Exception e) {
			logMessage("Error removing the Database! " + e);
			System.out.println("QV critical:");
			e.printStackTrace();
		}
	}
}
