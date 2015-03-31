package i5.las2peer.services.queryVisualization.query;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minidev.json.JSONObject;
import i5.las2peer.api.Service;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.Agent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.queryVisualization.QueryVisualizationService;
import i5.las2peer.services.queryVisualization.database.DBDoesNotExistException;
import i5.las2peer.services.queryVisualization.database.DoesNotExistException;
import i5.las2peer.services.queryVisualization.database.SQLDatabase;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseManager;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseSettings;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseType;
import i5.las2peer.services.queryVisualization.encoding.ModificationType;
import i5.las2peer.services.queryVisualization.encoding.VisualizationType;

/**
 * 
 * QueryManager.java
 *<br>
 * A Manager that stores and fetches queries from the network.
 * 
 */
public class QueryManager {
	
	private HashMap<String, Query> userQueryMap = new HashMap<String, Query>();
	private HashMap<String, String> loadedQueryValues = new HashMap<String, String>();
	private SQLDatabase storageDatabase = null;
	private QueryVisualizationService service = null;
	
	private boolean connected = false;

	
	/*************** "service" helper methods *************************/
	
	private boolean connect() {
		try {
			storageDatabase.connect();
			connected = true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void disconnect(boolean wasConnected) {
		if (!wasConnected && connected) {
			storageDatabase.disconnect();
			connected = false;
		}
	}

	private void disconnect() {
		disconnect(false);
	}
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
	 * get the anonymous agent
	 * @return anonymous agent
	 */
	protected Agent getAnonymousAgent () {
		return getL2pThread().getContext().getLocalNode().getAnonymous();
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
	
	public QueryManager(QueryVisualizationService service, SQLDatabase dbm) {
		storageDatabase = dbm;
		this.service = service;
		// get the user's security object which contains the database information
		
		Query[] settings = null;
		
		try {
			connect();
			PreparedStatement p = storageDatabase.prepareStatement(
					"SELECT * FROM QVS.QUERIES WHERE USER = ?;");
			p.setLong(1, getL2pThread().getContext().getMainAgent().getId());
			ResultSet databases = p.executeQuery();
			settings = Query.fromResultSet(databases);
		} catch ( Exception e ) {
			logMessage("Failed to get the users' SQL settings. " + e.getMessage());
		} finally {
			disconnect();
		}
		
		for ( Query setting: settings )
			userQueryMap.put ( setting.getKey(), setting);
	}
	
	// add a query to the p2p storage
	public boolean storeQuery(Query query) {
		Query[] queryArray = new Query[1];
		queryArray[0] = query;
		try {
			String title = query.getTitle();
			for (Query q : userQueryMap.values()) {
				if (q.getTitle().equals(title)) {
					query.setKey(q.getKey());
					break;
				}
			}
			storageDatabase.connect();
			PreparedStatement p = storageDatabase.prepareStatement(Query.getReplace());
			query.prepareStatement(p);
			p.executeUpdate();
			storageDatabase.disconnect();
			userQueryMap.put(query.getKey(), query);
			logMessage("stored query: " + query.getKey());	
			return true;
		}
		catch (Exception e) {
			logMessage("Error storing query! " + e);
			System.out.println ( "QV critical:");
			e.printStackTrace();
			return false;
		}
	}
	
	// get a query from the p2p storage
	public Query getQuery(String queryKey) throws Exception {
		try {
			
			UserAgent u = (UserAgent) getL2pThread().getContext().getMainAgent();
			if (!u.hasLogin()) {
                storageDatabase.connect();
                PreparedStatement p = storageDatabase.prepareStatement(
                                "SELECT * FROM QVS.QUERIES WHERE KEY = ?;");
                p.setString(1, queryKey);
                ResultSet databases = p.executeQuery();
                Query[] settings = Query.fromResultSet(databases);
                storageDatabase.disconnect();
                if (settings.length > 0) {
                        return settings[0];
				}
                throw new Exception("The requested Query is not known! (Requested:" + queryKey );
			}
			
			Query query = userQueryMap.get(queryKey);

			if(query != null) {
				//TODO: check that the database is still open/valid
			} else {
				query = userQueryMap.get(queryKey);

				if(query == null) {
					// the requested query is not known
					String keyListString = "";
					Iterator<String> iterator = getQueryKeyList().iterator();
					while(iterator.hasNext()) {
						keyListString += " " + iterator.next();
					}

					throw new Exception("The requested Query is not known! (Requested:" + queryKey + ", Available: "+keyListString + ")");
				}
			}
			return query;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}

	public void databaseDeleted(String dbKey) {
		String db;
		try {
			db = service.databaseManagerMap.get(getL2pThread().getContext().getMainAgent().getId()).getDatabaseInstance(dbKey).getDatabase();
		} catch (Exception e1) {
			return;
		}
		for (Query q : userQueryMap.values()) {
			if (q.getDatabase().equals(db)) {
				try {
					removeQ(q.getKey());
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Remove given database from the database
	 */
	public void removeQ(String queryKey) {
		try {
			storageDatabase.connect();
			PreparedStatement s = storageDatabase.prepareStatement("DELETE FROM `QUERIES` WHERE ((`KEY` = ? AND `USER` = ?))");
			s.setString(1, queryKey);
			s.setLong(2, getL2pThread().getContext().getMainAgent().getId());
			s.executeUpdate();
			storageDatabase.disconnect();
		} catch (Exception e) {
			logMessage("Error removing the Query! " + e);
			System.out.println ( "QV critical:");
			e.printStackTrace();
		} finally {
			userQueryMap.remove(queryKey);
		}
	}

	// get a list of the names of all queries of the user
	public List<String> getQueryKeyList() {
		try {
			LinkedList<String> keyList = new LinkedList<String>();
			Iterator<String> iterator = this.userQueryMap.keySet().iterator();
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
	public List<Query> getQueryList() {
		try {
			LinkedList<Query> settingsList = new LinkedList<Query>();
			Iterator<Query> iterator = this.userQueryMap.values().iterator();
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

	public String getQueryValues(String queryKey, QueryVisualizationService agent) throws Exception {
		try {
			String queryValues = loadedQueryValues.get(queryKey);
			
			if(queryValues == null) {
				// load them
				Query querySettings = userQueryMap.get(queryKey);
				
				if(querySettings == null) {
					// the requested filter is not known/defined
					throw new Exception("The requested filter is not known/configured! sRequested:" + queryKey);
				}
				
				// get the filter values from the database...
				String query = querySettings.getQueryStatement();
				String databaseName = querySettings.getDatabase();
				String databaseKey = getDBSettings(querySettings).getKey();
				String[] vparams = querySettings.getVisualizationParameters();
				VisualizationType vtypei = querySettings.getVisualizationTypeIndex();
				queryValues = agent.createQueryString(query, null, databaseKey, true, ModificationType.IDENTITIY.ordinal(), vtypei, vparams,false);

				// store/cache the filter values
				loadedQueryValues.put(queryKey, queryValues);
			}
			
			return queryValues;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}
	public SQLDatabaseSettings getDBSettings(Query q) {
		String databaseName = q.getDatabase();
        SQLDatabaseManager dbm = service.databaseManagerMap.get(getL2pThread().getContext().getMainAgent().getId());
		for (SQLDatabaseSettings db : dbm.getDatabaseSettingsList()) {
			if (databaseName.equals(db.getDatabase())) {
				return db;
			}
		}
		return null;
	}

	public JSONObject toJSON(Query query) throws DBDoesNotExistException {
		JSONObject o = new JSONObject();
		o.put("key", query.getKey());
		try {
		    o.put("db", getDBSettings(query).getKey());
		} catch (Exception e) {
			throw new DBDoesNotExistException("Database " + query.getDatabase() + " does not exist.");
		}
		o.put("query", query.getQueryStatement());
		o.put("modtypei", query.getModificationTypeIndex());
		o.put("format", query.getVisualizationTypeIndex().toString());
		o.put("title", query.getTitle());
		o.put("width", query.getWidth());
		o.put("height", query.getHeight());
		return o;
	}
}
