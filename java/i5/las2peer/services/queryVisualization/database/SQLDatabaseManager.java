package i5.las2peer.services.queryVisualization.database;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * SQLDatabaseManager.java
 *<br>
 * The Manager of the SQLDatabases. This Class provides methods to handle the users databases.
 * 
 */
public class SQLDatabaseManager {
	// used to store (and retrieve during execution) the settings for the users' databases
	private HashMap<String, SQLDatabaseSettings> userDatabaseMap = new HashMap<String, SQLDatabaseSettings>();
	private HashMap<String, SQLDatabase> loadedDatabases = new HashMap<String, SQLDatabase>();
	
		
	private Envelope storedDBs = null;
	
	
	
	/*************** "service" helper methods *************************/
	
	/**
	 * get the current l2p thread
	 * @return the L2pThread we're currently running in
	 */
	public final L2pThread getL2pThread () {
		Thread t = Thread.currentThread();
		
		if (! ( t instanceof L2pThread ))
			throw new IllegalStateException ( "Not executed in a L2pThread environment!");
		
		return (L2pThread) t;
	}
	
	/** 
	 * get the currently active agent
	 * @return active agent
	 */
	protected Agent getActiveAgent () {
		return getL2pThread().getContext().getMainAgent();
	}
	
	
	/**
	 * write a log message
	 * 
	 * @param message
	 */
	protected void logMessage ( String message ) {
		getActiveNode().observerNotice(Event.SERVICE_MESSAGE, this.getClass().getName() + ": " + message);
	}
	

	/**
	 * get the currently active l2p node (from the current thread context)
	 * 
	 * @return 	the currently active las2peer node
	 */
	protected Node getActiveNode() {
		return getL2pThread().getContext().getLocalNode();
	}
	
	/************************** end of service helper methods ************************************/
	
	/**
	 * get an id String for the envelope stored for an user
	 * 
	 * @param user
	 */
	public static String getEnvelopeId ( Agent user ) {
		return "userDBs-" + user.getId();
	}	
	
	
	public SQLDatabaseManager() {
		
		// get the user's security object which contains the database information
		Agent user = getActiveAgent();
		
		SQLDatabaseSettings[] settings = null;
		
		try {
			storedDBs = Context.getCurrent().getStoredObject(SQLDatabaseSettings[].class, getEnvelopeId (user) );
			storedDBs.open();
			
			settings = storedDBs.getContent(SQLDatabaseSettings[].class);
		} catch ( Exception e ) {
			try {
				storedDBs = Envelope.createClassIdEnvelope(new SQLDatabaseSettings[0], getEnvelopeId(user), user);
				storedDBs.open();
				storedDBs.addSignature(getActiveAgent());
				storedDBs.store();
				logMessage("Failed to get the users' SQL settings from the net -- using a fresh one! " + e.getMessage());
			} catch ( Exception e2 ) {
				logMessage("Failed to generate a new envelope for storing user filters! " + e2 );
			}
		}
		
		if(settings == null || settings.length <= 0) {
			// there are no database settings available yet...
		} else {
			for ( SQLDatabaseSettings setting: settings )
				userDatabaseMap.put ( setting.getKey(), setting);
		}
	}
	
	// add database to users' security object
	public boolean addDatabase(String key, SQLDatabaseType jdbcInfo, String username, String password, String database, String host, int port) throws Exception {
		try {			
			//TODO: sanity checks for the parameters
			if(exitsDatabase(key)) {
				throw new Exception("Database with key " + key + " already exists!");
			}
			
			SQLDatabaseSettings databaseSettings = new SQLDatabaseSettings(key, jdbcInfo, username, password, database, host, port);
			userDatabaseMap.put(databaseSettings.getKey(), databaseSettings);
			storeDBList();
			
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}
	
	// removes a database from the hashmap and the security objects
	public boolean removeDatabase(String key) throws Exception {
		try {
			if(!exitsDatabase(key)) {
				throw new Exception("Database with key " + key + " does not exists!");
			}
			
			if(userDatabaseMap != null && userDatabaseMap.containsKey( key )) {
				// delete from hash map
				userDatabaseMap.remove(key);
				storeDBList();
			}
			
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}
	
	public boolean exitsDatabase(String key) {
		try {
			return (userDatabaseMap.get(key) != null);
		}
		catch(Exception e) {
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
			while(iterator.hasNext()) {
				keyList.add(iterator.next());
			}
			
			return keyList;
		}
		catch(Exception e) {
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
			while(iterator.hasNext()) {
				settingsList.add(iterator.next());
			}
			
			return settingsList;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		return null;
	}
	
	public String getDatabaseIdString(String databaseKey) throws Exception {
		try {
			SQLDatabaseSettings databaseSettings = userDatabaseMap.get(databaseKey);
			
			if(databaseSettings == null) {
				return null;
			}
			
			return databaseSettings.getHost() + ":" +databaseSettings.getPort() + "/" + databaseSettings.getDatabase();
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}
	
	// get a instance of a SQL database (JDBC based)
	public SQLDatabase getDatabaseInstance(String databaseKey) throws Exception {
		try {
			SQLDatabase sqlDatabase = loadedDatabases.get(databaseKey);
			
			if(sqlDatabase != null) {
				//TODO: check that the database is still open/valid
			}
			
			else {
				SQLDatabaseSettings databaseSettings = userDatabaseMap.get(databaseKey);
				
				if(databaseSettings == null) {
					// the requested database is not known
					String dbKeyListString = "";
					Iterator<String> iterator = getDatabaseKeyList().iterator();
					while(iterator.hasNext()) {
						dbKeyListString += " " + iterator.next();
					}
					
					throw new Exception("The requested database is not known/configured! (Requested:" + databaseKey + ", Available: "+dbKeyListString + ")");
				}
				
				sqlDatabase = new SQLDatabase(databaseSettings.getJdbcInfo(), databaseSettings.getUsername(), databaseSettings.getPassword(), databaseSettings.getDatabase(), databaseSettings.getHost(), databaseSettings.getPort());
			
				// try to connect ...
				sqlDatabase.connect();
			}
			return sqlDatabase;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}
	
	
	// closes/disconnects all loaded databases
	public boolean closeAllDatabaseInstances() {
		try {
			boolean noErrorOccurred = true;
			Iterator<SQLDatabase> iterator = loadedDatabases.values().iterator();
			
			while(iterator.hasNext()) {
				SQLDatabase sqlDatabase = iterator.next();
				if(!sqlDatabase.disconnect()) {
					noErrorOccurred = false;
				}
			}
			
			return noErrorOccurred;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		
		return false;
	}
		
	
	/**
	 * store the current database list to the backend
	 */
	public void storeDBList () {
		SQLDatabaseSettings[] databases = userDatabaseMap.values().toArray(new SQLDatabaseSettings[0]);
		try {
			storedDBs = Context.getCurrent().getStoredObject(SQLDatabaseSettings[].class, getEnvelopeId ( getActiveAgent()) );
			storedDBs.open();
			storedDBs.updateContent ( databases );
			storedDBs.addSignature(getActiveAgent());
			storedDBs.store();
			logMessage("stored Database list");
		} catch (Exception e) {
			logMessage("Error storing the Database list! " + e);
			System.out.println ( "QV critical:");
			e.printStackTrace();
		}
	}
}
